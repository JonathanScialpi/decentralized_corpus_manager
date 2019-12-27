package com.dcm
import com.dcm.flows.ModelIssueFlow
import net.corda.core.contracts.Command
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.core.identity.Party
import net.corda.testing.internal.chooseIdentityAndCert
import net.corda.testing.node.*
import com.dcm.contract.ModelContract
import com.dcm.flows.ModelIssueFlowResponder
import com.dcm.states.DataRowState
import com.dcm.states.ModelState
import groovy.util.GroovyTestCase.assertEquals
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.TransactionVerificationException
import org.junit.*
import kotlin.test.assertFailsWith

/**
These tests rely on Quasar to be loaded, set your run configuration to "-ea -javaagent:lib/quasar.jar"
 * Run configuration can be edited in IntelliJ under Run -> Edit Configurations -> VM options
 * On some machines/configurations you may have to provide a full path to the quasar.jar file.
 * On some machines/configurations you may have to use the "JAR manifest" option for shortening the command line.
 */
class ModelIssueFlowTests {
    lateinit var mockNetwork: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode
    lateinit var c: StartedMockNode
    lateinit var d: StartedMockNode

    @Before
    fun setup() {
        mockNetwork = MockNetwork(
                listOf("com.dcm"),
                notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary", "London", "GB")))
        )
        a = mockNetwork.createNode(MockNodeParameters())
        b = mockNetwork.createNode(MockNodeParameters())
        c = mockNetwork.createNode(MockNodeParameters())
        d = mockNetwork.createNode(MockNodeParameters())
        val startedNodes = arrayListOf(a, b, c, d)
        // For real nodes this happens automatically, but we have to manually register the flow for tests
        startedNodes.forEach { it.registerInitiatedFlow(ModelIssueFlowResponder::class.java) }
        mockNetwork.runNetwork()
    }

    @After
    fun tearDown() {
        mockNetwork.stopNodes()
    }

    /**
     * - Create a [TransactionBuilder] and pass it a notary reference.
     * - A notary [Party] object can be obtained from [FlowLogic.serviceHub.networkMapCache].
     * - Create an [ModelContract.Commands.Issue] inside a new [Command].
     * - The required signers will be the same as the state's participants
     * - use [TransactionBuilder.withItems] to create the transaction instead
     * - Sign the transaction and convert it to a [SignedTransaction] using the [serviceHub.signInitialTransaction] method.
     * - Return the [SignedTransaction].
     */
    @Test
    fun flowReturnsCorrectlyFormedPartiallySignedTransaction() {
        val corpus = listOf<DataRowState>()
        val dataRowMap = LinkedHashMap<String, DataRowState>()
        val g1 = a.info.chooseIdentityAndCert().party
        val g2 = b.info.chooseIdentityAndCert().party
        val g3 = c.info.chooseIdentityAndCert().party
        val g4 = d.info.chooseIdentityAndCert().party
        val gateKeepers = listOf<Party>(g1,g2,g3,g4)
        val model = ModelState(corpus, dataRowMap, gateKeepers)
        val flow = ModelIssueFlow(model)
        val future = a.startFlow(flow)
        mockNetwork.runNetwork()
        // Return the unsigned(!) SignedTransaction object from the ModelIssueFlow.
        val ptx: SignedTransaction = future.getOrThrow()

        // Check the transaction is well formed...
        // No inputs, one output ModelState and a command with the right properties.
        assert(ptx.tx.inputs.isEmpty())
        assert(ptx.tx.outputs.single().data is ModelState)
        val command = ptx.tx.commands.single()
        assert(command.value is ModelContract.Commands.Issue)
        assert(command.signers.toSet() == model.participants.map { it.owningKey }.toSet())
        ptx.verifySignaturesExcept(
                g2.owningKey, g3.owningKey, g4.owningKey,
                mockNetwork.defaultNotaryNode.info.legalIdentitiesAndCerts.first().owningKey
        )
    }

    /**
     * On the Initiator side:
     * - Get a set of signers required from the participants who are not the node
     * - - [ourIdentity] will give you the identity of the node you are operating as
     * - Use [initiateFlow] to get a set of [FlowSession] objects
     * - - Using [state.participants] as a base to determine the sessions needed is recommended. [participants] is on
     * - - the state interface so it is guaranteed to exist where [lender] and [borrower] are not.
     * - - Hint: [ourIdentity] will give you the [Party] that represents the identity of the initiating flow.
     * - Use [subFlow] to start the [CollectSignaturesFlow]
     * - Pass it a [SignedTransaction] object and [FlowSession] set
     * - It will return a [SignedTransaction] with all the required signatures
     * - The subflow performs the signature checking and transaction verification for you
     *
     * On the Responder side:
     * - Create a subclass of [SignTransactionFlow]
     * - Override [SignTransactionFlow.checkTransaction] to impose any constraints on the transaction
     *
     * Using this flow you abstract away all the back-and-forth communication required for parties to sign a
     * transaction.
     */
    @Test
    fun flowReturnsTransactionSignedByBothParties() {
        val corpus = listOf<DataRowState>()
        val dataRowMap = LinkedHashMap<String, DataRowState>()
        val g1 = a.info.chooseIdentityAndCert().party
        val g2 = b.info.chooseIdentityAndCert().party
        val g3 = c.info.chooseIdentityAndCert().party
        val g4 = d.info.chooseIdentityAndCert().party
        val gateKeepers = listOf<Party>(g1,g2,g3,g4)
        val model = ModelState(corpus, dataRowMap, gateKeepers)
        val flow = ModelIssueFlow(model)
        val future = a.startFlow(flow)
        mockNetwork.runNetwork()
        val stx = future.getOrThrow()
        stx.verifyRequiredSignatures()
    }

    /**
     * - Use the [FinalityFlow] to ensure the transaction is recorded in both [Party] vaults.
     * - Do not use the [BroadcastTransactionFlow]!
     * - The [FinalityFlow] determines if the transaction requires notarisation or not.
     * - We don't need the notary's signature as this is an issuance transaction without a timestamp. There are no
     *   inputs in the transaction that could be double spent! If we added a timestamp to this transaction then we
     *   would require the notary's signature as notaries act as a timestamping authority.
     */
    @Test
    fun flowRecordsTheSameTransactionInBothPartyVaults() {
        val corpus = listOf<DataRowState>()
        val dataRowMap = LinkedHashMap<String, DataRowState>()
        val g1 = a.info.chooseIdentityAndCert().party
        val g2 = b.info.chooseIdentityAndCert().party
        val g3 = c.info.chooseIdentityAndCert().party
        val g4 = d.info.chooseIdentityAndCert().party
        val gateKeepers = listOf<Party>(g1,g2,g3,g4)
        val model = ModelState(corpus, dataRowMap, gateKeepers)
        val flow = ModelIssueFlow(model)
        val future = a.startFlow(flow)
        mockNetwork.runNetwork()
        val stx = future.getOrThrow()
        println("Signed transaction hash: ${stx.id}")
        listOf(a, b, c, d).map {
            it.services.validatedTransactions.getTransaction(stx.id)
        }.forEach {
            val txHash = (it as SignedTransaction).id
            println("$txHash == ${stx.id}")
            assertEquals(stx.id, txHash)
        }
    }
}
