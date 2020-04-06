package com.dcm.webserver

import com.dcm.contract.ModelContract.Commands.TransferOwnership
import com.dcm.flows.*
import com.dcm.states.ModelState
import com.fasterxml.jackson.annotation.JsonCreator
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.messaging.startFlow
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

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

    @GetMapping(value = ["/status"])
    private fun isAlive() = "Up and running!"

    data class ModelObj @JsonCreator constructor(
            val algorithmUsed : String,
            val classificationURL : String,
            val corpus: LinkedHashMap<String, String>,
            val participants : List<Party>
    )

    @RequestMapping(value = "/issueModel", method = [RequestMethod.POST])
    private fun issueModel(@RequestBody newModel : ModelObj): ResponseEntity<Any?>{
        val result = proxy.startFlow(
                ::IssueModelFlow,
                newModel.algorithmUsed,
                newModel.classificationURL,
                newModel.corpus,
                newModel.participants
        )
        val responseMap = HashMap<String, Any>()
        responseMap["LinearID"] = (result.returnValue.get().tx.outputs[0].data as ModelState).linearId
        return ResponseEntity.ok(responseMap.toString())
    }

    data class CorpusObj @JsonCreator constructor(
            val proposedCorpus: LinkedHashMap<String, String>,
            val modelLinearId: UniqueIdentifier
    )

    @RequestMapping(value = "/updateCorpus", method = [RequestMethod.POST])
    private fun updateCorpus(@RequestBody updatedCorpus : CorpusObj): ResponseEntity<Any?>{
        val result = proxy.startFlow(
                ::UpdateCorpusFlow,
                updatedCorpus.proposedCorpus,
                updatedCorpus.modelLinearId
        )
        val responseMap = HashMap<String, Any>()
        responseMap["LinearID"] = (result.returnValue.get().tx.outputs[0].data as ModelState).linearId
        return ResponseEntity.ok(responseMap.toString())
    }

    data class ClassificationURLObj @JsonCreator constructor(
            val newURL: String,
            val modelLinearId: UniqueIdentifier
    )

    @RequestMapping(value = "/updateClassificationURL", method = [RequestMethod.POST])
    private fun updateClassificationEndpoint(@RequestBody updatedURL : ClassificationURLObj): ResponseEntity<Any?>{
        val result = proxy.startFlow(
                ::UpdateClassificationURL,
                updatedURL.newURL,
                updatedURL.modelLinearId
        )
        val responseMap = HashMap<String, Any>()
        responseMap["LinearID"] = (result.returnValue.get().tx.outputs[0].data as ModelState).linearId
        return ResponseEntity.ok(responseMap.toString())
    }

    data class OwnershipObj @JsonCreator constructor(
            val newOwner: Party,
            val modelLinearId: UniqueIdentifier
    )

    @RequestMapping(value = "/transferOwnership", method = [RequestMethod.POST])
    private fun transferOwnership(@RequestBody updatedOwner : OwnershipObj): ResponseEntity<Any?>{
        val result = proxy.startFlow(
                ::TransferOwnershipFlow,
                updatedOwner.newOwner,
                updatedOwner.modelLinearId
        )
        val responseMap = HashMap<String, Any>()
        responseMap["LinearID"] = (result.returnValue.get().tx.outputs[0].data as ModelState).linearId
        return ResponseEntity.ok(responseMap.toString())
    }

    data class ModelToClose @JsonCreator constructor(
            val modelLinearId: UniqueIdentifier
    )

    @RequestMapping(value = "/closeModel", method = [RequestMethod.POST])
    private fun closeModel(@RequestBody modelLinearId : ModelToClose): ResponseEntity<Any?>{
        val result = proxy.startFlow(
                ::CloseModelFlow,
                modelLinearId.modelLinearId
        )
        val responseMap = HashMap<String, Any>()
        responseMap["LinearID"] = (result.returnValue.get().tx.outputs[0].data as ModelState).linearId
        return ResponseEntity.ok(responseMap.toString())
    }

    //TODO: IssueWithCSV POST
}