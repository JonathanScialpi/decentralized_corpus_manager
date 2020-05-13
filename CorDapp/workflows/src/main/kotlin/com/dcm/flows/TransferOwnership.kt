package com.dcm.flows

import co.paralleluniverse.fibers.Suspendable
import com.dcm.contract.CorpusContract
import com.dcm.states.CorpusState
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

class TransferOwnershipFlow (
        private val newOwner: Party,
        private val corpusLinearId: UniqueIdentifier
): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val transactionBuilder = TransactionBuilder(notary)

        // get current corpus state and use as input state
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(corpusLinearId))
        val corpusStateAndRef =  serviceHub.vaultService.queryBy<CorpusState>(queryCriteria).states.single()
        transactionBuilder.addInputState(corpusStateAndRef)
        val inputCorpusState = corpusStateAndRef.state.data

        if(ourIdentity != inputCorpusState.owner){
            throw IllegalArgumentException("Only the owner can transfer ownership of a corpus.")
        }

        // finish building tx
        val outputCorpusState = inputCorpusState.replaceOwner(newOwner)
        val commandData = CorpusContract.Commands.TransferOwnership()
        transactionBuilder.addCommand(commandData, inputCorpusState.participants.map { it.owningKey })
        transactionBuilder.addOutputState(outputCorpusState, CorpusContract.ID)
        transactionBuilder.verify(serviceHub)

        // sign and get other signatures
        val ptx = serviceHub.signInitialTransaction(transactionBuilder)
        val sessions = (inputCorpusState.participants - ourIdentity).map { initiateFlow(it) }.toSet()
        val stx = subFlow(CollectSignaturesFlow(ptx, sessions))
        return subFlow(FinalityFlow(stx, sessions))
    }
}

@InitiatedBy(TransferOwnershipFlow::class)
class TransferOwnershipResponder(val counterpartySession: FlowSession): FlowLogic<SignedTransaction>() {

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