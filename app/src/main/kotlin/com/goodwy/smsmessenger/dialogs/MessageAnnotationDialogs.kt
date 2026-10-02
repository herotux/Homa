package com.goodwy.smsmessenger.dialogs

import com.goodwy.commons.activities.BaseSimpleActivity
import com.goodwy.commons.dialogs.TagNoteDialogs
import com.goodwy.commons.helpers.ensureBackgroundThread
import com.goodwy.smsmessenger.R
import com.goodwy.smsmessenger.helpers.HomaDiagnostics
import com.goodwy.smsmessenger.helpers.MessageAnnotationStore
import com.goodwy.smsmessenger.models.Message

object MessageAnnotationDialogs {
    fun editLabels(activity: BaseSimpleActivity, message: Message, onSaved: () -> Unit) {
        ensureBackgroundThread {
            val current = MessageAnnotationStore.getMessageLabels(activity, message.id).map { it.name }
            activity.runOnUiThread {
                TagNoteDialogs.editTags(
                    activity, current,
                    activity.getString(R.string.annotation_tags_hint),
                    activity.getString(R.string.annotation_tags_help),
                    activity.getString(R.string.annotation_add_tag)
                ) { names ->
                    ensureBackgroundThread {
                        try {
                            HomaDiagnostics.timed("SAVE_MESSAGE_TAGS") {
                                MessageAnnotationStore.setMessageLabels(activity, message.id, names)
                            }
                            activity.runOnUiThread(onSaved)
                        } catch (e: Exception) {
                            HomaDiagnostics.error("SAVE_MESSAGE_TAGS_FAILED", e)
                        }
                    }
                }
            }
        }
    }

    fun editNote(activity: BaseSimpleActivity, message: Message, onSaved: () -> Unit) {
        ensureBackgroundThread {
            val current = MessageAnnotationStore.getMessageNote(activity, message.id)?.text.orEmpty()
            activity.runOnUiThread {
                TagNoteDialogs.editNote(
                    activity, current,
                    activity.getString(R.string.annotation_note_hint),
                    activity.getString(R.string.annotation_add_note)
                ) { text ->
                    ensureBackgroundThread {
                        try {
                            HomaDiagnostics.timed("SAVE_MESSAGE_NOTE") {
                                MessageAnnotationStore.setMessageNote(activity, message.id, text)
                            }
                            activity.runOnUiThread(onSaved)
                        } catch (e: Exception) {
                            HomaDiagnostics.error("SAVE_MESSAGE_NOTE_FAILED", e)
                        }
                    }
                }
            }
        }
    }
}
