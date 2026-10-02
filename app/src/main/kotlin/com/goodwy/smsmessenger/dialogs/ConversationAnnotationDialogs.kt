package com.goodwy.smsmessenger.dialogs

import com.goodwy.commons.activities.BaseSimpleActivity
import com.goodwy.commons.dialogs.TagNoteDialogs
import com.goodwy.commons.helpers.ensureBackgroundThread
import com.goodwy.smsmessenger.R
import com.goodwy.smsmessenger.helpers.HomaDiagnostics
import com.goodwy.smsmessenger.helpers.MessageAnnotationStore
import com.goodwy.smsmessenger.models.Conversation

object ConversationAnnotationDialogs {
    fun editLabels(activity: BaseSimpleActivity, conversation: Conversation, onSaved: () -> Unit) {
        ensureBackgroundThread {
            val current = MessageAnnotationStore.getConversationLabels(activity, conversation.threadId).map { it.name }
            activity.runOnUiThread {
                TagNoteDialogs.editTags(
                    activity, current,
                    activity.getString(R.string.annotation_tags_hint),
                    activity.getString(R.string.annotation_tags_help),
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
                TagNoteDialogs.editNote(
                    activity, current,
                    activity.getString(R.string.annotation_note_hint),
                    activity.getString(R.string.annotation_add_note)
                ) { text ->
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
                }
            }
        }
    }
}
