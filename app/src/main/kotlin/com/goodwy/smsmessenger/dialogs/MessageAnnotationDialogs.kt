package com.goodwy.smsmessenger.dialogs

import com.goodwy.commons.activities.BaseSimpleActivity
import com.goodwy.commons.dialogs.TagNoteDialogs
import com.goodwy.smsmessenger.R
import com.goodwy.smsmessenger.helpers.MessageAnnotationStore
import com.goodwy.smsmessenger.models.Message

object MessageAnnotationDialogs {
    fun editLabels(activity: BaseSimpleActivity, message: Message, onSaved: () -> Unit) {
        TagNoteDialogs.editTags(
            activity = activity,
            currentTags = MessageAnnotationStore.getMessageLabels(activity, message.id).map { it.name },
            hint = activity.getString(R.string.annotation_tags_hint),
            help = activity.getString(R.string.annotation_tags_help),
            title = activity.getString(R.string.annotation_add_tag)
        ) { names ->
            MessageAnnotationStore.setMessageLabels(activity, message.id, names)
            onSaved()
        }
    }

    fun editNote(activity: BaseSimpleActivity, message: Message, onSaved: () -> Unit) {
        TagNoteDialogs.editNote(
            activity = activity,
            currentNote = MessageAnnotationStore.getMessageNote(activity, message.id)?.text.orEmpty(),
            hint = activity.getString(R.string.annotation_note_hint),
            title = activity.getString(R.string.annotation_add_note)
        ) { text ->
            MessageAnnotationStore.setMessageNote(activity, message.id, text)
            onSaved()
        }
    }
}
