package com.goodwy.smsmessenger.dialogs

import com.goodwy.commons.activities.BaseSimpleActivity
import com.goodwy.commons.helpers.ensureBackgroundThread
import com.goodwy.smsmessenger.R
import com.goodwy.smsmessenger.helpers.HomaDiagnostics
import com.goodwy.smsmessenger.helpers.MessageAnnotationStore
import com.goodwy.smsmessenger.models.Conversation

object ConversationAnnotationDialogs {
    fun editLabels(activity: BaseSimpleActivity, conversation: Conversation, onSaved: () -> Unit) {
        ensureBackgroundThread {
            val current = MessageAnnotationStore.getConversationLabels(activity, conversation.threadId).map { it.name }
            val suggestions = MessageAnnotationStore.getAllLabelNames(activity)
            activity.runOnUiThread {
                HomaTagNoteDialogs.editTags(
                    activity, current, suggestions,
                    activity.getString(R.string.annotation_tags_hint),
                    activity.getString(R.string.annotation_add_tag)
                ) { names ->
                    ensureBackgroundThread {
                        try {
                            HomaDiagnostics.timed("SAVE_CONVERSATION_TAGS") {
                                MessageAnnotationStore.setConversationLabels(activity, conversation.threadId, names)
                            }
                            activity.runOnUiThread(onSaved)
                        } catch (e: Exception) {
                            HomaDiagnostics.error("SAVE_CONVERSATION_TAGS_FAILED", e)
                        }
                    }
                }
            }
        }
    }

    fun editNote(activity: BaseSimpleActivity, conversation: Conversation, onSaved: () -> Unit) {
        ensureBackgroundThread {
            val current = MessageAnnotationStore.getConversationNote(activity, conversation.threadId)?.text.orEmpty()
            activity.runOnUiThread {
                HomaTagNoteDialogs.editNote(
                    activity, current,
                    activity.getString(R.string.annotation_note_hint),
                    activity.getString(R.string.annotation_edit_note),
                    onSaved = { text ->

                    ensureBackgroundThread {
                        try {
                            HomaDiagnostics.timed("SAVE_CONVERSATION_NOTE") {
                                MessageAnnotationStore.setConversationNote(activity, conversation.threadId, text)
                            }
                            activity.runOnUiThread(onSaved)
                        } catch (e: Exception) {
                            HomaDiagnostics.error("SAVE_CONVERSATION_NOTE_FAILED", e)
                        }
                    }
                },
                onDeleted = {
                    ensureBackgroundThread {
                        try {
                            MessageAnnotationStore.setConversationNote(activity, conversation.threadId, "")
                            activity.runOnUiThread(onSaved)
                        } catch (e: Exception) {
                            HomaDiagnostics.error("DELETE_CONVERSATION_NOTE_FAILED", e)
                        }
                    }
                }
            )
        }
    }
}
