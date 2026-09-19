package com.goodwy.smsmessenger.dialogs
import android.app.AlertDialog
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.goodwy.commons.activities.BaseSimpleActivity
import com.goodwy.commons.helpers.ensureBackgroundThread
import com.goodwy.smsmessenger.R
import com.goodwy.smsmessenger.helpers.MessageAnnotationStore
import com.goodwy.smsmessenger.models.Message
object MessageAnnotationDialogs {
 fun editLabels(activity: BaseSimpleActivity, message: Message, onSaved: () -> Unit) {
  val input=EditText(activity).apply{hint=activity.getString(R.string.annotation_labels_hint);inputType=InputType.TYPE_CLASS_TEXT;setSingleLine(false)}
  val container=LinearLayout(activity).apply{orientation=LinearLayout.VERTICAL;val p=(20*resources.displayMetrics.density).toInt();setPadding(p,0,p,0);addView(input,LinearLayout.LayoutParams(-1,-2));addView(TextView(activity).apply{text=activity.getString(R.string.annotation_labels_help);textSize=12f;alpha=.7f;setPadding(0,p/2,0,0)})}
  ensureBackgroundThread{val labels=MessageAnnotationStore.getMessageLabels(activity,message.id);activity.runOnUiThread{input.setText(labels.joinToString(", "){it.name});AlertDialog.Builder(activity).setTitle(R.string.annotation_add_label).setView(container).setNegativeButton(android.R.string.cancel,null).setPositiveButton(android.R.string.ok){_,_->val names=input.text.toString().split(',', '\n','،');ensureBackgroundThread{MessageAnnotationStore.setMessageLabels(activity,message.id,names);activity.runOnUiThread(onSaved)}}.show()}}
 }
 fun editNote(activity: BaseSimpleActivity,message: Message,onSaved:()->Unit){
  val input=EditText(activity).apply{hint=activity.getString(R.string.annotation_note_hint);inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;minLines=4;maxLines=8}
  val container=LinearLayout(activity).apply{orientation=LinearLayout.VERTICAL;val p=(20*resources.displayMetrics.density).toInt();setPadding(p,0,p,0);addView(input,LinearLayout.LayoutParams(-1,-2))}
  ensureBackgroundThread{input.setText(MessageAnnotationStore.getMessageNote(activity,message.id)?.text.orEmpty());activity.runOnUiThread{input.setSelection(input.text.length);AlertDialog.Builder(activity).setTitle(R.string.annotation_add_note).setView(container).setNegativeButton(android.R.string.cancel,null).setPositiveButton(android.R.string.save,null).create().apply{setOnShowListener{getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{ensureBackgroundThread{MessageAnnotationStore.setMessageNote(activity,message.id,input.text.toString());activity.runOnUiThread{dismiss();onSaved()}}}}}.show()}}
 }
}