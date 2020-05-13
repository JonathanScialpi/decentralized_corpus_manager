package net.corda.training.com.dcm.state

import com.dcm.states.CorpusState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.training.ALICE
import net.corda.training.BOB
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class CorpusStateTests {
    private var origClassificationReport = LinkedHashMap<String, LinkedHashMap<String, Double>>()
    private var newClassificationReport =  LinkedHashMap<String, LinkedHashMap<String, Double>>()
    private var origCorpus =  LinkedHashMap<String, String>()
    private var newCorpus =  LinkedHashMap<String, String>()

    @Before
    fun setup(){
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
        newCorpus["Book a bistro in New Zealand in 119 and a half days."] = "BookRestaurant"
        newCorpus["Book a table at T-Rex distant from Halsey St."] = "BookRestaurant"
        newCorpus["add how to my week end playlist"] = "Negative"
        newCorpus["add clem burke in my playlist Pre-Party R&B Jams"] = "Negative"
    }

    @Test
    fun hasAllFieldsAndProperTypes(){
        CorpusState::class.java.getDeclaredField("status")
        assertEquals(CorpusState::class.java.getDeclaredField("status").type, String::class.java)

        CorpusState::class.java.getDeclaredField("algorithmUsed")
        assertEquals(CorpusState::class.java.getDeclaredField("algorithmUsed").type, String::class.java)

        CorpusState::class.java.getDeclaredField("classificationURL")
        assertEquals(CorpusState::class.java.getDeclaredField("classificationURL").type, String::class.java)

        CorpusState::class.java.getDeclaredField("corpus")
        assertEquals(CorpusState::class.java.getDeclaredField("corpus").type, LinkedHashMap::class.java)

        CorpusState::class.java.getDeclaredField("classificationReport")
        assertEquals(CorpusState::class.java.getDeclaredField("classificationReport").type, LinkedHashMap::class.java)

        CorpusState::class.java.getDeclaredField("participants")
        assertEquals(CorpusState::class.java.getDeclaredField("participants").type, List::class.java)

        CorpusState::class.java.getDeclaredField("linearId")
        assertEquals(CorpusState::class.java.getDeclaredField("linearId").type, UniqueIdentifier::class.java)
    }

    @Test
    fun checkReplaceCorpusCorpus(){
        val corpus = CorpusState(
                status = "Open",
                algorithmUsed = "Passive Aggressive",
                classificationURL = "http://127.0.0.1:5000/classify",
                corpus = origCorpus,
                classificationReport = origClassificationReport,
                owner = ALICE.party,
                participants = listOf(ALICE.party, BOB.party)
        )

        assertEquals(newCorpus, corpus.replaceCorpusCorpus(newCorpus).corpus)
    }

    @Test
    fun checkReplaceCorpusClassificationReport(){
        val corpus = CorpusState(
                status = "Open",
                algorithmUsed = "Passive Aggressive",
                classificationURL = "http://127.0.0.1:5000/classify",
                corpus = origCorpus,
                classificationReport = origClassificationReport,
                owner = ALICE.party,
                participants = listOf(ALICE.party, BOB.party)
        )

        assertEquals(newClassificationReport, corpus.replaceClassificationReport(newClassificationReport).classificationReport)
    }
}