package com.dcm.webserver

import com.dcm.flows.*
import com.dcm.states.CorpusState
import com.fasterxml.jackson.annotation.JsonCreator
import com.google.gson.Gson
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.startFlow
import net.corda.core.node.services.vault.QueryCriteria
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import javax.validation.Valid


/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    // @DEV: A simple endpoint to make sure that the server is up and running
    @GetMapping(value = ["/status"])
    private fun isAlive() = "Up and running!"

    data class CorpusObj @JsonCreator constructor(
            val algorithmUsed : String,
            val classificationURL : String,
            val corpus: LinkedHashMap<String, String>,
            val participants : String
    )

    // @DEV: An endpoint to issue a corpus using JSON.
    // @Param: corpus is a LinkedHashMap<String, String> where the Key is the data row and the value is the
    // @Param: algorithmUsed is a String describing the type of algo used to produce the model
    // @Param: classificationURL is a String which represents the Flask endpoint where the classification report can be
    // created from.
    // @Param: participants is the list (Strings) of CordaX500 names for each party included on the TX.
    @RequestMapping(value = "/issueCorpus", method = [RequestMethod.POST])
    private fun issueCorpus(@RequestBody newCorpus : CorpusObj): ResponseEntity<String?>{
        val result = proxy.startFlow(
                ::IssueCorpusFlow,
                newCorpus.algorithmUsed,
                newCorpus.classificationURL,
                newCorpus.corpus,
                newCorpus.participants.split(";").map{ proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(it)) as Party}
        )
        val corpus = result.returnValue.get().tx.outputs[0].data as CorpusState
        val responseMap = HashMap<String, Any>()
        responseMap["algorithmUsed"] = corpus.algorithmUsed
        responseMap["classificationURL"] = corpus.classificationURL
        responseMap["corpus"] = corpus.corpus
        responseMap["classificationReport"] = corpus.classificationReport
        responseMap["owner"] = corpus.owner.toString()
        responseMap["participants"] = corpus.participants.toString()
        responseMap["LinearID"] = corpus.linearId
        return ResponseEntity.ok(Gson().toJson(responseMap))
    }

    data class ProposedCorpusObj @JsonCreator constructor(
            val proposedCorpus: LinkedHashMap<String, String>,
            val corpusLinearId: String
    )

    // @DEV: An endpoint that allows a user to propose a new corpus for the model with the intent to improve it.
    // @Param: proposedCorpus is a LinkedHashMap<String, String> where the Key is the data row and the value is the
    // label.
    // @Param: corpusLinearId is the LinearPointer used to query for the corpus state.
    @RequestMapping(value = "/updateCorpus", method = [RequestMethod.POST])
    private fun updateCorpus(@RequestBody updatedCorpus : ProposedCorpusObj): ResponseEntity<String?>{
        val result = proxy.startFlow(
                ::UpdateCorpusFlow,
                updatedCorpus.proposedCorpus,
                UniqueIdentifier.fromString(updatedCorpus.corpusLinearId)
        )

        val corpus = result.returnValue.get().tx.outputs[0].data as CorpusState
        val responseMap = HashMap<String, Any>()
        responseMap["algorithmUsed"] = corpus.algorithmUsed
        responseMap["classificationURL"] = corpus.classificationURL
        responseMap["corpus"] = corpus.corpus
        responseMap["classificationReport"] = corpus.classificationReport
        responseMap["owner"] = corpus.owner.toString()
        responseMap["participants"] = corpus.participants.toString()
        responseMap["LinearID"] = corpus.linearId
        return ResponseEntity.ok(Gson().toJson(responseMap))
    }

    data class ClassificationURLObj @JsonCreator constructor(
            val newURL: String,
            val corpusLinearId: String
    )

    // @DEV: An endpoint for strictly modifying the URL that is used to build the classification report.
    // @Param: newURL is a String representing the new endpoint used to produce the classification report.
    // @Param: corpusLinearId is the LinearPointer used to query for the corpus state.
    @RequestMapping(value = "/updateClassificationURL", method = [RequestMethod.POST])
    private fun updateClassificationEndpoint(@RequestBody updatedURL : ClassificationURLObj): ResponseEntity<String?>{
        val result = proxy.startFlow(
                ::UpdateClassificationURL,
                updatedURL.newURL,
                UniqueIdentifier.fromString(updatedURL.corpusLinearId)
        )
        val corpus = result.returnValue.get().tx.outputs[0].data as CorpusState
        val responseMap = HashMap<String, Any>()
        responseMap["algorithmUsed"] = corpus.algorithmUsed
        responseMap["classificationURL"] = corpus.classificationURL
        responseMap["corpus"] = corpus.corpus
        responseMap["classificationReport"] = corpus.classificationReport
        responseMap["owner"] = corpus.owner.toString()
        responseMap["participants"] = corpus.participants.toString()
        responseMap["LinearID"] = corpus.linearId
        return ResponseEntity.ok(Gson().toJson(responseMap))
    }

    data class OwnershipObj @JsonCreator constructor(
            val newOwner: String,
            val corpusLinearId: String
    )

    // @DEV: Only the owner of a corpus can "close" it or update its classification URL. This endpoint allows an owner
    // to re-assign the ownership of a corpus.
    // @Param: newOwner is the party representing the new owner of the corpus.
    // @Param: corpusLinearId is the LinearPointer used to query for the corpus state.
    @RequestMapping(value = "/transferOwnership", method = [RequestMethod.POST])
    private fun transferOwnership(@RequestBody updatedOwner : OwnershipObj): ResponseEntity<String?>{
        val result = proxy.startFlow(
                ::TransferOwnershipFlow,
                proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(updatedOwner.newOwner)) as Party,
                UniqueIdentifier.fromString(updatedOwner.corpusLinearId)
        )

        val corpus = result.returnValue.get().tx.outputs[0].data as CorpusState
        val responseMap = HashMap<String, Any>()
        responseMap["algorithmUsed"] = corpus.algorithmUsed
        responseMap["classificationURL"] = corpus.classificationURL
        responseMap["corpus"] = corpus.corpus
        responseMap["classificationReport"] = corpus.classificationReport
        responseMap["owner"] = corpus.owner.toString()
        responseMap["participants"] = corpus.participants.toString()
        responseMap["LinearID"] = corpus.linearId
        return ResponseEntity.ok(Gson().toJson(responseMap))
    }

    data class CorpusLinearId @JsonCreator constructor(
            val corpusLinearId: String
    )

    // @DEV: An endpoint for the corpus owner to prevent any further changes to a corpus by pointing to the exit state.
    // @Param: corpusLinearId is the LinearPointer used to query for the corpus state.
    @RequestMapping(value = "/closeCorpus", method = [RequestMethod.POST])
    private fun closeCorpus(@RequestBody corpusLinearId : CorpusLinearId): ResponseEntity<String?>{
        val result = proxy.startFlow(
                ::CloseCorpusFlow,
                UniqueIdentifier.fromString(corpusLinearId.corpusLinearId)
        )
        val corpus = result.returnValue.get().tx.outputs[0].data as CorpusState
        val responseMap = HashMap<String, Any>()
        responseMap["algorithmUsed"] = corpus.algorithmUsed
        responseMap["classificationURL"] = corpus.classificationURL
        responseMap["corpus"] = corpus.corpus
        responseMap["classificationReport"] = corpus.classificationReport
        responseMap["owner"] = corpus.owner.toString()
        responseMap["participants"] = corpus.participants.toString()
        responseMap["LinearID"] = corpus.linearId
        return ResponseEntity.ok(Gson().toJson(responseMap))
    }

    data class CSVCorpusObj @JsonCreator constructor(
            val algorithmUsed : String,
            val classificationURL : String,
            val participants : String
    )

    // @DEV: This endpoint gives the user has the option of using a CSV file delimited by "|" to use as a corpus for
    // corpus state creation.
    // @Param: csvFile is a multipart file that is a "|" delimited utterance (data|label).
    // @Param: algorithmUsed is a String describing the type of algo used to produce the model
    // @Param: classificationURL is a String which represents the Flask endpoint where the classification report can be
    // created from.
    // @Param: participants is the list (Strings) of CordaX500 names for each party included on the TX.
    @RequestMapping(value = "/issueCorpusWithCSV", method = [RequestMethod.POST], consumes = ["multipart/form-data"])
    open fun importCSV(@Valid @RequestParam("csvFile") multipart: MultipartFile, csvCorpusObj: CSVCorpusObj): ResponseEntity<String?> {
        var corpus = LinkedHashMap<String, String>()
        multipart.inputStream.bufferedReader().use { stream ->
            stream.readLines().map {
                var currentLine = it.split("|")
                corpus[currentLine[0]] = currentLine[1]
            }
        }

        val result = proxy.startFlow(
                ::IssueCorpusFlow,
                csvCorpusObj.algorithmUsed,
                csvCorpusObj.classificationURL,
                corpus,
                csvCorpusObj.participants.split(";").map{ proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(it)) as Party}
        )

        val corpusState = result.returnValue.get().tx.outputs[0].data as CorpusState
        val responseMap = HashMap<String, Any>()
        responseMap["algorithmUsed"] = corpusState.algorithmUsed
        responseMap["classificationURL"] = corpusState.classificationURL
        responseMap["corpus"] = corpusState.corpus
        responseMap["classificationReport"] = corpusState.classificationReport
        responseMap["owner"] = corpusState.owner.toString()
        responseMap["participants"] = corpusState.participants.toString()
        responseMap["LinearID"] = corpusState.linearId
        return ResponseEntity.ok(Gson().toJson(responseMap))
    }

    // @DEV: Retrieve the most recent version of a corpus state.
    // @Param: corpusLinearId is the LinearPointer used to query for the corpus state.
    @RequestMapping(value = ["/corpusLookup"],  method = [RequestMethod.POST],  produces = ["application/json"])
    private fun getCorpusState(@RequestBody corpusLinearId : CorpusLinearId) : ResponseEntity<String?> {
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(UniqueIdentifier.fromString(corpusLinearId.corpusLinearId)))
        val corpus = proxy.vaultQueryByCriteria(queryCriteria, LinearState::class.java).states.single().state.data as CorpusState
        val responseMap = HashMap<String, Any>()
        responseMap["algorithmUsed"] = corpus.algorithmUsed
        responseMap["classificationURL"] = corpus.classificationURL
        responseMap["corpus"] = corpus.corpus
        responseMap["classificationReport"] = corpus.classificationReport
        responseMap["owner"] = corpus.owner.toString()
        responseMap["participants"] = corpus.participants.toString()
        responseMap["LinearID"] = corpus.linearId
        return ResponseEntity.ok(Gson().toJson(responseMap))
    }
}