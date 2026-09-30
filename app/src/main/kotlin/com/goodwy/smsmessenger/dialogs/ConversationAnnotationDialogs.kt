package com.goodwy.smsmessenger.dialogs

import com.goodwy.commons.activities.BaseSimpleActivity
import com.goodwy.commons.dialogs.TagNoteDialogs
import com.goodwy.commons.helpers.ensureBackgroundThread
import com.goodwy.smsmessenger.R
import com.goodwy.smsmessenger.helpers.MessageAnnotationStore
import com.goodwy.smsmessenger.models.Conversation

object ConversationAnnotationDialogs {
    fun editLabels(activity: BaseSimpleActivity, conversation: Conversation, onSaved: () -> Unit) {
        TagNoteDialogs.editTags(
            activity,
            MessageAnnotationStore.getConversationLabels(activity, conversation.threadId).map { it.name },
            activity.getString(R.string.annotation_tags_hint),
            activity.getString(R.string.annotation_tags_help),
            activity.getString(R.string.annotation_add_tag)
        ) { names ->
            ensureBackgroundThread {
                MessageAnnotationStore.setConversationLabels(activity, conversation.threadId, names)
                activity.runOnUiThread(onSaved)
            }
        }
    }

    fun editNote(activity: BaseSimpleActivity, conversation: Conversation, onSaved: () -> Unit) {
        TagNoteDialogs.editNote(
            activity,
            MessageAnnotationStore.getConversationNote(activity, conversation.threadId)?.text.orEmpty(),
            activity.getString(R.string.annotation_note_hint),
            activity.getString(R.string.annotation_add_note)
        ) { text ->
            ensureBackgroundThread {
                MessageAnnotationStore.setConversationNote(activity, conversation.threadId, text)
                activity.runOnUiThread(onSaved)
            }
        }
    }
}
