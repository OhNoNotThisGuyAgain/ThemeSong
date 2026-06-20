package com.spotzones.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.spotzones.domain.model.Action
import com.spotzones.domain.model.Condition
import com.spotzones.domain.model.Rule

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val enabled: Boolean,
    val priority: Int,
    val condition: Condition,
    val action: Action,
    val createdAt: Long,
    val updatedAt: Long,
)

fun RuleEntity.toDomain(): Rule = Rule(id, name, enabled, priority, condition, action, createdAt, updatedAt)

fun Rule.toEntity(): RuleEntity = RuleEntity(id, name, enabled, priority, condition, action, createdAt, updatedAt)
