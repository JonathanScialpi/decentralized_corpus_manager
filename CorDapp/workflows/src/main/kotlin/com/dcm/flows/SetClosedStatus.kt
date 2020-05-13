package com.dcm.flows

import co.paralleluniverse.fibers.Suspendable
import com.dcm.contract.CorpusContract
import com.dcm.states.CorpusState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC

class SetClosedStatusFlow(private val corpusStateAndRef : StateAndRef<CorpusState>) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val transactionBuilder = TransactionBuilder(notary)

        transactionBuilder.addInputState(corpusStateAndRef)
        val inputCorpusState = corpusStateAndRef.state.data

        if(ourIdentity != inputCorpusState.owner){
            throw IllegalArgumentException("Only the owner can set the status of a corpus to 'Closed'.")
        }

        // finish building tx
        val outputCorpusState = inputCorpusState.setClosedStatus()
        val commandData = CorpusContract.Commands.SetClosedStatus()
        transactionBuilder.addCommand(commandData, inputCorpusState.participants.map { it.owningKey })
        transactionBuilder.addOutputState(outputCorpusState, CorpusContract.ID)
        transactionBuilder.verify(serviceHub)

        // sign and get other signatures
        val ptx = serviceHub.signInitialTransaction(transactionBuilder)
        val sessions = (inputCorpusState.participants - ourIdentity).map { initiateFlow(it) }.toSet()
        val stx = subFlow(CollectSignaturesFlow(ptx, sessions))
        return subFlow(FinalityFlow(stx, sessions))
    }


@InitiatedBy(SetClosedStatusFlow::class)
class SetClosedStatusResponder(val counterpartySession: FlowSession): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an Corpus State transaction" using (output is CorpusState)
            }
        }

        val txWeJustSignedId = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(otherSideSession = counterpartySession, expectedTxId = txWeJustSignedId.id))
    }
}
}