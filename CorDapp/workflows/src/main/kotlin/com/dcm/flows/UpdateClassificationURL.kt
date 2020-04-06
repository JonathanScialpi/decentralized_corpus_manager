package com.dcm.flows

import co.paralleluniverse.fibers.Suspendable
import com.dcm.contract.ModelContract
import com.dcm.states.ModelState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC

class UpdateClassificationURL (
        private val newURL: String,
        private val modelLinearId: UniqueIdentifier
): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val transactionBuilder = TransactionBuilder(notary)

        // get current model state and use as input state
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(modelLinearId))
        val modelStateAndRef =  serviceHub.vaultService.queryBy<ModelState>(queryCriteria).states.single()
        transactionBuilder.addInputState(modelStateAndRef)
        val inputModelState = modelStateAndRef.state.data

        if(ourIdentity != inputModelState.owner){
            throw IllegalArgumentException("Only the owner can change the classification URL of a model.")
        }

        // finish building tx
        val outputModelState = inputModelState.replaceClassificationURL(newURL)
        val commandData = ModelContract.Commands.UpdateClassifcationEndpoint()
        transactionBuilder.addCommand(commandData, inputModelState.participants.map { it.owningKey })
        transactionBuilder.addOutputState(outputModelState, ModelContract.ID)
        transactionBuilder.verify(serviceHub)

        // sign and get other signatures
        val ptx = serviceHub.signInitialTransaction(transactionBuilder)
        val sessions = (inputModelState.participants - ourIdentity).map { initiateFlow(it) }.toSet()
        val stx = subFlow(CollectSignaturesFlow(ptx, sessions))
        return subFlow(FinalityFlow(stx, sessions))
    }
}

@InitiatedBy(UpdateClassificationURL::class)
class UpdateClassificationURLResponder(val counterpartySession: FlowSession): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an Model State transaction" using (output is ModelState)
            }
        }

        val txWeJustSignedId = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(otherSideSession = counterpartySession, expectedTxId = txWeJustSignedId.id))
    }
}