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
@param corpus List of DataRow State Objects.
@param participants A list of party objects whom will act as gatekeepers of all the model's properties.
@param modelID A unique identifier for reference.
 *********/
@BelongsToContract(ModelContract::class)
data class ModelState(
        val corpus: LinkedHashMap<String, String>,
        val labelsAndScoreMap: LinkedHashMap<String, LinkedHashMap<String, Double>>,
        override val participants: List<Party>,
        override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState{

    fun addDataRows(dataRowsToAdd: LinkedHashMap<String, String>) : ModelState{
        val newCorpus = LinkedHashMap(corpus)
        dataRowsToAdd.map{newCorpus.put(it.key, it.value)}
        return copy(corpus = newCorpus)
    }

    fun removeDataRows(dataRowsToRemove: LinkedHashMap<String, String>) : ModelState{
        val newCorpus = LinkedHashMap(corpus)
        dataRowsToRemove.map{newCorpus.remove(it.key)}
        return copy(corpus = newCorpus)
    }
}
