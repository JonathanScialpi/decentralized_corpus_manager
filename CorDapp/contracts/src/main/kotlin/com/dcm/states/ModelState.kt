package com.dcm.states

import com.dcm.contract.ModelContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap

/*********
@Dev: The Model State Object represents the characteristics commonly found in Machine Learning Models.
@param algorithm used pertains to the method used to train a model. Ex.: "Naive Bayes"
@param classificationURL is the enpoint that actually consumes the corpus to produce the classification report
@param corpus each intent and its relative label -> intent:label.
@param classificationReport the classification report for each label
For example:
    {
        "MyLabelA": {
        "precision": 0.9983108108108109,
        "recall": 0.9949494949494949,
        "f1-score": 0.9966273187183812,
        "support": 594
        }
    }
@param participants all contributers to the data set.
 *********/
@BelongsToContract(ModelContract::class)
data class ModelState(
        val status: String,
        val algorithmUsed: String,
        val classificationURL : String,
        val corpus: LinkedHashMap<String, String>,
        val classificationReport: LinkedHashMap<String, LinkedHashMap<String, Double>>,
        val owner: Party,
        override val participants: List<Party>,
        override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState{

    fun setClosedStatus(): ModelState{
        return copy(status = "Closed")
    }

    fun replaceClassificationURL(newURL : String) : ModelState{
        return copy(classificationURL = newURL)
    }

    fun replaceOwner(newOwner: Party) : ModelState{
        return copy(owner = newOwner)
    }

    fun replaceModelCorpus(newCorpus: LinkedHashMap<String, String>) : ModelState{
        return copy(corpus = newCorpus)
    }

    fun replaceClassificationReport( newClassificationReport: LinkedHashMap<String, LinkedHashMap<String, Double>>) : ModelState {
        return copy(classificationReport = newClassificationReport)
    }

}
