package com.dcm.State

import com.dcm.states.ModelState
import net.corda.core.contracts.UniqueIdentifier
import org.junit.Test
import kotlin.test.assertEquals

class ModelStateTests {
    @Test
    fun hasAllFieldsAndProperTypes(){
        ModelState::class.java.getDeclaredField("corpus")
        assertEquals(ModelState::class.java.getDeclaredField("corpus").type, LinkedHashMap::class.java)

        ModelState::class.java.getDeclaredField("classificationReport")
        assertEquals(ModelState::class.java.getDeclaredField("classificationReport").type, LinkedHashMap::class.java)

        ModelState::class.java.getDeclaredField("participants")
        assertEquals(ModelState::class.java.getDeclaredField("participants").type, List::class.java)

        ModelState::class.java.getDeclaredField("linearId")
        assertEquals(ModelState::class.java.getDeclaredField("linearId").type, UniqueIdentifier::class.java)
    }
}