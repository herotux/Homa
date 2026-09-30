package com.goodwy.smsmessenger.dialogs

import com.goodwy.commons.activities.BaseSimpleActivity
import com.goodwy.commons.dialogs.TagNoteDialogs
import com.goodwy.smsmessenger.R
import com.goodwy.smsmessenger.helpers.MessageAnnotationStore
import com.goodwy.smsmessenger.models.Conversation

object ConversationAnnotationDialogs {
    fun editLabels(activity: BaseSimpleActivity, conversation: Conversation, onSaved: () -> Unit) {
        TagNoteDialogs.editTags(
            activity = activity,
            currentTags = MessageAnnotationStore.getConversationLabels(activity, conversation.threadId).map { it.name },
            hint = activity.getString(R.string.annotation_tags_hint),
            help = activity.getString(R.string.annotation_tags_help),
            title = activity.getString(R.string.annotation_add_tag)
        ) { names ->
            MessageAnnotationStore.setConversationLabels(activity, conversation.threadId, names)
            onSaved()
        }
    }

    fun editNote(activity: BaseSimpleActivity, conversation: Conversation, onSaved: () -> Unit) {
        TagNoteDialogs.editNote(
            activity = activity,
            currentNote = MessageAnnotationStore.getConversationNote(activity, conversation.threadId)?.text.orEmpty(),
            hint = activity.getString(R.string.annotation_note_hint),
            title = activity.getString(R.string.annotation_add_note)
        ) { text ->
            MessageAnnotationStore.setConversationNote(activity, conversation.threadId, text)
            onSaved()
        }
    }
}
