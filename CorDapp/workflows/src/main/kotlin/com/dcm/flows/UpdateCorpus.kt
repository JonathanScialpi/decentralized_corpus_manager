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
import okhttp3.*

@InitiatingFlow
@StartableByRPC
class UpdateCorpus(
        private val proposedCorpus: LinkedHashMap<String, String>,
        private val modelLinearId: UniqueIdentifier
): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction{
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val transactionBuilder = TransactionBuilder(notary)

        // get current model state and use as input state
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(modelLinearId))
        val modelStateAndRef =  serviceHub.vaultService.queryBy<ModelState>(queryCriteria).states.single()
        transactionBuilder.addInputState(modelStateAndRef)
        val inputModelState = modelStateAndRef.state.data
        var outputModelState = inputModelState.replaceModelCorpus(proposedCorpus)

        // get new classification report
        val payload = LinkedHashMap<String, LinkedHashMap<String, String>>()
        payload["corpus"] = proposedCorpus
        val json = MediaType.parse("application/json; charset=utf-8")
        val corpusJson =  Gson().toJson(payload)
        val body = RequestBody.create(json, corpusJson)
        val request = Request.Builder()
                .url(inputModelState.classificationURL)
                .post(body)
                .build()
        var response = OkHttpClient().newCall(request).execute()
        val newClassificationReport: LinkedHashMap<String, LinkedHashMap<String, Double>> = Gson().fromJson(response.body().string(), object : TypeToken<LinkedHashMap<String, LinkedHashMap<String, Double>>>() {}.type)
        response.body().close()
        response = null

        // finish building tx
        outputModelState = outputModelState.replaceClassificationReport(newClassificationReport)
        val commandData = ModelContract.Commands.UpdateCorpus()
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