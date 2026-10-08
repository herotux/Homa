package com.goodwy.smsmessenger.dialogs

import android.content.res.ColorStateList
import android.text.InputType
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.goodwy.commons.activities.BaseSimpleActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.goodwy.smsmessenger.R

object HomaTagNoteDialogs {

    fun editTags(
        activity: BaseSimpleActivity,
        currentTags: List<String>,
        suggestions: List<String>,
        hint: String,
        title: String,
        onSaved: (List<String>) -> Unit
    ) {
        val density = activity.resources.displayMetrics.density
        val padding = (20 * density).toInt()
        val vertical = (10 * density).toInt()

        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, 0, padding, padding / 2)
        }

        val chipGroup = ChipGroup(activity).apply {
            isSingleLine = false
            chipSpacingHorizontal = (6 * density).toInt()
            chipSpacingVertical = (6 * density).toInt()
            layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
        }
        container.addView(chipGroup, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = vertical
        })

        val input = AutoCompleteTextView(activity).apply {
            this.hint = hint
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_DONE
            threshold = 1
            setPadding((12 * density).toInt(), 0, (12 * density).toInt(), 0)
        }
        container.addView(input, LinearLayout.LayoutParams(-1, (52 * density).toInt()))

        val normalizedSuggestions = suggestions
            .map { it.trim().removePrefix("#") }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }

        val adapter = ArrayAdapter(activity, android.R.layout.simple_dropdown_item_1line, normalizedSuggestions)
        input.setAdapter(adapter)

        fun currentNames(): List<String> =
            (0 until chipGroup.childCount)
                .mapNotNull { (chipGroup.getChildAt(it) as? Chip)?.tag as? String }

        fun addChip(raw: String) {
            val name = raw.trim().removePrefix("#").trim()
            if (name.isEmpty() || currentNames().any { it.equals(name, ignoreCase = true) }) return

            val chip = Chip(activity).apply {
                text = name
                tag = name
                isCloseIconVisible = true
                setEnsureMinTouchTargetSize(false)
                setOnCloseIconClickListener { chipGroup.removeView(this) }
                chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(activity, R.color.color_primary)
                )
                setTextColor(ContextCompat.getColor(activity, android.R.color.white))
            }
            chipGroup.addView(chip)
        }

        currentTags.forEach(::addChip)

        input.setOnItemClickListener { _, _, position, _ ->
            val value = adapter.getItem(position).orEmpty()
            addChip(value)
            input.text?.clear()
            input.dismissDropDown()
        }

        fun commitTypedTag() {
            val value = input.text?.toString().orEmpty()
            value.split(',', '،', '\n')
                .map(String::trim)
                .filter(String::isNotEmpty)
                .forEach(::addChip)
            input.text?.clear()
            input.dismissDropDown()
        }

        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                commitTypedTag()
                true
            } else false
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle(title)
            .setView(container)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                onSaved(currentNames())
            }
            .show()
    }

    fun editNote(
        activity: BaseSimpleActivity,
        currentNote: String,
        hint: String,
        title: String,
        onSaved: (String) -> Unit,
        onDeleted: (() -> Unit)? = null
    ) {
        val density = activity.resources.displayMetrics.density
        val padding = (20 * density).toInt()

        val input = EditText(activity).apply {
            this.hint = hint
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = 4
            maxLines = 8
            gravity = Gravity.TOP or Gravity.START
            setText(currentNote)
            setSelection(text.length)
        }

        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, 0, padding, padding / 2)
            addView(input, LinearLayout.LayoutParams(-1, -2))
        }

        val builder = MaterialAlertDialogBuilder(activity)
            .setTitle(title)
            .setView(container)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                onSaved(input.text?.toString().orEmpty())
            }

        if (currentNote.isNotBlank() && onDeleted != null) {
            builder.setNeutralButton(R.string.annotation_delete_note) { _, _ ->
                onDeleted()
            }
        }

        builder.show()
    }
}
