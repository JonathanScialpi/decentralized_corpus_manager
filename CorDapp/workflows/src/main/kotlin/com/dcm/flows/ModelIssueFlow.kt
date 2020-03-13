package com.dcm.flows

import co.paralleluniverse.fibers.Suspendable
import com.dcm.contract.ModelContract
import com.dcm.states.ModelState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
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
class ModelIssueFlow(
        private val corpus: LinkedHashMap<String, String>,
        private val classificationReport: LinkedHashMap<String, LinkedHashMap<String, Double>>,
        private val targetModelNode : Party
): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction{
        val participants = listOf<Party>(ourIdentity, targetModelNode)
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val transactionBuilder = TransactionBuilder(notary)
        val output = ModelState(corpus, classificationReport, participants)
        val commandData = ModelContract.Commands.Issue()
        transactionBuilder.addCommand(commandData, participants.map { it -> it.owningKey })
        transactionBuilder.addOutputState(output, ModelContract.ID)
        transactionBuilder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(transactionBuilder)
        val session = initiateFlow(targetModelNode)
        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(session)))
        return subFlow(FinalityFlow(stx, listOf(session)))

    }
}

@InitiatedBy(ModelIssueFlow::class)
class ModelIssueFlowResponder(val counterpartySession: FlowSession): FlowLogic<SignedTransaction>() {

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