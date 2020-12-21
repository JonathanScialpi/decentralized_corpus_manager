package com.dcm.flows

import co.paralleluniverse.fibers.Suspendable
import com.dcm.contract.CorpusContract
import com.dcm.states.CorpusState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException

@InitiatingFlow
@StartableByRPC
class UpdateCorpusFlow(
        private val proposedCorpus: LinkedHashMap<String, String>,
        private val corpusLinearId: UniqueIdentifier
): FlowLogic<SignedTransaction>() {
    companion object{
        var payload = LinkedHashMap<String, LinkedHashMap<String, String>>()
        lateinit var classificationUpdateURL : String
    }

    @Suspendable
    override fun call(): SignedTransaction{
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val transactionBuilder = TransactionBuilder(notary)

        // get current corpus state and use as input state
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(corpusLinearId))
        val corpusStateAndRef =  serviceHub.vaultService.queryBy<CorpusState>(queryCriteria).states.single()
        transactionBuilder.addInputState(corpusStateAndRef)
        val inputCorpusState = corpusStateAndRef.state.data
        classificationUpdateURL = inputCorpusState.classificationUpdateURL
        var outputCorpusState = inputCorpusState.replaceCorpus(proposedCorpus)

        // get new classification report
        payload["corpus"] = inputCorpusState.corpus
        payload["proposedCorpus"] = proposedCorpus

        val classificationResponse = await(RetrieveClassificationReport())
        val newClassificationReport: LinkedHashMap<String, LinkedHashMap<String, Double>> = Gson().fromJson(classificationResponse, object : TypeToken<LinkedHashMap<String, LinkedHashMap<String, Double>>>() {}.type)

        // finish building tx
        outputCorpusState = outputCorpusState.replaceClassificationReport(newClassificationReport)
        val commandData = CorpusContract.Commands.UpdateCorpus()
        transactionBuilder.addCommand(commandData, inputCorpusState.participants.map { it.owningKey })
        transactionBuilder.addOutputState(outputCorpusState, CorpusContract.ID)
        transactionBuilder.verify(serviceHub)

        // sign and get other signatures
        val ptx = serviceHub.signInitialTransaction(transactionBuilder)
        val sessions = (inputCorpusState.participants - ourIdentity).map { initiateFlow(it) }.toSet()
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
                        .url(classificationUpdateURL)
                        .post(body)
                        .build()
                val responseObject = OkHttpClient().newCall(request).execute()
                val responseString = responseObject.body().string()
                responseObject.body().close()
                return responseString

            }catch(e: IOException){
                throw HospitalizeFlowException("External classification report call failed.", e)
            }
        }
    }
}

@InitiatedBy(UpdateCorpusFlow::class)
class UpdateCorpusResponder(val counterpartySession: FlowSession): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be a Corpus State transaction" using (output is CorpusState)
            }
        }

        val txWeJustSignedId = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(otherSideSession = counterpartySession, expectedTxId = txWeJustSignedId.id))
    }
}