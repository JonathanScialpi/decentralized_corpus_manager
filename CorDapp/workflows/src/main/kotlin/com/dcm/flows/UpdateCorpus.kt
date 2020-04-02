package com.dcm.flows

import co.paralleluniverse.fibers.Suspendable
import com.dcm.contract.ModelContract
import com.dcm.states.ModelState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

const val CLASSIFY_URL = "http://127.0.0.1:5000/classify"

@InitiatingFlow
@StartableByRPC
class UpdateCorpus(
        private val proposedCorpus: LinkedHashMap<String, String>,
        private val modelLinearId: UniqueIdentifier,
        private val targetModelNode : Party
): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction{
        val participants = listOf<Party>(ourIdentity, targetModelNode)
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val transactionBuilder = TransactionBuilder(notary)

        // get model state and use as input state
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(modelLinearId))
        val modelStateAndRef =  serviceHub.vaultService.queryBy<ModelState>(queryCriteria).states.single()
        val inputModelState = modelStateAndRef.state.data
        var outputModelState = inputModelState.replaceModelCorpus(proposedCorpus)

        // get new classification report
        val formBody = FormBody.Builder()
                .add("corpus", Gson().toJson(outputModelState.corpus))
                .build()
        val request = Request.Builder()
                .url(CLASSIFY_URL)
                .post(formBody)
                .addHeader("Content-Type", "application/json")
                .build()
        val response = OkHttpClient().newCall(request).execute()
        val newClassificationReport: LinkedHashMap<String, LinkedHashMap<String, Double>> = Gson().fromJson(response.body().string(), object : TypeToken<LinkedHashMap<String, LinkedHashMap<String, Double>>>() {}.type)
        outputModelState = outputModelState.replaceClassificationReport(newClassificationReport)

        // finish building tx
        val commandData = ModelContract.Commands.UpdateCorpus()
        transactionBuilder.addCommand(commandData, participants.map { it.owningKey })
        transactionBuilder.addOutputState(outputModelState, ModelContract.ID)
        transactionBuilder.verify(serviceHub)

        // sign and get other signatures
        val ptx = serviceHub.signInitialTransaction(transactionBuilder)
        val session = initiateFlow(targetModelNode)
        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(session)))
        return subFlow(FinalityFlow(stx, listOf(session)))
    }
}

@InitiatedBy(UpdateCorpus::class)
class UpdateCorpusResponder(val counterpartySession: FlowSession): FlowLogic<SignedTransaction>() {

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