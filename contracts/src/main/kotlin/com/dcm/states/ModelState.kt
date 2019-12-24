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
data class ModelState(val corpus: List<LinearState>,
                      val dataRowMap : LinkedHashMap<String, DataRowState>,
                      override val participants: List<Party>,
                      override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState{

    fun addDataRows(dataRowsToAdd: List<DataRowState>) : ModelState{
        val newDataRowMap = LinkedHashMap(dataRowMap)
        dataRowsToAdd.map{newDataRowMap.put(it.dataRow, it)}
        return copy(corpus = corpus.plus(dataRowsToAdd).distinct(), dataRowMap = newDataRowMap)
    }

    fun removeDataRows(dataRowsToRemove: List<DataRowState>) : ModelState{
        var newDataRowMap = LinkedHashMap(dataRowMap)
        dataRowsToRemove.map{newDataRowMap.remove(it.dataRow, it)}
        return copy(corpus = corpus.minus(dataRowsToRemove), dataRowMap = newDataRowMap)
    }
    fun addGateKeepers(gateKeepersToAdd: Party) = copy(participants = participants.plus(gateKeepersToAdd).distinct())
    fun removeGateKeepers(gateKeepersToRemove: Party) = copy(participants = participants.minus(gateKeepersToRemove))
}
