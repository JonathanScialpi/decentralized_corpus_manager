package com.dcm.flow

import com.dcm.flows.CorpusIssueFlowResponder
import com.dcm.flows.IssueCorpusFlow
import com.dcm.flows.UpdateClassificationURL
import com.dcm.states.CorpusState
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.internal.chooseIdentityAndCert
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.MockNodeParameters
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith

class UpdateClassificationURLFlowTests {
    private lateinit var mockNetwork: MockNetwork
    private lateinit var a: StartedMockNode
    private lateinit var b: StartedMockNode
    private var origClassificationReport = LinkedHashMap<String, LinkedHashMap<String, Double>>()
    private var newClassificationReport = LinkedHashMap<String, LinkedHashMap<String, Double>>()
    private var origCorpus = LinkedHashMap<String, String>()
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
        origCorpus["Will it be sunny one hundred thirty five days from now in Monterey Bay National Marine Sanctuary"] = "GetWeather"
        origCorpus["Is it supposed to rain nearby my current location at 0 o'clock?"] = "GetWeather"
        origCorpus["what is the forecast starting on september 1, 2039 for chillier conditions in AK"] = "GetWeather"
        origCorpus["how cold is it in Princeton Junction"] = "GetWeather"
        origCorpus["weather in Nationalpark Nevado Tres Cruces on mar. 4th, 2020"] = "GetWeather"
        origCorpus["What will be wind speed in Tiplersville, South Sudan?"] = "GetWeather"
        origCorpus["whats the weather in GA"] = "GetWeather"
        origCorpus["what is the weather at my current location"] = "GetWeather"

        origCorpus["book The Middle East restaurant in IN for noon"] = "BookRestaurant"
        origCorpus["Book a table at T-Rex distant from Halsey St."] = "BookRestaurant"
        origCorpus["I'd like to eat at a taverna that serves chili con carne for a party of 10"] = "BookRestaurant"
        origCorpus["I have a party of four in Japan and need a reservation at Rimsky-Korsakoffee House on Aug. the 3rd."] = "BookRestaurant"
        origCorpus["Please make a restaurant reservation for somewhere in Mondovi, Connecticut."] = "BookRestaurant"
        origCorpus["book a spot far from Ãland"] = "BookRestaurant"
        origCorpus["I'd like to eat at the best restaurant in Coalton"] = "BookRestaurant"
        origCorpus["book a taverna that serves vichyssoise within walking distance in OH"] = "BookRestaurant"
        origCorpus["I want to book a popular tyrolean steakhouse in Madison Park WA in 1 hour nine minutes and one second"] = "BookRestaurant"
        origCorpus["Find a table for 8 somewhere in Bonaire in 345 days"] = "BookRestaurant"
        origCorpus["Book a restaurant with parking facility for 3."] = "BookRestaurant"

        origCorpus["Add another song to the Cita Romantica playlist."] = "Negative"
        origCorpus["add clem burke in my playlist Pre-Party R&B Jams"] = "Negative"
        origCorpus["Add Live from Aragon Ballroom to Trapeo"] = "Negative"
        origCorpus["add Unite and Win to my night out"] = "Negative"
        origCorpus["Add track to my Digster Future Hits"] = "Negative"
        origCorpus["add the piano bar to my Cindy Wilson"] = "Negative"
        origCorpus["Add Spanish Harlem Incident to cleaning the house"] = "Negative"
        origCorpus["add The Greyest of Blue Skies in Indie EspaÃ±ol my playlist"] = "Negative"
        origCorpus["Add the name kids in the street to the plylist New Indie Mix"] = "Negative"
        origCorpus["add album radar latino"] = "Negative"
        origCorpus["Add Tranquility to the Latin Pop Rising playlist."] = "Negative"

