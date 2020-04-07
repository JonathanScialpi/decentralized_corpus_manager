package com.dcm.webserver

import com.dcm.flows.*
import com.dcm.states.ModelState
import com.fasterxml.jackson.annotation.JsonCreator
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.vaultQueryBy
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

    // A simple endpoint to make sure that the server is up and running
    @GetMapping(value = ["/status"])
    private fun isAlive() = "Up and running!"

    data class ModelObj @JsonCreator constructor(
            val algorithmUsed : String,
            val classificationURL : String,
            val corpus: LinkedHashMap<String, String>,
            val participants : String
    )

    // Create a model using a LinkedHashMap of String to String. They Keys of the map represent data rows while the
    // values represent their labels.
    @RequestMapping(value = "/issueModel", method = [RequestMethod.POST])
    private fun issueModel(@RequestBody newModel : ModelObj): ResponseEntity<Any?>{
        val result = proxy.startFlow(
                ::IssueModelFlow,
                newModel.algorithmUsed,
                newModel.classificationURL,
                newModel.corpus,
                newModel.participants.split(";").map{ proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(it)) as Party}
        )
        val model = result.returnValue.get().tx.outputs[0].data as ModelState
        val responseMap = HashMap<String, Any>()
        responseMap["algorithmUsed"] = model.algorithmUsed
        responseMap["classificationURL"] = model.classificationURL
        responseMap["corpus"] = model.corpus
        responseMap["classificationReport"] = model.classificationReport
        responseMap["owner"] = model.owner
        responseMap["participants"] = model.participants
        responseMap["LinearID"] = model.linearId
        return ResponseEntity.ok(responseMap.toString())
    }

    data class CorpusObj @JsonCreator constructor(
            val proposedCorpus: LinkedHashMap<String, String>,
            val modelLinearId: String
    )

    // An endpoint that allows a user to propose a new corpus for the model with the intent to improve it.
    @RequestMapping(value = "/updateCorpus", method = [RequestMethod.POST])
    private fun updateCorpus(@RequestBody updatedCorpus : CorpusObj): ResponseEntity<Any?>{
        val result = proxy.startFlow(
                ::UpdateCorpusFlow,
                updatedCorpus.proposedCorpus,
                UniqueIdentifier.fromString(updatedCorpus.modelLinearId)
        )

        val model = result.returnValue.get().tx.outputs[0].data as ModelState
        val responseMap = HashMap<String, Any>()
        responseMap["algorithmUsed"] = model.algorithmUsed
        responseMap["classificationURL"] = model.classificationURL
        responseMap["corpus"] = model.corpus
        responseMap["classificationReport"] = model.classificationReport
        responseMap["owner"] = model.owner
        responseMap["participants"] = model.participants
        responseMap["LinearID"] = model.linearId
        return ResponseEntity.ok(responseMap.toString())
    }

    data class ClassificationURLObj @JsonCreator constructor(
            val newURL: String,
            val modelLinearId: String
    )

    // An endpoint for strictly modifying the URL that is used to build the classification report.
    @RequestMapping(value = "/updateClassificationURL", method = [RequestMethod.POST])
    private fun updateClassificationEndpoint(@RequestBody updatedURL : ClassificationURLObj): ResponseEntity<Any?>{
        val result = proxy.startFlow(
                ::UpdateClassificationURL,
                updatedURL.newURL,
                UniqueIdentifier.fromString(updatedURL.modelLinearId)
        )

        val model = result.returnValue.get().tx.outputs[0].data as ModelState
        val responseMap = HashMap<String, Any>()
        responseMap["algorithmUsed"] = model.algorithmUsed
        responseMap["classificationURL"] = model.classificationURL
        responseMap["corpus"] = model.corpus
        responseMap["classificationReport"] = model.classificationReport
        responseMap["owner"] = model.owner
        responseMap["participants"] = model.participants
        responseMap["LinearID"] = model.linearId
        return ResponseEntity.ok(responseMap.toString())
    }

    data class OwnershipObj @JsonCreator constructor(
            val newOwner: String,
            val modelLinearId: String
    )

    // Only the owner of a model can "close" it or update its classification URL. This endpoint allows an owner to
    // re-assign the ownership of a model.
    @RequestMapping(value = "/transferOwnership", method = [RequestMethod.POST])
    private fun transferOwnership(@RequestBody updatedOwner : OwnershipObj): ResponseEntity<Any?>{
        val result = proxy.startFlow(
                ::TransferOwnershipFlow,
                proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(updatedOwner.newOwner)) as Party,
                UniqueIdentifier.fromString(updatedOwner.modelLinearId)
        )

        val model = result.returnValue.get().tx.outputs[0].data as ModelState
        val responseMap = HashMap<String, Any>()
        responseMap["algorithmUsed"] = model.algorithmUsed
        responseMap["classificationURL"] = model.classificationURL
        responseMap["corpus"] = model.corpus
        responseMap["classificationReport"] = model.classificationReport
        responseMap["owner"] = model.owner
        responseMap["participants"] = model.participants
        responseMap["LinearID"] = model.linearId
        return ResponseEntity.ok(responseMap.toString())
    }

    data class ModelLinearId @JsonCreator constructor(
            val modelLinearId: String
    )

    // Prevent any further changes to a model by pointing to the exit state.
    @RequestMapping(value = "/closeModel", method = [RequestMethod.POST])
    private fun closeModel(@RequestBody modelLinearId : ModelLinearId): ResponseEntity<Any?>{
        val result = proxy.startFlow(
                ::CloseModelFlow,
                UniqueIdentifier.fromString(modelLinearId.modelLinearId)
        )
        val model = result.returnValue.get().tx.outputs[0].data as ModelState
        val responseMap = HashMap<String, Any>()
        responseMap["algorithmUsed"] = model.algorithmUsed
        responseMap["classificationURL"] = model.classificationURL
        responseMap["corpus"] = model.corpus
        responseMap["classificationReport"] = model.classificationReport
        responseMap["owner"] = model.owner
        responseMap["participants"] = model.participants
        responseMap["LinearID"] = model.linearId
        return ResponseEntity.ok(responseMap.toString())
    }

    data class CSVModelObj @JsonCreator constructor(
            val algorithmUsed : String,
            val classificationURL : String,
            val participants : String
    )

    // The user has the option of using a CSV file delimited by "|" to use as a corpus for model creation.
    @RequestMapping(value = "/issueModelWithCSV", method = [RequestMethod.POST], consumes = ["multipart/form-data"])
    open fun importCSV(@Valid @RequestParam("csvFile") multipart: MultipartFile, csvModelObj: CSVModelObj): ResponseEntity<String> {
        var corpus = LinkedHashMap<String, String>()
        multipart.inputStream.bufferedReader().use { stream ->
            stream.readLines().map {
                var currentLine = it.split("|")
                corpus[currentLine[0]] = currentLine[1]
            }
        }

        val result = proxy.startFlow(
                ::IssueModelFlow,
                csvModelObj.algorithmUsed,
                csvModelObj.classificationURL,
                corpus,
                csvModelObj.participants.split(";").map{ proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(it)) as Party}
        )

        val model = result.returnValue.get().tx.outputs[0].data as ModelState
        val responseMap = HashMap<String, Any>()
        responseMap["algorithmUsed"] = model.algorithmUsed
        responseMap["classificationURL"] = model.classificationURL
        responseMap["corpus"] = model.corpus
        responseMap["classificationReport"] = model.classificationReport
        responseMap["owner"] = model.owner
        responseMap["participants"] = model.participants
        responseMap["LinearID"] = model.linearId
        return ResponseEntity.ok(responseMap.toString())
    }

    // Use a linearId to retrieve the most recent version of a model state.
    @RequestMapping(value = ["/modelLookup"], produces = ["text/plain"],  method = [RequestMethod.POST])
    private fun getModelState(modelLinearId : ModelLinearId) : ResponseEntity<String> {
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(UniqueIdentifier.fromString(modelLinearId.modelLinearId)))
        val model = proxy.vaultQueryByCriteria(queryCriteria, LinearState::class.java).states.single().state.data as ModelState
        val responseMap = HashMap<String, Any>()
        responseMap["algorithmUsed"] = model.algorithmUsed
        responseMap["classificationURL"] = model.classificationURL
        responseMap["corpus"] = model.corpus
        responseMap["classificationReport"] = model.classificationReport
        responseMap["owner"] = model.owner
        responseMap["participants"] = model.participants
        responseMap["LinearID"] = model.linearId
        return ResponseEntity.ok(responseMap.toString())
    }
}