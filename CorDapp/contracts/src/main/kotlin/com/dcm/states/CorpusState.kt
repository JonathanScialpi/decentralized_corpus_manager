package com.dcm.states

import com.dcm.contract.CorpusContract
import com.dcm.schemas.CorpusSchemaV1
import com.dcm.schemas.PersistentCorpus
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

/*********
@Dev: The Corpus State Object represents the characteristics commonly found in Machine Learning Models.
@Param status (set/modified by owner only)- Initially set to "Open" for contributions and can be "Closed" when the owner no longer wishes to receive data contributions which exits the state.
@Param algorithmUsed (set/modified by owner only)- pertains to the method used to train a corpus. Example: "Naive Bayes".
@Param classificationURL (set/modified by owner only)- is the endpoint that actually consumes the corpus to produce the classification report.
@Param corpus (set by owner and modified by any participant)- each intent and its relative label -> intent:label.
@Param classificationReport (set/modified only by classification endpoint)- the classification report for each label.
@Param owner is the user who issued the CorpusState and the one with access to modify: status, algorithmUsed, and classificationURL.
    Example:
        {
            "MyLabelA": {
            "precision": 0.9983108108108109,
            "recall": 0.9949494949494949,
            "f1-score": 0.9966273187183812,
            "support": 594
            }
        }
@param participants all contributors to the data set.
 *********/
@BelongsToContract(CorpusContract::class)
data class CorpusState(
        val status: String,
        val algorithmUsed: String,
        val classificationURL : String,
        val classificationUpdateURL : String,
        val corpus: LinkedHashMap<String, String>,
        val classificationReport: LinkedHashMap<String, LinkedHashMap<String, Double>>,
        val owner: Party,
        override val participants: List<Party>,
        override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, QueryableState{

    fun setClosedStatus(): CorpusState{
        return copy(status = "Closed")
    }

    fun replaceClassificationURL(newURL : String) : CorpusState{
        return copy(classificationURL = newURL)
    }

    fun replaceClassificationURL(newURL : String) : CorpusState{
        return copy(classificationUpdateURL = newURL)
    }

    fun replaceOwner(newOwner: Party) : CorpusState{
        return copy(owner = newOwner)
    }

    fun replaceCorpus(newCorpus: LinkedHashMap<String, String>) : CorpusState{
        return copy(corpus = newCorpus)
    }

    fun replaceClassificationReport( newClassificationReport: LinkedHashMap<String, LinkedHashMap<String, Double>>) : CorpusState {
        return copy(classificationReport = newClassificationReport)
    }

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
      if(schema is CorpusSchemaV1){
        return PersistentCorpus(
          status,
          algorithmUsed,
          classificationURL,
          corpus.values.toSet().toString(),
          classificationReport?.get("accuracy")?.get("score"),
          owner,
          linearId.id
        )
      }else{
        throw IllegalArgumentException("Unsupported Schema")
      }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> {
      return listOf(CorpusSchemaV1())
    }

}
