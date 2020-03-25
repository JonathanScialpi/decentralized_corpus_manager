package net.corda.training.com.dcm.contract

import com.dcm.contract.ModelContract
import com.dcm.states.ModelState
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import net.corda.training.ALICE
import net.corda.training.BOB
import org.junit.Test

class ModelIssueTests {
    // A pre-defined dummy command.
    class DummyCommand : TypeOnlyCommandData()
    private var ledgerServices = MockServices(listOf("com.dcm"))
    @Test
    fun mustIncludeIssueCommand() {
        var testClassificationReport : LinkedHashMap<String, LinkedHashMap<String, Double>> =  LinkedHashMap<String, LinkedHashMap<String, Double>>()
        testClassificationReport["BookRestaurant"] = linkedMapOf("f1-score" to 0.9975103734439834)
        testClassificationReport["BookRestaurant"] = linkedMapOf("precision" to 0.9983388704318937)
        testClassificationReport["BookRestaurant"] = linkedMapOf("recall" to 0.9966832504145937)
        testClassificationReport["BookRestaurant"] = linkedMapOf("support" to 603.0)
        testClassificationReport["GetWeather"] = linkedMapOf("f1-score" to 0.9966273187183812)
        testClassificationReport["GetWeather"] = linkedMapOf("precision" to 0.9983108108108109)
        testClassificationReport["GetWeather"] = linkedMapOf("recall" to 0.9949494949494949)
        testClassificationReport["GetWeather"] = linkedMapOf("support" to 594.0)
        testClassificationReport["Negative"] = linkedMapOf("f1-score" to 0.9993197278911564)
        testClassificationReport["Negative"] = linkedMapOf("precision" to 0.9989799387963277)
        testClassificationReport["Negative"] = linkedMapOf("recall" to 0.9996597482136781)
        testClassificationReport["Negative"] = linkedMapOf("support" to 2939.0)

        var testCorpus : LinkedHashMap<String, String> =  LinkedHashMap<String, String>()
        testCorpus["What will the weather be this year in Horseshoe Lake State Fish and Wildlife Area?"] = "GetWeather"
        testCorpus["Book a bistro in New Zealand in 119 and a half days."] = "BookRestaurant"
        testCorpus["add how to my week end playlist"] = "Negative"

        val model = ModelState(
                corpus = testCorpus,
                classificationReport = testClassificationReport,
                participants = listOf(ALICE.party, BOB.party)
        )
        ledgerServices.ledger {
            transaction {
                output(ModelContract.ID,  model)
                command(listOf(ALICE.publicKey, BOB.publicKey), DummyCommand()) // Wrong type.
                this.fails()
            }
            transaction {
                output(ModelContract.ID, model)
                command(listOf(ALICE.publicKey, BOB.publicKey), ModelContract.Commands.Issue()) // Correct type.
                this.verifies()
            }
        }
    }
}