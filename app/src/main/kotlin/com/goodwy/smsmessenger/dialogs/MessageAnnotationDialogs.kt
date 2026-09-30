package com.goodwy.smsmessenger.dialogs

import com.goodwy.commons.activities.BaseSimpleActivity
import com.goodwy.commons.dialogs.TagNoteDialogs
import com.goodwy.commons.helpers.ensureBackgroundThread
import com.goodwy.smsmessenger.R
import com.goodwy.smsmessenger.helpers.MessageAnnotationStore
import com.goodwy.smsmessenger.models.Message

object MessageAnnotationDialogs {
    fun editLabels(activity: BaseSimpleActivity, message: Message, onSaved: () -> Unit) {
        TagNoteDialogs.editTags(
            activity,
            MessageAnnotationStore.getMessageLabels(activity, message.id).map { it.name },
            activity.getString(R.string.annotation_tags_hint),
            activity.getString(R.string.annotation_tags_help),
            activity.getString(R.string.annotation_add_tag)
        ) { names ->
            ensureBackgroundThread {
                MessageAnnotationStore.setMessageLabels(activity, message.id, names)
                activity.runOnUiThread(onSaved)
            }
        }
    }

    fun editNote(activity: BaseSimpleActivity, message: Message, onSaved: () -> Unit) {
        TagNoteDialogs.editNote(
            activity,
            MessageAnnotationStore.getMessageNote(activity, message.id)?.text.orEmpty(),
            activity.getString(R.string.annotation_note_hint),
            activity.getString(R.string.annotation_add_note)
        ) { text ->
            ensureBackgroundThread {
                MessageAnnotationStore.setMessageNote(activity, message.id, text)
                activity.runOnUiThread(onSaved)
            }
        }
    }
}
