package com.dcm.flows

import co.paralleluniverse.fibers.Suspendable
import com.dcm.contract.ModelContract
import com.dcm.states.ModelState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

@InitiatingFlow
@StartableByRPC
class IssueModelFlow(
        private val corpus: LinkedHashMap<String, String>,
        private val participants : List<Party>
): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction{
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val transactionBuilder = TransactionBuilder(notary)

        // get new classification report
        val formBody = FormBody.Builder()
                .add("corpus", Gson().toJson(corpus))
                .build()
        val request = Request.Builder()
                .url(CLASSIFY_URL)
                .post(formBody)
                .addHeader("Content-Type", "application/json")
                .build()
        val response = OkHttpClient().newCall(request).execute()
        val classificationReport: LinkedHashMap<String, LinkedHashMap<String, Double>> = Gson().fromJson(response.body().string(), object : TypeToken<LinkedHashMap<String, LinkedHashMap<String, Double>>>() {}.type)

        val outputModelState = ModelState(corpus, classificationReport, ourIdentity, participants)
        val commandData = ModelContract.Commands.Issue()
        transactionBuilder.addCommand(commandData, participants.map { it.owningKey })
        transactionBuilder.addOutputState(outputModelState, ModelContract.ID)
        transactionBuilder.verify(serviceHub)

        val ptx = serviceHub.signInitialTransaction(transactionBuilder)
        val sessions = (participants - ourIdentity).map { initiateFlow(it) }.toSet()
        val stx = subFlow(CollectSignaturesFlow(ptx, sessions))
        return subFlow(FinalityFlow(stx, sessions))
    }
}

@InitiatedBy(IssueModelFlow::class)
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