        newCorpus["What will the weather be this year in Horseshoe Lake State Fish and Wildlife Area?"] = "GetWeather"
        newCorpus["Will it be sunny one hundred thirty five days from now in Monterey Bay National Marine Sanctuary"] = "GetWeather"
        newCorpus["Is it supposed to rain nearby my current location at 0 o'clock?"] = "GetWeather"
        newCorpus["what is the forecast starting on september 1, 2039 for chillier conditions in AK"] = "GetWeather"
        newCorpus["how cold is it in Princeton Junction"] = "GetWeather"
        newCorpus["weather in Nationalpark Nevado Tres Cruces on mar. 4th, 2020"] = "GetWeather"
        newCorpus["What will be wind speed in Tiplersville, South Sudan?"] = "GetWeather"
        newCorpus["whats the weather in GA"] = "GetWeather"
        newCorpus["what is the weather at my current location"] = "GetWeather"
        newCorpus["Will it snow in Haigler Bosnia and Herzegovina"] = "GetWeather"
        newCorpus["What is the weather in Aland 4 seconds from now"] = "GetWeather"
        newCorpus["Will it snowstorm neighboring the Rio Grande Wild and Scenic River on feb. the second?"] = "GetWeather"
        newCorpus["what is the Sri Lanka forecast for snow"] = "GetWeather"
        newCorpus["How is the weather going to be in Pearblossom."] = "GetWeather"
        newCorpus["Can you tell me if it'll be freezing in Wrightstown in seven years ?"] = "GetWeather"
        newCorpus["Is the weather going to be colder in GU in 11 years"] = "GetWeather"
        newCorpus["Will there be a snowstorm at my current place?"] = "GetWeather"
        newCorpus["Is it going to be warmer in Central Cebu Protected Landscape"] = "GetWeather"
        newCorpus["What will the weather be in Federated States Of Micronesia at 00:17 am"] = "GetWeather"
        newCorpus["Will it be chillier at 06:05:48 in Wagener RÃ©union"] = "GetWeather"

        newCorpus["book The Middle East restaurant in IN for noon"] = "BookRestaurant"
        newCorpus["Book a table at T-Rex distant from Halsey St."] = "BookRestaurant"
        newCorpus["I'd like to eat at a taverna that serves chili con carne for a party of 10"] = "BookRestaurant"
        newCorpus["I have a party of four in Japan and need a reservation at Rimsky-Korsakoffee House on Aug. the 3rd."] = "BookRestaurant"
        newCorpus["Please make a restaurant reservation for somewhere in Mondovi, Connecticut."] = "BookRestaurant"
        newCorpus["book a spot far from Aland"] = "BookRestaurant"
        newCorpus["I'd like to eat at the best restaurant in Coalton"] = "BookRestaurant"
        newCorpus["book a taverna that serves vichyssoise within walking distance in OH"] = "BookRestaurant"
        newCorpus["I want to book a popular tyrolean steakhouse in Madison Park WA in 1 hour nine minutes and one second"] = "BookRestaurant"
        newCorpus["Find a table for 8 somewhere in Bonaire in 345 days"] = "BookRestaurant"
        newCorpus["Book a restaurant with parking facility for 3."] = "BookRestaurant"
        newCorpus["please book a room in Spaghetti Warehouse for catalina, delores and brandie mendoza at 12 AM"] = "BookRestaurant"
        newCorpus["I need a table for 1 at a highly rated restaurant next autumn in Emmons, RI"] = "BookRestaurant"
        newCorpus["book a spot in 1 second that is neighboring robin's hotel"] = "BookRestaurant"
        newCorpus["patty and I need a table booked at a highly rated restaurant in Sandstone."] = "BookRestaurant"
        newCorpus["Make a reservation for four at a pub in Sugarville."] = "BookRestaurant"
        newCorpus["book a table for a Macedonia restaurant"] = "BookRestaurant"
        newCorpus["I'd like to go to a halal restaurant in twenty minutes around the District Of Columbia and book seats for four"] = "BookRestaurant"
        newCorpus["Book a table for 10 people at Dunbrody Country House Hotel in Strandburg."] = "BookRestaurant"
        newCorpus["Book a table in a Haines Borough restaurant for nine that is within walking distance."] = "BookRestaurant"
        newCorpus["book midday at a faraway cuban place for five at a top-rated bakery in Grainola"] = "BookRestaurant"

