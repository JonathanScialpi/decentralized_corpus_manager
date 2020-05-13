package com.dcm.flow

import com.dcm.contract.CorpusContract
import com.dcm.flows.CorpusIssueFlowResponder
import com.dcm.flows.IssueCorpusFlow
import com.dcm.states.CorpusState
import com.google.gson.JsonSyntaxException
import net.corda.core.identity.CordaX500Name
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
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IssueCorpusFlowTests {

    private lateinit var mockNetwork: MockNetwork
    private lateinit var a: StartedMockNode
    private lateinit var b: StartedMockNode
    private var origClassificationReport = LinkedHashMap<String, LinkedHashMap<String, Double>>()
    private var newClassificationReport =  LinkedHashMap<String, LinkedHashMap<String, Double>>()
    private var origCorpus =  LinkedHashMap<String, String>()
    private var newCorpus = LinkedHashMap<String, String>()

    @Before
    fun setup() {
        mockNetwork = MockNetwork(
                listOf("com.dcm"),
                notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary", "London", "GB")))
        )
        a = mockNetwork.createNode(MockNodeParameters())
        b = mockNetwork.createNode(MockNodeParameters())
        val startedNodes = arrayListOf(a, b)
        // For real nodes this happens automatically, but we have to manually register the flow for tests
        startedNodes.forEach { it.registerInitiatedFlow(CorpusIssueFlowResponder::class.java) }
        mockNetwork.runNetwork()

        origClassificationReport["BookRestaurant"] = linkedMapOf("f1-score" to 0.9975103734439834)
        origClassificationReport["BookRestaurant"] = linkedMapOf("precision" to 0.9983388704318937)
        origClassificationReport["BookRestaurant"] = linkedMapOf("recall" to 0.9966832504145937)
        origClassificationReport["BookRestaurant"] = linkedMapOf("support" to 603.0)
        origClassificationReport["GetWeather"] = linkedMapOf("f1-score" to 0.9966273187183812)
        origClassificationReport["GetWeather"] = linkedMapOf("precision" to 0.9983108108108109)
        origClassificationReport["GetWeather"] = linkedMapOf("recall" to 0.9949494949494949)
        origClassificationReport["GetWeather"] = linkedMapOf("support" to 594.0)
        origClassificationReport["Negative"] = linkedMapOf("f1-score" to 0.9993197278911564)
        origClassificationReport["Negative"] = linkedMapOf("precision" to 0.9989799387963277)
        origClassificationReport["Negative"] = linkedMapOf("recall" to 0.9996597482136781)
        origClassificationReport["Negative"] = linkedMapOf("support" to 2939.0)

        newClassificationReport["BookRestaurant"] = linkedMapOf("f1-score" to 0.998000)
        newClassificationReport["BookRestaurant"] = linkedMapOf("precision" to 0.999000)
        newClassificationReport["BookRestaurant"] = linkedMapOf("recall" to 0.997000)
        newClassificationReport["BookRestaurant"] = linkedMapOf("support" to 624.0)
        newClassificationReport["GetWeather"] = linkedMapOf("f1-score" to 0.997000)
        newClassificationReport["GetWeather"] = linkedMapOf("precision" to 0.999000)
        newClassificationReport["GetWeather"] = linkedMapOf("recall" to 0.995000)
        newClassificationReport["GetWeather"] = linkedMapOf("support" to 597.0)
        newClassificationReport["Negative"] = linkedMapOf("f1-score" to 0.9994000)
        newClassificationReport["Negative"] = linkedMapOf("precision" to 0.999000)
        newClassificationReport["Negative"] = linkedMapOf("recall" to 0.9997000)
        newClassificationReport["Negative"] = linkedMapOf("support" to 2800.0)

        origCorpus["What will the weather be this year in Horseshoe Lake State Fish and Wildlife Area?"] = "GetWeather"
        origCorpus["Book a bistro in New Zealand in 119 and a half days."] = "BookRestaurant"
        origCorpus["add how to my week end playlist"] = "Negative"

        newCorpus["What will the weather be this year in Horseshoe Lake State Fish and Wildlife Area?"] = "GetWeather"
        newCorpus["Will it snowstorm neighboring the Rio Grande Wild and Scenic River on feb. the second?"] = "GetWeather"
        newCorpus["What will the weather be this year in Horseshoe Lake State Fish and Wildlife Area?"] = "GetWeather"
        newCorpus["Will it be sunny one hundred thirty five days from now in Monterey Bay National Marine Sanctuary"] = "GetWeather"
        newCorpus["Is it supposed to rain nearby my current location at 0 o'clock?"] = "GetWeather"
        newCorpus["what is the forecast starting on september 1, 2039 for chillier conditions in AK"] = "GetWeather"
        newCorpus["how cold is it in Princeton Junction"] = "GetWeather"
        newCorpus["weather in Nationalpark Nevado Tres Cruces on mar. 4th, 2020"] = "GetWeather"
        newCorpus["What will be wind speed in Tiplersville, South Sudan?"] = "GetWeather"
        newCorpus["whats the weather in GA"] = "GetWeather"
        newCorpus["what is the weather at my current location"] = "GetWeather"
        newCorpus["book The Middle East restaurant in IN for noon"] = "BookRestaurant"
        newCorpus["Book a table at T-Rex distant from Halsey St."] = "BookRestaurant"
        newCorpus["I'd like to eat at a taverna that serves chili con carne for a party of 10"] = "BookRestaurant"
        newCorpus["I have a party of four in Japan and need a reservation at Rimsky-Korsakoffee House on Aug. the 3rd."] = "BookRestaurant"
        newCorpus["Please make a restaurant reservation for somewhere in Mondovi, Connecticut."] = "BookRestaurant"
        newCorpus["book a spot far from land"] = "BookRestaurant"
        newCorpus["I'd like to eat at the best restaurant in Coalton"] = "BookRestaurant"
        newCorpus["book a taverna that serves vichyssoise within walking distance in OH"] = "BookRestaurant"
        newCorpus["I want to book a popular tyrolean steakhouse in Madison Park WA in 1 hour nine minutes and one second"] = "BookRestaurant"
        newCorpus["Find a table for 8 somewhere in Bonaire in 345 days"] = "BookRestaurant"
        newCorpus["Book a restaurant with parking facility for 3."] = "BookRestaurant"
        newCorpus["Add another song to the Cita Romantica playlist."] = "Negative"
        newCorpus["add clem burke in my playlist Pre-Party R&B Jams"] = "Negative"
        newCorpus["Add Live from Aragon Ballroom to Trapeo"] = "Negative"
        newCorpus["add Unite and Win to my night out"] = "Negative"
        newCorpus["Add track to my Digster Future Hits"] = "Negative"
        newCorpus["add the piano bar to my Cindy Wilson"] = "Negative"
        newCorpus["Add Spanish Harlem Incident to cleaning the house"] = "Negative"
        newCorpus["add The Greyest of Blue Skies in Indie Espa±ol my playlist"] = "Negative"
        newCorpus["Add the name kids in the street to the playlist New Indie Mix"] = "Negative"
        newCorpus["add album radar latino"] = "Negative"
        newCorpus["Add Tranquility to the Latin Pop Rising playlist."] = "Negative"
    }

    @After
    fun tearDown() {
        mockNetwork.stopNodes()
    }

    @Test
    fun flowReturnsCorrectlyFormedPartiallySignedTransaction() {
        val creator = a.info.chooseIdentityAndCert().party
        val otherParty = b.info.chooseIdentityAndCert().party
        val flow = IssueCorpusFlow(
                algorithmUsed = "Passive Aggressive",
                classificationURL = "http://127.0.0.1:5000/classify",
                corpus = newCorpus,
                participants = listOf(otherParty)
        )
        val future = a.startFlow(flow)
        mockNetwork.runNetwork()
        // Return the unsigned(!) SignedTransaction object from the IOUIssueFlow.
        val ptx: SignedTransaction = future.getOrThrow()
        // Print the transaction for debugging purposes.
        println(ptx.tx)
        // Check the transaction is well formed...
        // No outputs, one input IOUState and a command with the right properties.
        assert(ptx.tx.inputs.isEmpty())
        assert(ptx.tx.outputs.single().data is CorpusState)
        val command = ptx.tx.commands.single()
        assert(command.value is CorpusContract.Commands.Issue)
        assert(creator.owningKey in command.signers)
        ptx.verifySignaturesExcept(
                otherParty.owningKey,
                mockNetwork.defaultNotaryNode.info.legalIdentitiesAndCerts.first().owningKey
        )
    }

    @Test
    fun flowReturnsVerifiedPartiallySignedTransaction() {
        // Check that an empty corpus fails
        a.info.chooseIdentityAndCert().party
        val otherParty = b.info.chooseIdentityAndCert().party
        val emptyCorpus = LinkedHashMap<String, String>()
        val emptyCorpusFlow = IssueCorpusFlow(
                algorithmUsed = "Passive Aggressive",
                classificationURL = "http://127.0.0.1:5000/classify",
                corpus = emptyCorpus,
                participants = listOf(otherParty)
        )
        val futureOne = a.startFlow(emptyCorpusFlow)
        mockNetwork.runNetwork()
        assertFailsWith<JsonSyntaxException> { futureOne.getOrThrow() }

        // Check a good CorpusState passes.
        val futureTwo = a.startFlow(IssueCorpusFlow(
                algorithmUsed = "Passive Aggressive",
                classificationURL = "http://127.0.0.1:5000/classify",
                corpus = newCorpus,
                participants = listOf(otherParty)
        ))
        mockNetwork.runNetwork()
        futureTwo.getOrThrow()
    }

    @Test
    fun flowReturnsTransactionSignedByBothParties() {
        val otherParty = b.info.chooseIdentityAndCert().party
        val flow = IssueCorpusFlow(
                algorithmUsed = "Passive Aggressive",
                classificationURL = "http://127.0.0.1:5000/classify",
                corpus = newCorpus,
                participants = listOf(otherParty)
        )
        val future = a.startFlow(flow)
        mockNetwork.runNetwork()
        val stx = future.getOrThrow()
        stx.verifyRequiredSignatures()
    }

    @Test
    fun flowRecordsTheSameTransactionInBothPartyVaults() {
        val otherParty = b.info.chooseIdentityAndCert().party
        val flow = IssueCorpusFlow(
                algorithmUsed = "Passive Aggressive",
                classificationURL = "http://127.0.0.1:5000/classify",
                corpus = newCorpus,
                participants = listOf(otherParty)
        )
        val future = a.startFlow(flow)
        mockNetwork.runNetwork()
        val stx = future.getOrThrow()
        println("Signed transaction hash: ${stx.id}")
        listOf(a, b).map {
            it.services.validatedTransactions.getTransaction(stx.id)
        }.forEach {
            val txHash = (it as SignedTransaction).id
            println("$txHash == ${stx.id}")
            assertEquals(stx.id, txHash)
        }
    }
}