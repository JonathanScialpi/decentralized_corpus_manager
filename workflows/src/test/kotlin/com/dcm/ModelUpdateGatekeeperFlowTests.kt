package com.dcm

import com.dcm.contract.ModelContract
import com.dcm.flows.ModelAddGateKeeperFlowResponder
import com.dcm.flows.ModelIssueFlow
import com.dcm.flows.ModelIssueFlowResponder
import com.dcm.flows.ModelUpdateGateKeeperFlow
import com.dcm.states.DataRowState
import com.dcm.states.ModelState
import net.corda.core.contracts.StateRef
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.internal.chooseIdentityAndCert
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.MockNodeParameters
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
These tests rely on Quasar to be loaded, set your run configuration to "-ea -javaagent:lib/quasar.jar"
 * Run configuration can be edited in IntelliJ under Run -> Edit Configurations -> VM options
 * On some machines/configurations you may have to provide a full path to the quasar.jar file.
 * On some machines/configurations you may have to use the "JAR manifest" option for shortening the command line.
 */
class ModelUpdateGatekeeperFlowTests {
    lateinit var mockNetwork: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode
    lateinit var c: StartedMockNode

    @Before
    fun setup() {
        mockNetwork = MockNetwork(listOf("com.dcm"),
                notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB"))))
        a = mockNetwork.createNode(MockNodeParameters())
        b = mockNetwork.createNode(MockNodeParameters())
        c = mockNetwork.createNode(MockNodeParameters())
        val startedNodes = arrayListOf(a, b, c)
        // For real nodes this happens automatically, but we have to manually register the flow for tests
        startedNodes.forEach { it.registerInitiatedFlow(ModelIssueFlowResponder::class.java) }
        startedNodes.forEach { it.registerInitiatedFlow(ModelAddGateKeeperFlowResponder::class.java) }
        mockNetwork.runNetwork()
    }

    @After
    fun tearDown() {
        mockNetwork.stopNodes()
    }

    /**
     * Issue a Model on the ledger, we need to do this before we can update one.
     * */

    private fun issueModel(model: ModelState): SignedTransaction{
        val flow = ModelIssueFlow(model)
        val future = a.startFlow(flow)
        mockNetwork.runNetwork()
        return future.getOrThrow()
    }

    /**
     * Test to make sure we are getting back a partially signed TX.
     */

    @Test
    fun flowReturnsCorrectlyFormedPartiallySignedTransaction(){
        val corpus = listOf<DataRowState>()
        val dataRowMap = LinkedHashMap<String, DataRowState>()
        val gatekeepers = listOf<Party>(a.info.chooseIdentityAndCert().party)
        val stx = issueModel(ModelState(corpus, dataRowMap, gatekeepers))
        val inputModel = stx.tx.outputs.single().data as ModelState
        val flow = ModelUpdateGateKeeperFlow(inputModel.linearId, b.info.chooseIdentityAndCert().party, true)
        val future = a.startFlow(flow)
        mockNetwork.runNetwork()
        val ptx = future.getOrThrow()
        // Check the transaction is well formed...
        // One output IOUState, one input state reference and a Transfer command with the right properties.
        assert(ptx.tx.inputs.size == 1)
        assert(ptx.tx.outputs.size == 1)
        assert(ptx.tx.inputs.single() == StateRef(stx.id, 0))
        println("Input state ref: ${ptx.tx.inputs.single()} == ${StateRef(stx.id, 0)}")
        val outputIou = ptx.tx.outputs.single().data as ModelState
        println("Output state: $outputIou")
        val command = ptx.tx.commands.single()
        assert(command.value == ModelContract.Commands.AddGateKeepers() || command.value == ModelContract.Commands.RemoveGateKeepers())
        ptx.verifySignaturesExcept(b.info.chooseIdentityAndCert().party.owningKey, c.info.chooseIdentityAndCert().party.owningKey,
                mockNetwork.defaultNotaryNode.info.legalIdentitiesAndCerts.first().owningKey)
    }

}

