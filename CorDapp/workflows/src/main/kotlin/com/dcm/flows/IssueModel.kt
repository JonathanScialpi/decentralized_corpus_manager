package com.dcm.flows

import co.paralleluniverse.fibers.Suspendable
import com.dcm.contract.ModelContract
import com.dcm.states.ModelState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException

@InitiatingFlow
@StartableByRPC

open class IssueModelFlow(
        private val algorithmUsed : String,
        open val classificationURL : String,
        private val corpus: LinkedHashMap<String, String>,
        private val participants : List<Party>
): FlowLogic<SignedTransaction>() {
    companion object{
        var payload = LinkedHashMap<String, LinkedHashMap<String, String>>()
    }

    @Suspendable
    override fun call(): SignedTransaction{
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val transactionBuilder = TransactionBuilder(notary)

        // get new classification report
        payload["corpus"] = corpus
        val classificationResponse = await(RetrieveClassificationReport())
        val classificationReport: LinkedHashMap<String, LinkedHashMap<String, Double>> = Gson().fromJson(classificationResponse, object : TypeToken<LinkedHashMap<String, LinkedHashMap<String, Double>>>() {}.type)

        // finish building tx
        val outputModelState = ModelState(
                "Open",
                algorithmUsed,
                classificationURL,
                corpus,
                classificationReport,
                ourIdentity,
                participants)
        val commandData = ModelContract.Commands.Issue()
        transactionBuilder.addCommand(commandData, participants.plus(ourIdentity).map { it.owningKey })
        transactionBuilder.addOutputState(outputModelState, ModelContract.ID)
        transactionBuilder.verify(serviceHub)

        // sign and get other signatures
        val ptx = serviceHub.signInitialTransaction(transactionBuilder)
        val sessions = (participants - ourIdentity).map { initiateFlow(it) }.toSet()
        val stx = subFlow(CollectSignaturesFlow(ptx, sessions))
        return subFlow(FinalityFlow(stx, sessions))
    }

    inner class RetrieveClassificationReport : FlowExternalOperation<String>{
        override fun execute(deduplicationId: String): String {
            try{
                val json = MediaType.parse("application/json; charset=utf-8")
                val corpusJson =  Gson().toJson(payload)
                val body = RequestBody.create(json, corpusJson)
                val request = Request.Builder()
                        .url(classificationURL)
                        .post(body)
                        .build()
                var responseObject = OkHttpClient().newCall(request).execute()
                val responseString = responseObject.body().string()
                responseObject.body().close()
                //responseObject = null
                return responseString

            }catch(e: IOException){
                throw HospitalizeFlowException("External classification report call failed.", e)
            }
        }
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