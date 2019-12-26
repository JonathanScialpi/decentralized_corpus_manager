package com.dcm.flows

import co.paralleluniverse.fibers.Suspendable
import com.dcm.contract.ModelContract
import com.dcm.states.ModelState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class ModelIssueFlow(val state: ModelState): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction{
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val issueCommand = Command(ModelContract.Commands.Issue(), state.participants.map { it.owningKey })
        val outputStateAndContract = StateAndContract(state, ModelContract.MODEL_CONTRACT_ID)
        val txBuilder = TransactionBuilder(notary = notary).withItems(outputStateAndContract, issueCommand)
        txBuilder.verify(serviceHub)
        val partiallySignedTx = serviceHub.signInitialTransaction(txBuilder)
        val sessions = (state.participants - ourIdentity).map { initiateFlow(it) }.toSet()
        val stx = subFlow(CollectSignaturesFlow(partiallySignedTx, sessions))
        return subFlow(FinalityFlow(stx, sessions))
    }
}

@InitiatedBy(ModelIssueFlow::class)
class ModelIssueFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an Model State transaction" using (output is ModelState)
            }
        }

        val txWeJustSignedId = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))
    }
}