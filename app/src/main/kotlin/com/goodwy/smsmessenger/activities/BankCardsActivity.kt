package com.goodwy.smsmessenger.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import com.goodwy.smsmessenger.features.bankcards.BankCard
import com.goodwy.smsmessenger.features.bankcards.BankCardsRepository
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Homa Bank Cards — a clean, native screen using the same visual language as Settings.
 */
class BankCardsActivity : AppCompatActivity() {
    private lateinit var repo: BankCardsRepository
    private lateinit var root: FrameLayout
    private lateinit var content: LinearLayout
    private lateinit var cardsContainer: LinearLayout
    private lateinit var emptyState: LinearLayout
    private lateinit var searchInput: TextInputEditText
    private lateinit var searchHolder: View
    private lateinit var countText: TextView
    private var cards = mutableListOf<BankCard>()

    private val backgroundColor get() = themeColor(com.google.android.material.R.attr.colorSurface)
    private val surfaceColor get() = themeColor(com.google.android.material.R.attr.colorSurface)
    private val primaryColor get() = themeColor(androidx.appcompat.R.attr.colorPrimary)
    private val textColor get() = themeColor(android.R.attr.textColorPrimary)
    private val secondaryTextColor get() = themeColor(android.R.attr.textColorSecondary)

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        repo = BankCardsRepository(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideNavigationBar()
        buildPage()
        load()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideNavigationBar()
    }

    private fun hideNavigationBar() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    private fun buildPage() {
        root = FrameLayout(this).apply {
            setBackgroundColor(backgroundColor)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val scroll = ScrollView(this).apply {
            clipToPadding = false
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16), 0, dp(16), dp(28))
        }

        content.addView(buildHeader(), LinearLayout.LayoutParams(-1, dp(76)))
        searchHolder = buildSearch()
        content.addView(searchHolder, LinearLayout.LayoutParams(-1, dp(68)).apply { topMargin = dp(2) })
        content.addView(buildOverview(), LinearLayout.LayoutParams(-1, dp(68)).apply {
            topMargin = dp(8)
            bottomMargin = dp(12)
        })

        cardsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        content.addView(cardsContainer, LinearLayout.LayoutParams(-1, -2))

        emptyState = buildEmptyState()
        content.addView(emptyState)

        scroll.addView(content, ViewGroup.LayoutParams(-1, -2))
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            content.updatePadding(
                left = dp(16),
                top = bars.top + dp(2),
                right = dp(16),
                bottom = dp(28) + bars.bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun buildHeader(): View = FrameLayout(this).apply {
        layoutDirection = View.LAYOUT_DIRECTION_RTL

        addView(iconButton(R.drawable.ic_homa_back, "بازگشت", textColor).apply {
            rotation = 180f
            setOnClickListener { finish() }
        }, FrameLayout.LayoutParams(dp(48), dp(56), Gravity.END or Gravity.BOTTOM))

        addView(TextView(this@BankCardsActivity).apply {
            text = "کارت‌های بانکی"
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(textColor)
        }, FrameLayout.LayoutParams(-2, dp(56), Gravity.CENTER or Gravity.BOTTOM))

        val actions = LinearLayout(this@BankCardsActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        actions.addView(iconButton(R.drawable.ic_homa_search, "جستجوی کارت", textColor).apply {
            setOnClickListener {
                searchHolder.visibility = if (searchHolder.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                if (searchHolder.visibility == View.VISIBLE) searchInput.requestFocus() else searchInput.setText("")
            }
        }, LinearLayout.LayoutParams(dp(48), dp(56)))
        actions.addView(iconButton(R.drawable.ic_homa_add, "افزودن کارت", primaryColor).apply {
            setOnClickListener { showEditor(null) }
        }, LinearLayout.LayoutParams(dp(48), dp(56)))
        addView(actions, FrameLayout.LayoutParams(dp(96), dp(56), Gravity.START or Gravity.BOTTOM))
    }

    private fun buildSearch(): View {
        searchInput = TextInputEditText(this).apply {
            hint = "جستجوی بانک یا شماره کارت"
            inputType = InputType.TYPE_CLASS_TEXT
            setSingleLine(true)
            textDirection = View.TEXT_DIRECTION_RTL
        }
        return TextInputLayout(this).apply {
            hint = "جستجو"
            endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            addView(searchInput)
            visibility = View.GONE
            searchInput.addTextChangedListener { renderCards() }
        }
    }

    private fun buildOverview(): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = roundedBackground(withAlpha(primaryColor, 0.08f), Color.TRANSPARENT, 0, dp(16))
        }

        val icon = MaterialCardView(this).apply {
            radius = dp(12).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(withAlpha(primaryColor, 0.14f))
            addView(iconView(R.drawable.ic_homa_card, "کارت بانکی", primaryColor))
        }
        box.addView(icon, LinearLayout.LayoutParams(dp(44), dp(44)))

        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(12), 0, 0, 0)
        }
        labels.addView(TextView(this@BankCardsActivity).apply {
            text = "کارت‌های ذخیره‌شده"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
        })
        countText = TextView(this).apply {
            textSize = 12f
            setTextColor(secondaryTextColor)
        }
        labels.addView(countText)
        box.addView(labels, LinearLayout.LayoutParams(0, -1, 1f))

