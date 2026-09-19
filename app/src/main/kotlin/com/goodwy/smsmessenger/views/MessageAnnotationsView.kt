package com.goodwy.smsmessenger.views
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import com.goodwy.commons.extensions.getProperTextColor
import com.goodwy.commons.views.MyTextView
import com.goodwy.smsmessenger.activities.SimpleActivity
import com.goodwy.smsmessenger.models.AnnotationLabel
import com.goodwy.smsmessenger.models.MessageNote
import kotlin.math.roundToInt
class MessageAnnotationsView(private val activity:SimpleActivity):LinearLayout(activity){
 private val labelsRow=LinearLayout(activity)
 private val noteView=MyTextView(activity)
 init{orientation=VERTICAL;gravity=Gravity.START;visibility=GONE;setPadding(dp(2),dp(2),dp(2),dp(2));labelsRow.orientation=HORIZONTAL;labelsRow.gravity=Gravity.START or Gravity.CENTER_VERTICAL;addView(labelsRow,LayoutParams(-1,-2));noteView.apply{textSize=11f;setTextColor(activity.getProperTextColor());alpha=.78f;maxLines=2;ellipsize=android.text.TextUtils.TruncateAt.END;setPadding(dp(4),dp(2),dp(4),dp(2))};addView(noteView,LayoutParams(-1,-2))}
 fun render(labels:List<AnnotationLabel>,note:MessageNote?){labelsRow.removeAllViews();labels.forEach{label->labelsRow.addView(TextView(activity).apply{text="#"+label.name;textSize=10f;maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END;gravity=Gravity.CENTER;setTextColor(label.color);setPadding(dp(7),dp(2),dp(7),dp(2));background=GradientDrawable().apply{cornerRadius=dp(10).toFloat();setColor(withAlpha(label.color,.12f));setStroke(dp(1),withAlpha(label.color,.28f))};layoutParams=LayoutParams(-2,dp(22)).apply{marginEnd=dp(5)}})};val t=note?.text?.trim().orEmpty();noteView.text=if(t.isEmpty())"" else "Note  ·  "+t;noteView.visibility=if(t.isEmpty())GONE else VISIBLE;visibility=if(labels.isEmpty()&&t.isEmpty())GONE else VISIBLE}
 private fun withAlpha(c:Int,a:Float):Int{val x=(android.graphics.Color.alpha(c)*a).roundToInt().coerceIn(18,255);return android.graphics.Color.argb(x,android.graphics.Color.red(c),android.graphics.Color.green(c),android.graphics.Color.blue(c))}
 private fun dp(v:Int)=(v*resources.displayMetrics.density).roundToInt()
}