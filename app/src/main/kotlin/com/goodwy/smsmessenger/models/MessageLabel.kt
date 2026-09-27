package com.goodwy.smsmessenger.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(tableName = "message_labels", primaryKeys = ["message_id", "label_id"], indices = [Index("label_id")])
data class MessageLabel(
    @ColumnInfo(name = "message_id") val messageId: Long,
    @ColumnInfo(name = "label_id") val labelId: Long,
)
