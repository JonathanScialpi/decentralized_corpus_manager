package com.dcm.schemas

import com.google.common.collect.ImmutableList
import net.corda.core.schemas.MappedSchema


class ModelSchemaV1 : MappedSchema(ModelSchemaFamily::class.java, 1, ImmutableList.of(PersistentModel::class.java))
