package com.goodwy.smsmessenger.helpers

import android.content.Context
import com.goodwy.commons.extensions.getProperPrimaryColor
import com.goodwy.smsmessenger.extensions.messagesDB
import com.goodwy.smsmessenger.interfaces.AnnotationLabelsDao
import com.goodwy.smsmessenger.models.AnnotationLabel
import com.goodwy.smsmessenger.models.ConversationLabel
import com.goodwy.smsmessenger.models.ConversationNote
import com.goodwy.smsmessenger.models.MessageLabel
import com.goodwy.smsmessenger.models.MessageNote

object MessageAnnotationStore {
    private fun dao(context: Context): AnnotationLabelsDao = context.messagesDB.AnnotationLabelsDao()
    fun getMessageLabels(context: Context, messageId: Long) = dao(context).getMessageLabels(messageId)
    fun getMessageNote(context: Context, messageId: Long) = dao(context).getMessageNote(messageId)
    fun getConversationLabels(context: Context, threadId: Long) = dao(context).getConversationLabels(threadId)
    fun getConversationNote(context: Context, threadId: Long) = dao(context).getConversationNote(threadId)

    fun ensureLabel(context: Context, name: String, color: Int): AnnotationLabel {
        val normalized = name.trim().removePrefix("#")
        require(normalized.isNotEmpty())
        dao(context).getLabelByName(normalized)?.let { return it }
        val now = System.currentTimeMillis()
        val id = dao(context).insertLabel(AnnotationLabel(name = normalized, color = color, createdAt = now, updatedAt = now))
        return dao(context).getLabel(id) ?: dao(context).getLabelByName(normalized)!!
    }

    fun setMessageLabels(context: Context, messageId: Long, names: List<String>) {
        val db = dao(context)
        db.removeAllMessageLabels(messageId)
        names.map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }.forEach {
            val label = ensureLabel(context, it, context.getProperPrimaryColor())
            db.addMessageLabel(MessageLabel(messageId, label.id))
        }
    }

    fun setMessageNote(context: Context, messageId: Long, text: String) {
        val value = text.trim()
        if (value.isEmpty()) dao(context).deleteMessageNote(messageId) else {
            val now = System.currentTimeMillis()
            val old = dao(context).getMessageNote(messageId)
            dao(context).upsertMessageNote(MessageNote(messageId, value, old?.createdAt ?: now, now))
        }
    }

    fun setConversationLabels(context: Context, threadId: Long, names: List<String>) {
        val db = dao(context)
        db.removeAllConversationLabels(threadId)
        names.map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }.forEach {
            val label = ensureLabel(context, it, context.getProperPrimaryColor())
            db.addConversationLabel(ConversationLabel(threadId, label.id))
        }
    }

    fun setConversationNote(context: Context, threadId: Long, text: String) {
        val value = text.trim()
        if (value.isEmpty()) dao(context).deleteConversationNote(threadId) else {
            val now = System.currentTimeMillis()
            val old = dao(context).getConversationNote(threadId)
            dao(context).upsertConversationNote(ConversationNote(threadId, value, old?.createdAt ?: now, now))
        }
    }
}
