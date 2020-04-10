package com.dcm.schemas

import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import java.io.Serializable
import net.corda.core.schemas.PersistentState
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table


@Entity
@Table(name = "MODEL_STATES")
class PersistentModel(
  @Column(name = "status")
  val status: String,

  @Column(name = "algorithmUsed")
  val algorithmUsed: String,

  @Column(name = "classificationURL")
  val classificationURL : String,

  @Column(name = "corpus")
  val corpus: LinkedHashMap<String, String>,

  @Column(name = "classificationReport")
  val classificationReport: LinkedHashMap<String, LinkedHashMap<String, Double>>,

  @Column(name = "owner")
  val owner: Party,

  @Column(name = "linearId")
  val linearId: UniqueIdentifier
) : PersistentState() {}
