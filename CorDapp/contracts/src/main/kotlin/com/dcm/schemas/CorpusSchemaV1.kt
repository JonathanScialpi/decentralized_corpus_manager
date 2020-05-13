package com.dcm.schemas

import net.corda.core.schemas.MappedSchema

class CorpusSchemaV1 : MappedSchema(CorpusSchemaFamily::class.java, 1, listOf(PersistentCorpus::class.java))