        box.addView(MaterialButton(this).apply {
            text = "افزودن"
            this.icon = ContextCompat.getDrawable(context, R.drawable.ic_homa_add)
            iconTint = android.content.res.ColorStateList.valueOf(primaryColor)
            setTextColor(primaryColor)
            minWidth = 0
            minimumWidth = 0
            setPadding(dp(10), 0, dp(10), 0)
            setOnClickListener { showEditor(null) }
        }, LinearLayout.LayoutParams(-2, dp(44)))
        return box
    }

    private fun load() {
        Thread {
            val loaded = runCatching { repo.getCards() }.getOrElse { emptyList() }
            runOnUiThread {
                cards = loaded.toMutableList()
                renderCards()
            }
        }.start()
    }

    private fun renderCards() {
        val query = searchInput.text?.toString()?.trim().orEmpty()
        val filtered = if (query.isBlank()) cards else {
            val normalized = repo.normalizeCard(query)
            cards.filter { card ->
                card.visual?.persianName?.contains(query, ignoreCase = true) == true ||
                    card.visual?.englishName?.contains(query, ignoreCase = true) == true ||
                    card.cardNumber.contains(normalized) ||
                    card.holderName.contains(query, ignoreCase = true)
            }
        }

        cardsContainer.removeAllViews()
        filtered.forEachIndexed { index, card ->
            cardsContainer.addView(bankCardView(card), LinearLayout.LayoutParams(-1, dp(176)).apply {
                topMargin = if (index == 0) 0 else dp(12)
            })
        }

        emptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        countText.text = if (query.isBlank()) "${cards.size} کارت ثبت شده" else "${filtered.size} نتیجه از ${cards.size} کارت"

        val title = emptyState.findViewWithTag<TextView>("empty_title")
        val message = emptyState.findViewWithTag<TextView>("empty_message")
        if (query.isNotBlank() && filtered.isEmpty()) {
            title?.text = "نتیجه‌ای پیدا نشد"
            message?.text = "نام بانک، نام صاحب کارت یا بخشی از شماره کارت را جستجو کنید."
        } else {
            title?.text = "هنوز کارتی اضافه نشده"
            message?.text = "کارت‌های بانکی‌تان را یک‌جا و امن در Homa نگه دارید."
        }
    }

    private fun bankCardView(card: BankCard): View {
        val accent = card.visual?.color ?: primaryColor
        val cardRoot = MaterialCardView(this).apply {
            radius = dp(20).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(surfaceColor)
            strokeWidth = dp(1)
            strokeColor = withAlpha(accent, 0.22f)
            isClickable = true
            isFocusable = true
            setOnClickListener { showCardActions(card) }
        }

        val frame = FrameLayout(this).apply {
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            clipChildren = true
        }
        frame.addView(View(this).apply { setBackgroundColor(accent) },
            FrameLayout.LayoutParams(dp(5), -1, Gravity.END))

        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(18), dp(14), dp(22), dp(12))
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val logoHolder = MaterialCardView(this).apply {
            radius = dp(12).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(withAlpha(accent, 0.10f))
        }
        val logo = ImageView(this).apply {
            val name = card.visual?.logoResourceName
            if (!name.isNullOrBlank()) {
                resources.getIdentifier(name, "drawable", packageName).takeIf { it != 0 }?.let(::setImageResource)
            } else {
                setImageResource(R.drawable.ic_homa_card)
            }
            contentDescription = card.visual?.persianName ?: "بانک"
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        logoHolder.addView(logo)
        top.addView(logoHolder, LinearLayout.LayoutParams(dp(44), dp(44)))

        val bankInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(10), 0, 0, 0)
        }
        bankInfo.addView(TextView(this@BankCardsActivity).apply {
            text = card.visual?.persianName ?: card.bankId
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
        })
        bankInfo.addView(TextView(this@BankCardsActivity).apply {
            text = if (card.holderName.isBlank()) "نام صاحب کارت ثبت نشده" else card.holderName
            textSize = 12f
            setTextColor(secondaryTextColor)
        })
        top.addView(bankInfo, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(iconButton(R.drawable.ic_homa_more, "گزینه‌های کارت", secondaryTextColor).apply {
            setOnClickListener { showCardActions(card) }
        }, LinearLayout.LayoutParams(dp(44), dp(44)))
        main.addView(top)

        main.addView(TextView(this).apply {
            text = maskedCardNumber(card.cardNumber)
            textSize = 19f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            letterSpacing = .055f
            textDirection = View.TEXT_DIRECTION_LTR
            gravity = Gravity.CENTER
            setTextColor(textColor)
            setOnClickListener { copy(card.cardNumber) }
        }, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(7) })

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        bottom.addView(TextView(this@BankCardsActivity).apply {
            text = if (card.iban.isBlank()) "شبا ثبت نشده" else "IR ${repo.formatIban(card.iban)}"
            textSize = 11.5f
            textDirection = View.TEXT_DIRECTION_LTR
            setTextColor(secondaryTextColor)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }, LinearLayout.LayoutParams(0, dp(30), 1f))
        bottom.addView(MaterialButton(this).apply {
            text = "کپی"
            minWidth = 0
            minimumWidth = 0
            setPadding(dp(10), 0, dp(10), 0)
            this.icon = ContextCompat.getDrawable(context, R.drawable.ic_homa_copy)
            iconTint = android.content.res.ColorStateList.valueOf(primaryColor)
            setTextColor(primaryColor)
            setOnClickListener { copy(card.cardNumber) }
        }, LinearLayout.LayoutParams(-2, dp(36)))
        main.addView(bottom)

        frame.addView(main, FrameLayout.LayoutParams(-1, -1).apply { rightMargin = dp(5) })
        cardRoot.addView(frame)
        return cardRoot
    }

    private fun buildEmptyState() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(24), dp(38), dp(24), dp(24))

        val icon = MaterialCardView(this@BankCardsActivity).apply {
            radius = dp(30).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(withAlpha(primaryColor, 0.10f))
            addView(iconView(R.drawable.ic_homa_card, "کارت بانکی", primaryColor))
        }
        addView(icon, LinearLayout.LayoutParams(dp(60), dp(60)))

        addView(TextView(this@BankCardsActivity).apply {
            tag = "empty_title"
            text = "هنوز کارتی اضافه نشده"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(textColor)
            setPadding(0, dp(14), 0, 0)
        })
        addView(TextView(this@BankCardsActivity).apply {
            tag = "empty_message"
            text = "کارت‌های بانکی‌تان را یک‌جا و امن در Homa نگه دارید."
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(secondaryTextColor)
            setPadding(0, dp(7), 0, dp(16))
        })
        addView(MaterialButton(this@BankCardsActivity).apply {
            text = "افزودن کارت بانکی"
            icon = ContextCompat.getDrawable(context, R.drawable.ic_homa_add)
            iconTint = android.content.res.ColorStateList.valueOf(Color.WHITE)
            setOnClickListener { showEditor(null) }
        }, LinearLayout.LayoutParams(-2, dp(48)))
    }

    private fun showEditor(existing: BankCard?) {
        val dialog = BottomSheetDialog(this)
        val scroll = ScrollView(this)
        val sheet = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(20), dp(10), dp(20), dp(26))
        }
        scroll.addView(sheet)

        sheet.addView(TextView(this).apply {
            text = if (existing == null) "افزودن کارت بانکی" else "ویرایش کارت بانکی"
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
        })
        sheet.addView(TextView(this).apply {
            text = "شماره کارت را وارد کنید؛ بانک به‌صورت خودکار شناسایی می‌شود."
            textSize = 13f
            setTextColor(secondaryTextColor)
            setPadding(0, dp(5), 0, dp(14))
        })

        val cardInput = TextInputEditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "6037 9918 1234 5678"
            existing?.let { setText(repo.formatCard(it.cardNumber)) }
            textDirection = View.TEXT_DIRECTION_LTR
            gravity = Gravity.CENTER
        }
        sheet.addView(TextInputLayout(this).apply {
            hint = "شماره کارت"
            endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
            addView(cardInput)
        }, LinearLayout.LayoutParams(-1, dp(70)))

        val detected = TextView(this).apply {
            textSize = 12.5f
            setTextColor(primaryColor)
            setPadding(dp(4), dp(4), dp(4), dp(6))
        }
        sheet.addView(detected)

        val holderInput = TextInputEditText(this).apply {
            hint = "نام صاحب کارت"
            existing?.holderName?.let(::setText)
        }
        sheet.addView(TextInputLayout(this).apply {
            hint = "نام صاحب کارت (اختیاری)"
            addView(holderInput)
        }, LinearLayout.LayoutParams(-1, dp(70)).apply { topMargin = dp(6) })

        val ibanInput = TextInputEditText(this).apply {
            hint = "IR..."
            existing?.let { if (it.iban.isNotBlank()) setText(repo.formatIban(it.iban)) }
            textDirection = View.TEXT_DIRECTION_LTR
        }
        sheet.addView(TextInputLayout(this).apply {
            hint = "شماره شبا (اختیاری)"
            addView(ibanInput)
        }, LinearLayout.LayoutParams(-1, dp(70)).apply { topMargin = dp(6) })

        fun updateDetected() {
            val bank = repo.detect(repo.normalizeCard(cardInput.text?.toString().orEmpty()))
            detected.text = bank?.let { "بانک شناسایی‌شده: ${it.persianName}" } ?: ""
        }
        cardInput.addTextChangedListener { updateDetected() }
        updateDetected()

        sheet.addView(MaterialButton(this).apply {
            text = if (existing == null) "ذخیره کارت" else "ذخیره تغییرات"
            setOnClickListener {
                val card = repo.normalizeCard(cardInput.text?.toString().orEmpty())
                val holder = holderInput.text?.toString().orEmpty().trim()
                val iban = repo.normalizeIban(ibanInput.text?.toString().orEmpty())
                if (card.length != 16 || !repo.validCard(card) || repo.detect(card) == null) {
                    cardInput.error = "شماره کارت معتبر نیست یا بانک شناسایی نشد"
                    return@setOnClickListener
                }
                if (iban.isNotBlank() && !repo.validIban(iban)) {
                    ibanInput.error = "شماره شبا معتبر نیست"
                    return@setOnClickListener
                }

                isEnabled = false
                Thread {
                    val result = repo.save(existing, card, holder, iban)
                    runOnUiThread {
                        isEnabled = true
                        if (result.isSuccess) {
                            dialog.dismiss()
                            load()
                        } else {
                            Toast.makeText(
                                this@BankCardsActivity,
                                result.exceptionOrNull()?.message ?: "ذخیره کارت انجام نشد",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }.start()
            }
        }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(14) })

        dialog.setContentView(scroll)
        dialog.show()
        applySheetInsets(scroll)
    }

    private fun showCardActions(card: BankCard) {
        val dialog = BottomSheetDialog(this)
        val sheet = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(20), dp(12), dp(20), dp(22))
        }
        sheet.addView(TextView(this).apply {
            text = card.visual?.persianName ?: "کارت بانکی"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
            setPadding(0, 0, 0, dp(12))
        })

        sheet.addView(actionButton("ویرایش کارت", R.drawable.ic_homa_edit) {
            dialog.dismiss()
            showEditor(card)
        })
        sheet.addView(actionButton("کپی شماره کارت", R.drawable.ic_homa_copy) {
            dialog.dismiss()
            copy(card.cardNumber)
        })
        if (card.iban.isNotBlank()) {
            sheet.addView(actionButton("کپی شماره شبا", R.drawable.ic_homa_copy) {
                dialog.dismiss()
                copy(card.iban)
            })
        }
        sheet.addView(actionButton("اشتراک‌گذاری", R.drawable.ic_homa_share) {
            dialog.dismiss()
            share(card)
        })
        sheet.addView(actionButton("حذف کارت", R.drawable.ic_homa_more, textColor) {
            dialog.dismiss()
            MaterialAlertDialogBuilder(this)
                .setTitle("حذف کارت")
                .setMessage("این کارت از Homa حذف شود؟")
                .setNegativeButton("انصراف", null)
                .setPositiveButton("حذف") { _, _ ->
                    Thread {
                        val result = repo.delete(card)
                        runOnUiThread {
                            if (result.isSuccess) load()
                            else Toast.makeText(this, "حذف کارت انجام نشد", Toast.LENGTH_SHORT).show()
                        }
                    }.start()
                }
                .show()
        })

        dialog.setContentView(sheet)
        dialog.show()
        applySheetInsets(sheet)
    }

    private fun actionButton(label: String, iconRes: Int, tint: Int = primaryColor, action: () -> Unit) =
        MaterialButton(this).apply {
            text = label
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            this.icon = ContextCompat.getDrawable(context, iconRes)
            iconTint = android.content.res.ColorStateList.valueOf(tint)
            setTextColor(textColor)
            minHeight = dp(52)
            minimumHeight = dp(52)
            setPadding(dp(12), 0, dp(12), 0)
            setOnClickListener { action() }
        }

    private fun applySheetInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { target, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            target.updatePadding(bottom = dp(22) + bottom)
            insets
        }
        ViewCompat.requestApplyInsets(view)
    }

    private fun share(card: BankCard) {
        val text = buildString {
            append(card.visual?.persianName ?: "کارت بانکی")
            append("\nشماره کارت: ").append(repo.formatCard(card.cardNumber))
            if (card.holderName.isNotBlank()) append("\nصاحب کارت: ").append(card.holderName)
            if (card.iban.isNotBlank()) append("\nشماره شبا: ").append(repo.formatIban(card.iban))
        }
        startActivity(Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            },
            "اشتراک‌گذاری کارت"
        ))
    }

    private fun copy(value: String) {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("Homa", value))
        Toast.makeText(this, "کپی شد", Toast.LENGTH_SHORT).show()
    }

    private fun maskedCardNumber(value: String): String {
        val card = repo.normalizeCard(value)
        return if (card.length == 16) "••••  ••••  ••••  ${card.takeLast(4)}" else repo.formatCard(card)
    }

    private fun iconButton(resId: Int, description: String, tint: Int): ImageView =
        ImageView(this).apply {
            setImageResource(resId)
            imageTintList = android.content.res.ColorStateList.valueOf(tint)
            contentDescription = description
            scaleType = ImageView.ScaleType.CENTER
            setPadding(dp(12), dp(12), dp(12), dp(12))
            isClickable = true
            isFocusable = true
        }

    private fun iconView(resId: Int, description: String, tint: Int): ImageView =
        ImageView(this).apply {
            setImageResource(resId)
            imageTintList = android.content.res.ColorStateList.valueOf(tint)
            contentDescription = description
            scaleType = ImageView.ScaleType.CENTER
            setPadding(dp(9), dp(9), dp(9), dp(9))
        }

    private fun roundedBackground(fill: Int, stroke: Int, strokeWidth: Int, radius: Int) =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(fill)
            if (strokeWidth > 0) setStroke(strokeWidth, stroke)
            cornerRadius = radius.toFloat()
        }

    private fun withAlpha(color: Int, alpha: Float): Int =
        Color.argb((255f * alpha).toInt().coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

    private fun themeColor(attr: Int): Int {
        val value = android.util.TypedValue()
        if (!theme.resolveAttribute(attr, value, true)) return Color.GRAY
        return if (value.resourceId != 0) {
            runCatching { ContextCompat.getColor(this, value.resourceId) }.getOrDefault(value.data)
        } else value.data
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
