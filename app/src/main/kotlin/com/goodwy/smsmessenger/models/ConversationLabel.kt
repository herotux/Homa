package com.goodwy.smsmessenger.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(tableName = "conversation_labels", primaryKeys = ["thread_id", "label_id"], indices = [Index("label_id")])
data class ConversationLabel(
    @ColumnInfo(name = "thread_id") val threadId: Long,
    @ColumnInfo(name = "label_id") val labelId: Long,
)
