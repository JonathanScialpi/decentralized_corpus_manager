package com.dcm.schemas

import net.corda.core.schemas.MappedSchema

class ModelSchemaV1 : MappedSchema(ModelSchemaFamily::class.java, 1, listOf(PersistentModel::class.java))
