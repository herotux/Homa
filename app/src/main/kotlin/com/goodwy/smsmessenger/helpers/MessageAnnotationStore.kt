package com.goodwy.smsmessenger.helpers

import android.content.Context
import com.goodwy.commons.extensions.getProperPrimaryColor
import com.goodwy.commons.models.Note
import com.goodwy.commons.models.Tag
import com.goodwy.smsmessenger.extensions.getMessagesDB
import com.goodwy.smsmessenger.models.AnnotationLabel
import com.goodwy.smsmessenger.models.ConversationLabel
import com.goodwy.smsmessenger.models.ConversationNote
import com.goodwy.smsmessenger.models.MessageLabel
import com.goodwy.smsmessenger.models.MessageNote

object MessageAnnotationStore {
    private fun dao(context: Context) = context.getMessagesDB().AnnotationLabelsDao()

    fun getAllLabelNames(context: Context): List<String> =
        dao(context).getLabels().map { it.name }

    fun getMessageLabels(context: Context, messageId: Long): List<Tag> =
        dao(context).getMessageLabels(messageId).map(::toTag)

    fun getMessageNote(context: Context, messageId: Long): Note? =
        dao(context).getMessageNote(messageId)?.let(::toNote)

    fun getConversationLabels(context: Context, threadId: Long): List<Tag> =
        dao(context).getConversationLabels(threadId).map(::toTag)

    fun getConversationNote(context: Context, threadId: Long): Note? =
        dao(context).getConversationNote(threadId)?.let(::toNote)

    fun ensureLabel(context: Context, name: String, color: Int): AnnotationLabel {
        val normalized = name.trim().removePrefix("#")
        require(normalized.isNotEmpty())
        dao(context).getLabelByName(normalized)?.let { return it }
        val now = System.currentTimeMillis()
        val id = dao(context).insertLabel(
            AnnotationLabel(name = normalized, color = color, createdAt = now, updatedAt = now)
        )
        return dao(context).getLabel(id) ?: dao(context).getLabelByName(normalized)!!
    }

    fun setMessageLabels(context: Context, messageId: Long, names: List<String>) {
        val db = dao(context)
        db.removeAllMessageLabels(messageId)
        names.map { it.trim().removePrefix("#") }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .forEach {
                val label = ensureLabel(context, it, context.getProperPrimaryColor())
                db.addMessageLabel(MessageLabel(messageId, label.id))
            }
    }

    fun setMessageNote(context: Context, messageId: Long, text: String) {
        val value = text.trim()
        if (value.isEmpty()) {
            dao(context).deleteMessageNote(messageId)
        } else {
            val now = System.currentTimeMillis()
            val old = dao(context).getMessageNote(messageId)
            dao(context).upsertMessageNote(
                MessageNote(messageId, value, old?.createdAt ?: now, now)
            )
        }
    }

    fun setConversationLabels(context: Context, threadId: Long, names: List<String>) {
        val db = dao(context)
        db.removeAllConversationLabels(threadId)
        names.map { it.trim().removePrefix("#") }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .forEach {
                val label = ensureLabel(context, it, context.getProperPrimaryColor())
                db.addConversationLabel(ConversationLabel(threadId, label.id))
            }
    }

    fun setConversationNote(context: Context, threadId: Long, text: String) {
        val value = text.trim()
        if (value.isEmpty()) {
            dao(context).deleteConversationNote(threadId)
        } else {
            val now = System.currentTimeMillis()
            val old = dao(context).getConversationNote(threadId)
            dao(context).upsertConversationNote(
                ConversationNote(threadId, value, old?.createdAt ?: now, now)
            )
        }
    }

    private fun toTag(label: AnnotationLabel) = Tag(
        id = label.id,
        name = label.name,
        color = label.color,
        createdAt = label.createdAt,
        updatedAt = label.updatedAt
    )

    private fun toNote(note: MessageNote) = Note(
        text = note.text,
        createdAt = note.createdAt,
        updatedAt = note.updatedAt
    )

    private fun toNote(note: ConversationNote) = Note(
        text = note.text,
        createdAt = note.createdAt,
        updatedAt = note.updatedAt
    )
}
