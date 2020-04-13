package com.dcm.schemas

import net.corda.core.identity.Party
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table


@Entity
@Table(name = "MODEL_STATES")
class PersistentModel(

        @Column(name = "STATUS")
        val status: String,

        @Column(name = "ALGORITHM_USED")
        val algorithmUsed: String,

        @Column(name = "CLASSIFICATION_URL")
        val classificationURL: String,

        @Column(name = "CORPUS_LABELS")
        val corpus: String,

        @Column(name = "ACCURACY")
        val accuracy: Double?,

        @Column(name = "OWNER")
        val owner: Party,

        @Column(name = "linearId")
        val linearId: UUID
) : PersistentState() {}
