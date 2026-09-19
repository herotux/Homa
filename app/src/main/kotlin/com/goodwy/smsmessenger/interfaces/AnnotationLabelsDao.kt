package com.goodwy.smsmessenger.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.goodwy.smsmessenger.models.AnnotationLabel
import com.goodwy.smsmessenger.models.Conversation
import com.goodwy.smsmessenger.models.ConversationLabel
import com.goodwy.smsmessenger.models.ConversationNote
import com.goodwy.smsmessenger.models.Message
import com.goodwy.smsmessenger.models.MessageLabel
import com.goodwy.smsmessenger.models.MessageNote

@Dao
interface AnnotationLabelsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) fun insertLabel(label: AnnotationLabel): Long
    @Query("SELECT * FROM annotation_labels ORDER BY name COLLATE NOCASE") fun getLabels(): List<AnnotationLabel>
    @Query("SELECT * FROM annotation_labels WHERE name = :name COLLATE NOCASE LIMIT 1") fun getLabelByName(name: String): AnnotationLabel?
    @Query("SELECT * FROM annotation_labels WHERE id = :id LIMIT 1") fun getLabel(id: Long): AnnotationLabel?
    @Query("UPDATE annotation_labels SET name = :name, color = :color, updated_at = :updatedAt WHERE id = :id") fun updateLabel(id: Long, name: String, color: Int, updatedAt: Long): Int
    @Query("DELETE FROM annotation_labels WHERE id = :id") fun deleteLabel(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE) fun addMessageLabel(relation: MessageLabel)
    @Query("DELETE FROM message_labels WHERE message_id = :messageId AND label_id = :labelId") fun removeMessageLabel(messageId: Long, labelId: Long)
    @Query("DELETE FROM message_labels WHERE message_id = :messageId") fun removeAllMessageLabels(messageId: Long)
    @Query("SELECT l.* FROM annotation_labels l INNER JOIN message_labels ml ON l.id = ml.label_id WHERE ml.message_id = :messageId ORDER BY l.name COLLATE NOCASE") fun getMessageLabels(messageId: Long): List<AnnotationLabel>
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun upsertMessageNote(note: MessageNote)
    @Query("SELECT * FROM message_notes WHERE message_id = :messageId LIMIT 1") fun getMessageNote(messageId: Long): MessageNote?
    @Query("DELETE FROM message_notes WHERE message_id = :messageId") fun deleteMessageNote(messageId: Long)
    @Query("SELECT DISTINCT m.* FROM messages m INNER JOIN message_labels ml ON m.id = ml.message_id INNER JOIN annotation_labels l ON l.id = ml.label_id WHERE l.name LIKE :name COLLATE NOCASE ORDER BY m.date DESC") fun getMessagesWithLabel(name: String): List<Message>
    @Query("SELECT DISTINCT m.* FROM messages m INNER JOIN message_notes n ON m.id = n.message_id WHERE n.text LIKE :text ORDER BY m.date DESC") fun getMessagesWithNote(text: String): List<Message>

    @Insert(onConflict = OnConflictStrategy.IGNORE) fun addConversationLabel(relation: ConversationLabel)
    @Query("DELETE FROM conversation_labels WHERE thread_id = :threadId AND label_id = :labelId") fun removeConversationLabel(threadId: Long, labelId: Long)
    @Query("DELETE FROM conversation_labels WHERE thread_id = :threadId") fun removeAllConversationLabels(threadId: Long)
    @Query("SELECT l.* FROM annotation_labels l INNER JOIN conversation_labels cl ON l.id = cl.label_id WHERE cl.thread_id = :threadId ORDER BY l.name COLLATE NOCASE") fun getConversationLabels(threadId: Long): List<AnnotationLabel>
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun upsertConversationNote(note: ConversationNote)
    @Query("SELECT * FROM conversation_notes WHERE thread_id = :threadId LIMIT 1") fun getConversationNote(threadId: Long): ConversationNote?
    @Query("DELETE FROM conversation_notes WHERE thread_id = :threadId") fun deleteConversationNote(threadId: Long)
    @Query("SELECT DISTINCT c.* FROM conversations c INNER JOIN conversation_labels cl ON c.thread_id = cl.thread_id INNER JOIN annotation_labels l ON l.id = cl.label_id WHERE l.name LIKE :name COLLATE NOCASE ORDER BY c.date DESC") fun getConversationsWithLabel(name: String): List<Conversation>
    @Query("SELECT DISTINCT c.* FROM conversations c INNER JOIN conversation_notes n ON c.thread_id = n.thread_id WHERE n.text LIKE :text ORDER BY c.date DESC") fun getConversationsWithNote(text: String): List<Conversation>
}