        newCorpus["Add another song to the Cita Romantica playlist."] = "Negative"
        newCorpus["add clem burke in my playlist Pre-Party R&B Jams"] = "Negative"
        newCorpus["Add Live from Aragon Ballroom to Trapeo"] = "Negative"
        newCorpus["add Unite and Win to my night out"] = "Negative"
        newCorpus["Add track to my Digster Future Hits"] = "Negative"
        newCorpus["add the piano bar to my Cindy Wilson"] = "Negative"
        newCorpus["Add Spanish Harlem Incident to cleaning the house"] = "Negative"
        newCorpus["add The Greyest of Blue Skies in Indie Espa±ol my playlist"] = "Negative"
        newCorpus["Add the name kids in the street to the plylist New Indie Mix"] = "Negative"
        newCorpus["add album radar latino"] = "Negative"
        newCorpus["Add Tranquility to the Latin Pop Rising playlist."] = "Negative"
        newCorpus["Add d flame to the Dcode2016 playlist."] = "Negative"
        newCorpus["Add album to my fairy tales"] = "Negative"
        newCorpus["I need another artist in the New Indie Mix playlist."] = "Negative"
        newCorpus["Add to playlist i love my neo soul the name national treasure book of secrets"] = "Negative"
        newCorpus["Add the fire and the wind to my Digster Future Hits playlist."] = "Negative"
        newCorpus["add Caleigh Peters to my women of country list"] = "Negative"
        newCorpus["add children of telepathic experiences to the playlist named baladas romÃ¡nticas"] = "Negative"
        newCorpus["add 9th Inning to my bossa nova dinner playlist"] = "Negative"
        newCorpus["I need another tune in my legendary guitar solos playlist."] = "Negative"
        newCorpus["Add Slimm Cutta Calhoun to my this is prince playlist."] = "Negative"
    }

    @After
    fun tearDown() {
        mockNetwork.stopNodes()
    }

    @Test
    fun changeClassificationURL() {
        val creator = a.info.chooseIdentityAndCert().party
        val otherParty = b.info.chooseIdentityAndCert().party
        val flow = IssueCorpusFlow(
                algorithmUsed = "Passive Aggressive",
                classificationURL = "http://127.0.0.1:5000/classify",
                corpus = origCorpus,
                participants = listOf(creator, otherParty)
        )
        val future = a.startFlow(flow)
        mockNetwork.runNetwork()

        val origCorpus = future.getOrThrow().tx.outputs.single().data as CorpusState
        val flowTwo = UpdateClassificationURL(
                newURL = "www.google.com",
                corpusLinearId = origCorpus.linearId
        )
        val futureTwo = a.startFlow(flowTwo)
        mockNetwork.runNetwork()
        futureTwo.getOrThrow()
    }

    @Test
    fun changeClassificationURLToSame() {
        val creator = a.info.chooseIdentityAndCert().party
        val otherParty = b.info.chooseIdentityAndCert().party
        val flow = IssueCorpusFlow(
                algorithmUsed = "Passive Aggressive",
                classificationURL = "http://127.0.0.1:5000/classify",
                corpus = origCorpus,
                participants = listOf(creator, otherParty)
        )
        val future = a.startFlow(flow)
        mockNetwork.runNetwork()

        val origCorpus = future.getOrThrow().tx.outputs.single().data as CorpusState
        val flowTwo = UpdateClassificationURL(
                newURL = "http://127.0.0.1:5000/classify",
                corpusLinearId = origCorpus.linearId
        )
        val futureTwo = a.startFlow(flowTwo)
        mockNetwork.runNetwork()
        assertFailsWith<TransactionVerificationException> {futureTwo.getOrThrow()}
    }
}