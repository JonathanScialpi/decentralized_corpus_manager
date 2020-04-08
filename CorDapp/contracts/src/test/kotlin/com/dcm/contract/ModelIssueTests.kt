package net.corda.training.com.dcm.contract

import com.dcm.contract.ModelContract
import com.dcm.states.ModelState
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import net.corda.training.ALICE
import net.corda.training.BOB
import org.junit.Before
import org.junit.Test

class ModelIssueTests {
    // A pre-defined dummy command.
    class DummyCommand : TypeOnlyCommandData()
    private var ledgerServices = MockServices(listOf("com.dcm"))
    private var origClassificationReport = LinkedHashMap<String, LinkedHashMap<String, Double>>()
    private var newClassificationReport = LinkedHashMap<String, LinkedHashMap<String, Double>>()
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
    fun mustIncludeIssueCommand() {
        val model = ModelState(
                status = "Open",
                algorithmUsed = "Passive Aggressive",
                classificationURL = "http://127.0.0.1:5000/classify",
                corpus = origCorpus,
                classificationReport = origClassificationReport,
                owner = ALICE.party,
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

    @Test
    fun mustNotHaveInputStates(){
        val inputModelState = ModelState(
                status = "Open",
                algorithmUsed = "Passive Aggressive",
                classificationURL = "http://127.0.0.1:5000/classify",
                corpus = origCorpus,
                classificationReport = origClassificationReport,
                owner = ALICE.party,
                participants = listOf(ALICE.party, BOB.party)
        )
        val outputModelState = ModelState(
                status = "Open",
                algorithmUsed = "Passive Aggressive",
                classificationURL = "http://127.0.0.1:5000/classify",
                corpus = newCorpus,
                classificationReport = newClassificationReport,
                owner = ALICE.party,
                participants = listOf(ALICE.party, BOB.party)
        )
        ledgerServices.ledger {
            transaction {
                input(ModelContract.ID, inputModelState)
                output(ModelContract.ID,  outputModelState)
                command(listOf(ALICE.publicKey, BOB.publicKey),  ModelContract.Commands.Issue())
                this `fails with` "No inputs should be consumed when issuing a Model."
            }
            transaction {
                output(ModelContract.ID, outputModelState)
                command(listOf(ALICE.publicKey, BOB.publicKey), ModelContract.Commands.Issue())
                this.verifies()
            }
        }
    }

    @Test
    fun mustHaveOneOutputState(){
        val outputModelStateOne = ModelState(
                status = "Open",
                algorithmUsed = "Passive Aggressive",
                classificationURL = "http://127.0.0.1:5000/classify",
                corpus = origCorpus,
                classificationReport = origClassificationReport,
                owner = ALICE.party,
                participants = listOf(ALICE.party, BOB.party)
        )
        val outputModelStateTwo = ModelState(
                status = "Open",
                algorithmUsed = "Passive Aggressive",
                classificationURL = "http://127.0.0.1:5000/classify",
                corpus = newCorpus,
                classificationReport = newClassificationReport,
                owner = ALICE.party,
                participants = listOf(ALICE.party, BOB.party)
        )
        ledgerServices.ledger {
            transaction {
                output(ModelContract.ID, outputModelStateOne)
                output(ModelContract.ID,  outputModelStateTwo)
                command(listOf(ALICE.publicKey, BOB.publicKey),  ModelContract.Commands.Issue())
                this `fails with` "Only one output state should be created when issuing a Model."
            }
            transaction {
                output(ModelContract.ID, outputModelStateTwo)
                command(listOf(ALICE.publicKey, BOB.publicKey), ModelContract.Commands.Issue())
                this.verifies()
            }
        }
    }

    @Test
    fun mustHaveAtLeastOneParty(){
        val outputModelStateNoParties = ModelState(
                status = "Open",
                algorithmUsed = "Passive Aggressive",
                classificationURL = "http://127.0.0.1:5000/classify",
                corpus = newCorpus,
                classificationReport = newClassificationReport,
                owner = ALICE.party,
                participants = listOf()
        )

        val outputModelState = ModelState(
                status = "Open",
                algorithmUsed = "Passive Aggressive",
                classificationURL = "http://127.0.0.1:5000/classify",
                corpus = newCorpus,
                classificationReport = newClassificationReport,
                owner = ALICE.party,
                participants = listOf(ALICE.party)
        )
        ledgerServices.ledger {
            transaction {
                output(ModelContract.ID,  outputModelStateNoParties)
                command(listOf(ALICE.publicKey),  ModelContract.Commands.Issue())
                this `fails with` "The participants of a model must have at least one party."
            }
            transaction {
                output(ModelContract.ID, outputModelState)
                command(listOf(ALICE.publicKey), ModelContract.Commands.Issue())
                this.verifies()
            }
        }
    }

    @Test
    fun corpusCannotBeEmpty(){
        var emptyCorpus : LinkedHashMap<String, String> =  LinkedHashMap<String, String>()

        val outputModelStateEmptyCorpus = ModelState(
                status = "Open",
                algorithmUsed = "Passive Aggressive",
                classificationURL = "http://127.0.0.1:5000/classify",
                corpus = emptyCorpus,
                classificationReport = newClassificationReport,
                owner = ALICE.party,
                participants = listOf(ALICE.party, BOB.party)
        )

        val outputModelState = ModelState(
                status = "Open",
                algorithmUsed = "Passive Aggressive",
                classificationURL = "http://127.0.0.1:5000/classify",
                corpus = newCorpus,
                classificationReport = newClassificationReport,
                owner = ALICE.party,
                participants = listOf(ALICE.party, BOB.party)
        )
        ledgerServices.ledger {
            transaction {
                output(ModelContract.ID,  outputModelStateEmptyCorpus)
                command(listOf(ALICE.publicKey, BOB.publicKey),  ModelContract.Commands.Issue())
                this `fails with` "The corpus cannot be empty."
            }
            transaction {
                output(ModelContract.ID, outputModelState)
                command(listOf(ALICE.publicKey, BOB.publicKey), ModelContract.Commands.Issue())
                this.verifies()
            }
        }
    }

    @Test
    fun classificationReportCannotBeEmpty(){
        var emptyClassificationReport = LinkedHashMap<String, LinkedHashMap<String, Double>>()

        val outputModelStateEmptyClassificationReport = ModelState(
                status = "Open",
                algorithmUsed = "Passive Aggressive",
                classificationURL = "http://127.0.0.1:5000/classify",
                corpus = origCorpus,
                classificationReport = emptyClassificationReport,
                owner = ALICE.party,
                participants = listOf(ALICE.party, BOB.party)
        )

        val outputModelState = ModelState(
                status = "Open",
                algorithmUsed = "Passive Aggressive",
                classificationURL = "http://127.0.0.1:5000/classify",
                corpus = newCorpus,
                classificationReport = newClassificationReport,
                owner = ALICE.party,
                participants = listOf(ALICE.party, BOB.party)
        )
        ledgerServices.ledger {
            transaction {
                output(ModelContract.ID,  outputModelStateEmptyClassificationReport)
                command(listOf(ALICE.publicKey, BOB.publicKey),  ModelContract.Commands.Issue())
                this `fails with` "The classification report cannot be empty."
            }
            transaction {
                output(ModelContract.ID, outputModelState)
                command(listOf(ALICE.publicKey, BOB.publicKey), ModelContract.Commands.Issue())
                this.verifies()
            }
        }
    }

    @Test
    fun ownerMustSign(){
        val outputModelStateNoParties = ModelState(
                status = "Open",
                algorithmUsed = "Passive Aggressive",
                classificationURL = "http://127.0.0.1:5000/classify",
                corpus = newCorpus,
                classificationReport = newClassificationReport,
                owner = ALICE.party,
                participants = listOf(ALICE.party, BOB.party)
        )

        val outputModelState = ModelState(
                status = "Open",
                algorithmUsed = "Passive Aggressive",
                classificationURL = "http://127.0.0.1:5000/classify",
                corpus = newCorpus,
                classificationReport = newClassificationReport,
                owner = ALICE.party,
                participants = listOf(ALICE.party)
        )
        ledgerServices.ledger {
            transaction {
                output(ModelContract.ID,  outputModelStateNoParties)
                command(listOf(BOB.publicKey),  ModelContract.Commands.Issue())
                this `fails with` "The owner must be included in the list of signers."
            }
            transaction {
                output(ModelContract.ID, outputModelState)
                command(listOf(ALICE.publicKey, BOB.publicKey), ModelContract.Commands.Issue())
                this.verifies()
            }
        }
    }
}