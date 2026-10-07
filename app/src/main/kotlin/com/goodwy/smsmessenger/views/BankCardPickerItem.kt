package com.goodwy.smsmessenger.views

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.goodwy.commons.extensions.getProperPrimaryColor
import com.goodwy.commons.extensions.getProperTextColor
import com.goodwy.smsmessenger.R
import com.goodwy.smsmessenger.features.bankcards.BankCard
import com.goodwy.smsmessenger.features.bankcards.BankCardsRepository
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView

/**
 * Homa-native bank card composer action.
 *
 * It deliberately follows the compact attachment picker instead of reusing the
 * old Messages/Fossify BankCardPickerItem UI.
 */
class BankCardPickerItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val repo = BankCardsRepository(context)

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        isClickable = true
        isFocusable = true
        contentDescription = if (isPersian()) "کارت بانکی" else "Bank card"

        val primary = context.getProperPrimaryColor()
        val textColor = context.getProperTextColor()

        val icon = ImageView(context).apply {
            layoutParams = LayoutParams(dp(40), dp(30))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(9).toFloat()
                setColor(withAlpha(primary, 0.14f))
            }
            setImageDrawable(
                AppCompatResources.getDrawable(context, R.drawable.ic_homa_card)
            )
            imageTintList = android.content.res.ColorStateList.valueOf(primary)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(6), dp(4), dp(6), dp(4))
            contentDescription = contentDescription
        }
        addView(icon)

        // The compact picker normally hides labels. Keep the label available
        // for accessibility without changing the current visual density.
        val label = TextView(context).apply {
            text = if (isPersian()) "کارت بانکی" else "Bank card"
            setTextColor(text)
            textSize = 11f
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        addView(label, LayoutParams(LayoutParams.WRAP_CONTENT, dp(18)))

        setOnClickListener { showPicker() }
    }

    private fun showPicker() {
        val activity = context as? Activity ?: return
        val cards = runCatching { repo.getCards() }.getOrElse { emptyList() }

        val dialog = BottomSheetDialog(activity)
        val surface = resolveSurfaceColor(activity)
        val textColor = activity.getProperTextColor()
        val secondary = withAlpha(textColor, 0.65f)
        val primary = activity.getProperPrimaryColor()

        val sheet = LinearLayout(activity).apply {
            orientation = VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = GradientDrawable().apply {
                setColor(surface)
                cornerRadii = floatArrayOf(
                    dp(24f), dp(24f), dp(24f), dp(24f),
                    0f, 0f, 0f, 0f
                )
            }
            setPadding(dp(20), dp(10), dp(20), dp(20))
        }

        sheet.addView(View(activity).apply {
            background = GradientDrawable().apply {
                setColor(withAlpha(textColor, 0.18f))
                cornerRadius = dp(2f)
            }
        }, LinearLayout.LayoutParams(dp(36), dp(4)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(14)
        })

        sheet.addView(TextView(activity).apply {
            text = if (isPersian()) "انتخاب کارت بانکی" else "Select bank card"
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
            gravity = Gravity.START
            setPadding(0, 0, 0, dp(4))
        }, LinearLayout.LayoutParams(-1, -2))

        sheet.addView(TextView(activity).apply {
            text = if (isPersian()) {
                "کارت ذخیره‌شده را انتخاب کنید تا اطلاعات آن در متن پیام قرار بگیرد."
            } else {
                "Choose a saved card to insert its details into the message."
            }
            textSize = 12f
            setTextColor(secondary)
            setPadding(0, 0, 0, dp(14))
        }, LinearLayout.LayoutParams(-1, -2))

        if (cards.isEmpty()) {
            val empty = LinearLayout(activity).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(28), dp(12), dp(20))
            }
            empty.addView(ImageView(activity).apply {
                setImageResource(R.drawable.ic_homa_card)
                imageTintList = android.content.res.ColorStateList.valueOf(primary)
                setPadding(dp(8), dp(8), dp(8), dp(8))
            }, LinearLayout.LayoutParams(dp(56), dp(56)))
            empty.addView(TextView(activity).apply {
                text = if (isPersian()) "هنوز کارت بانکی ذخیره نشده" else "No saved bank cards"
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(textColor)
                gravity = Gravity.CENTER
                setPadding(0, dp(10), 0, dp(4))
            })
            empty.addView(TextView(activity).apply {
                text = if (isPersian()) {
                    "ابتدا کارت را از بخش «کارت‌های بانکی» اضافه کنید."
                } else {
                    "Add a card first from Bank Cards."
                }
                textSize = 12f
                setTextColor(secondary)
                gravity = Gravity.CENTER
            })
            sheet.addView(empty, LinearLayout.LayoutParams(-1, -2))
        } else {
            cards.forEach { card ->
                sheet.addView(cardRow(activity, card, textColor, secondary, primary, dialog))
            }
        }

        dialog.setContentView(sheet)
        dialog.show()
    }

    private fun cardRow(
        activity: Activity,
        card: BankCard,
        textColor: Int,
        secondary: Int,
        primary: Int,
        dialog: BottomSheetDialog
    ): View {
        val row = MaterialCardView(activity).apply {
            radius = dp(16).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(withAlpha(primary, 0.06f))
            strokeWidth = dp(1)
            strokeColor = withAlpha(card.visual?.color ?: primary, 0.18f)
            isClickable = true
            isFocusable = true
        }

        val body = LinearLayout(activity).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        val logo = ImageView(activity).apply {
            val resourceId = card.visual?.logoResourceName
                ?.let { activity.resources.getIdentifier(it, "drawable", activity.packageName) } ?: 0
            setImageDrawable(
                resourceId.takeIf { it != 0 }?.let {
                    AppCompatResources.getDrawable(activity, it)
                } ?: AppCompatResources.getDrawable(activity, R.drawable.ic_homa_card)
            )
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(5), dp(5), dp(5), dp(5))
        }
        body.addView(logo, LinearLayout.LayoutParams(dp(46), dp(46)))

        val info = LinearLayout(activity).apply {
            orientation = VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(12), 0, 0, 0)
        }
        info.addView(TextView(activity).apply {
            text = card.visual?.persianName ?: card.bankId
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
        })
        info.addView(TextView(activity).apply {
            text = repo.formatCard(card.cardNumber)
            textSize = 13f
            typeface = Typeface.MONOSPACE
            textDirection = View.TEXT_DIRECTION_LTR
            setTextColor(secondary)
        })
        if (card.holderName.isNotBlank()) {
            info.addView(TextView(activity).apply {
                text = card.holderName
                textSize = 11f
                setTextColor(secondary)
            })
        }
        body.addView(info, LinearLayout.LayoutParams(0, -2, 1f))

        val copyIcon = ImageView(activity).apply {
            setImageResource(R.drawable.ic_homa_copy)
            imageTintList = android.content.res.ColorStateList.valueOf(primary)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            contentDescription = if (isPersian()) "کپی شماره کارت" else "Copy card number"
        }
        body.addView(copyIcon, LinearLayout.LayoutParams(dp(44), dp(44)))

        row.addView(body, FrameLayout.LayoutParams(-1, -2))
        row.setOnClickListener {
            insertCard(activity, card)
            dialog.dismiss()
        }
        copyIcon.setOnClickListener {
            val clipboard = ContextCompat.getSystemService(activity, android.content.ClipboardManager::class.java)
            clipboard?.setPrimaryClip(
                android.content.ClipData.newPlainText("Homa", repo.normalizeCard(card.cardNumber))
            )
        }

        return row.apply {
            layoutParams = LayoutParams(-1, dp(76)).apply {
                bottomMargin = dp(8)
            }
        }
    }

    private fun insertCard(activity: Activity, card: BankCard) {
        val message = (activity.currentFocus as? EditText) ?: return
        val bankName = card.visual?.persianName ?: card.bankId
        val cardNumber = repo.formatCard(card.cardNumber)

        val selectedText = if (isPersian()) {
            buildString {
                append("بانک: ").append(bankName)
                append("\nشماره کارت:\n").append(cardNumber)
                if (card.holderName.isNotBlank()) {
                    append("\nصاحب کارت: ").append(card.holderName)
                }
                if (card.iban.isNotBlank()) {
                    append("\nشماره شبا: ").append(repo.formatIban(card.iban))
                }
            }
        } else {
            buildString {
                append("Bank: ").append(card.visual?.englishName ?: card.bankId)
                append("\nCard number:\n").append(cardNumber)
                if (card.holderName.isNotBlank()) {
                    append("\nCard holder: ").append(card.holderName)
                }
                if (card.iban.isNotBlank()) {
                    append("\nIBAN: ").append(repo.formatIban(card.iban))
                }
            }
        }

        val existing = message.text?.toString().orEmpty()
        val newText = if (existing.isBlank()) selectedText else "$existing\n$selectedText"
        message.setText(newText)
        message.setSelection(newText.length)
        message.requestFocus()
    }

    private fun resolveSurfaceColor(context: Context): Int {
        val value = android.util.TypedValue()
        context.theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, value, true)
        return if (value.resourceId != 0) ContextCompat.getColor(context, value.resourceId) else value.data
    }

    private fun isPersian(): Boolean =
        resources.configuration.locales.firstOrNull()?.language?.equals("fa", true) == true

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density

    private fun withAlpha(color: Int, alpha: Float): Int =
        Color.argb(
            (255f * alpha).toInt().coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
}
