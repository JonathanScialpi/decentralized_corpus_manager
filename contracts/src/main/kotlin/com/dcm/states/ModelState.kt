package com.dcm.states

import com.dcm.contract.ModelContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import java.util.*

/*********
@Dev: The Model State Object represents the characteristics commonly found in Machine Learning Models.
@param corpus List of DataRow State Objects.
@param participants A list of party objects whom will act as gatekeepers of all the model's properties.
@param modelID A unique identifier for reference.
 *********/
@BelongsToContract(ModelContract::class)
data class ModelState(val corpus: List<LinearState>,
                      override val participants: List<Party>,
                      override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState{

    fun addDataRows(dataRowsToAdd: List<DataRowState>) = copy(corpus = corpus.plus(dataRowsToAdd).distinct())
    fun removeDataRows(dataRowsToRemove: List<DataRowState>) = copy(corpus = corpus.minus(dataRowsToRemove))
    fun addGateKeepers(gateKeepersToAdd: Party) = copy(participants = participants.plus(gateKeepersToAdd).distinct())
    fun removeGateKeepers(gateKeepersToRemove: Party) = copy(participants = participants.minus(gateKeepersToRemove))
}
