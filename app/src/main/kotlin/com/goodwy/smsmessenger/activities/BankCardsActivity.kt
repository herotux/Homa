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
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
 * Homa Bank Cards screen.
 *
 * Uses the same Material/theme colors as the rest of the app.
 * No custom icon drawing, no custom navigation bar color, and no balance/bottom navigation.
 */
class BankCardsActivity : AppCompatActivity() {
    private lateinit var repo: BankCardsRepository
    private lateinit var cardsContainer: LinearLayout
    private lateinit var emptyState: LinearLayout
    private lateinit var root: FrameLayout
    private var cards = mutableListOf<BankCard>()

    private val backgroundColor: Int
        get() = themeColor(com.google.android.material.R.attr.colorSurface)

    private val surfaceColor: Int
        get() = themeColor(com.google.android.material.R.attr.colorSurface)

    private val primaryColor: Int
        get() = themeColor(com.google.android.material.R.attr.colorPrimary)

    private val textColor: Int
        get() = themeColor(android.R.attr.textColorPrimary)

    private val secondaryTextColor: Int
        get() = themeColor(android.R.attr.textColorSecondary)

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        repo = BankCardsRepository(this)

        // Let the theme own system-bar colors. Content receives the insets instead of
        // painting a separate status/navigation-bar background.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        buildPage()
        load()
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

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16), 0, dp(16), dp(32))
        }

        content.addView(
            buildHeader(),
            LinearLayout.LayoutParams(-1, dp(88))
        )

        content.addView(
            buildFilters(),
            LinearLayout.LayoutParams(-1, dp(52)).apply {
                bottomMargin = dp(12)
            }
        )

        emptyState = emptyState()
        content.addView(emptyState)

        cardsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        content.addView(cardsContainer, LinearLayout.LayoutParams(-1, -2))

        scroll.addView(content)
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)

        // Apply only the system insets needed by the screen. The header gets the top
        // inset, and the scrolling content gets the bottom inset so the last card/FAB
        // never sits behind the navigation area.
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            content.updatePadding(
                left = dp(16),
                top = bars.top,
                right = dp(16),
                bottom = dp(32) + bars.bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun buildHeader(): View {
        return FrameLayout(this).apply {
            layoutDirection = View.LAYOUT_DIRECTION_LTR

            addView(
                themedIcon(android.R.drawable.ic_menu_search, "جستجوی کارت").apply {
                    setOnClickListener { showSearchSheet() }
                },
                FrameLayout.LayoutParams(dp(48), dp(56), Gravity.START or Gravity.BOTTOM)
            )

            addView(
                TextView(this@BankCardsActivity).apply {
                    text = "کارت‌ها"
                    textSize = 22f
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    setTextColor(textColor)
                    layoutDirection = View.LAYOUT_DIRECTION_RTL
                },
                FrameLayout.LayoutParams(-2, dp(56), Gravity.CENTER or Gravity.BOTTOM)
            )
        }
    }

    private fun buildFilters(): View {
        // Only the filter that is currently implemented is shown. Disabled fake
        // filters were removed instead of displaying a "coming later" toast.
        return TextView(this).apply {
            text = "فعال"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(primaryColor)
            background = roundedBackground(surfaceColor, primaryColor, dp(1), dp(22))
            isClickable = false
        }
    }

    private fun load() {
        Thread {
            val loaded = runCatching { repo.getCards() }.getOrElse {
                emptyList()
            }
            runOnUiThread {
                cards = loaded.toMutableList()
                renderCards()
            }
        }.start()
    }

    private fun renderCards() {
        cardsContainer.removeAllViews()
        emptyState.visibility = if (cards.isEmpty()) View.VISIBLE else View.GONE

        cards.forEachIndexed { index, card ->
            cardsContainer.addView(
                bankCardView(card),
                LinearLayout.LayoutParams(-1, dp(145)).apply {
                    topMargin = if (index == 0) dp(4) else dp(12)
                }
            )
        }

        root.findViewWithTag<View>("bank_cards_fab")?.let(root::removeView)

        if (cards.isNotEmpty()) {
            val fab = MaterialCardView(this).apply {
                tag = "bank_cards_fab"
                radius = dp(28).toFloat()
                cardElevation = dp(4).toFloat()
                setCardBackgroundColor(primaryColor)
                isClickable = true
                isFocusable = true
                addView(
                    themedIcon(android.R.drawable.ic_input_add, "افزودن کارت", Color.WHITE),
                    FrameLayout.LayoutParams(dp(56), dp(56))
                )
                setOnClickListener { showEditor(null) }
            }

            root.addView(
                fab,
                FrameLayout.LayoutParams(dp(56), dp(56), Gravity.BOTTOM or Gravity.END).apply {
                    marginEnd = dp(20)
                    bottomMargin = dp(20)
                }
            )

            ViewCompat.setOnApplyWindowInsetsListener(fab) { view, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                val lp = view.layoutParams as FrameLayout.LayoutParams
                lp.bottomMargin = dp(20) + bars.bottom
                view.layoutParams = lp
                insets
            }
        }
    }

    private fun bankCardView(card: BankCard): View {
        val cardRoot = MaterialCardView(this).apply {
            radius = dp(20).toFloat()
            cardElevation = dp(1).toFloat()
            setCardBackgroundColor(surfaceColor)
            strokeWidth = dp(1)
            strokeColor = withAlpha(primaryColor, 0.12f)
            clipChildren = true
        }

        val frame = FrameLayout(this).apply {
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16), dp(12), dp(16), dp(10))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val logo = ImageView(this).apply {
            val name = card.visual?.logoResourceName
            if (!name.isNullOrBlank()) {
                resources.getIdentifier(name, "drawable", packageName)
                    .takeIf { it != 0 }?.let(::setImageResource)
            }
            contentDescription = card.visual?.persianName ?: "بانک"
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        header.addView(logo, LinearLayout.LayoutParams(dp(38), dp(38)))

        header.addView(
            TextView(this).apply {
                text = card.visual?.persianName ?: card.bankId
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(textColor)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(8), 0, 0, 0)
            },
            LinearLayout.LayoutParams(0, dp(42), 1f)
        )
        main.addView(header)

        val numberRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }

        numberRow.addView(
            TextView(this).apply {
                text = repo.formatCard(card.cardNumber)
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = .045f
                textDirection = View.TEXT_DIRECTION_LTR
                gravity = Gravity.CENTER
                setTextColor(textColor)
            },
            LinearLayout.LayoutParams(0, dp(48), 1f)
        )

        numberRow.addView(
            themedIcon(android.R.drawable.ic_menu_set_as, "کپی شماره کارت", primaryColor).apply {
                setOnClickListener { copy(card.cardNumber) }
            },
            LinearLayout.LayoutParams(dp(42), dp(48))
        )

        main.addView(numberRow, LinearLayout.LayoutParams(-1, dp(48)).apply {
            topMargin = dp(4)
        })

        frame.addView(main, FrameLayout.LayoutParams(-1, -1).apply {
            leftMargin = dp(64)
        })

        val rail = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(surfaceColor)
        }

        rail.addView(
            themedIcon(android.R.drawable.ic_menu_manage, "ویرایش کارت", primaryColor).apply {
                setOnClickListener { showEditor(card) }
            },
            LinearLayout.LayoutParams(dp(64), dp(64))
        )

        rail.addView(
            View(this).apply { setBackgroundColor(withAlpha(primaryColor, 0.18f)) },
            LinearLayout.LayoutParams(dp(1), 0, 1f)
        )

        rail.addView(
            themedIcon(android.R.drawable.ic_menu_more, "گزینه‌های کارت", primaryColor).apply {
                setOnClickListener { showCardActions(card) }
            },
            LinearLayout.LayoutParams(dp(64), dp(64))
        )

        frame.addView(rail, FrameLayout.LayoutParams(dp(64), -1, Gravity.LEFT))
        cardRoot.addView(frame)
        cardRoot.setOnClickListener { showCardActions(card) }
        return cardRoot
    }

    private fun emptyState() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(24), dp(48), dp(24), dp(18))

        addView(TextView(this@BankCardsActivity).apply {
            text = "هنوز کارتی اضافه نشده"
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(textColor)
        })

        addView(TextView(this@BankCardsActivity).apply {
            text = "کارت بانکی خود را اضافه کنید تا شماره کارت و شبا همیشه در دسترس باشد."
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(secondaryTextColor)
            setPadding(0, dp(8), 0, dp(18))
        })

        addView(MaterialButton(this@BankCardsActivity).apply {
            text = "افزودن اولین کارت"
            setOnClickListener { showEditor(null) }
        }, LinearLayout.LayoutParams(-2, dp(48)))
    }

    private fun showSearchSheet() {
        val sheet = BottomSheetDialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(20), dp(16), dp(20), dp(28))
        }

        root.addView(TextView(this).apply {
            text = "جستجوی کارت"
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
        })

        val input = EditText(this).apply {
            hint = "نام بانک یا شماره کارت"
            inputType = InputType.TYPE_CLASS_TEXT
            setSingleLine()
        }
        root.addView(input, LinearLayout.LayoutParams(-1, dp(60)).apply {
            topMargin = dp(10)
        })

        root.addView(MaterialButton(this).apply {
            text = "بستن"
            setOnClickListener { sheet.dismiss() }
        }, LinearLayout.LayoutParams(-1, dp(50)).apply {
            topMargin = dp(10)
        })

        sheet.setContentView(root)
        sheet.show()
    }

    private fun showEditor(existing: BankCard?) {
        val dialog = BottomSheetDialog(this)
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(20), dp(12), dp(20), dp(28))
        }
        scroll.addView(root)

        root.addView(TextView(this).apply {
            text = if (existing == null) "افزودن کارت بانکی" else "ویرایش کارت بانکی"
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
        })

        root.addView(TextView(this).apply {
            text = "شماره کارت را وارد کنید؛ بانک به‌صورت خودکار شناسایی می‌شود."
            textSize = 13f
            setTextColor(secondaryTextColor)
            setPadding(0, dp(6), 0, dp(16))
        })

        val cardInput = TextInputEditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "6037 9918 1234 5678"
            existing?.let { setText(repo.formatCard(it.cardNumber)) }
            textDirection = View.TEXT_DIRECTION_LTR
            gravity = Gravity.CENTER
        }
        root.addView(TextInputLayout(this).apply {
            hint = "شماره کارت"
            endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
            addView(cardInput)
        }, LinearLayout.LayoutParams(-1, dp(72)))

        val holderInput = TextInputEditText(this).apply {
            hint = "نام صاحب کارت"
            existing?.holderName?.let { setText(it) }
        }
        root.addView(TextInputLayout(this).apply {
            hint = "نام صاحب کارت (اختیاری)"
            addView(holderInput)
        }, LinearLayout.LayoutParams(-1, dp(72)).apply { topMargin = dp(8) })

        val ibanInput = TextInputEditText(this).apply {
            hint = "IR..."
            existing?.let { if (it.iban.isNotBlank()) setText(repo.formatIban(it.iban)) }
            textDirection = View.TEXT_DIRECTION_LTR
        }
        root.addView(TextInputLayout(this).apply {
            hint = "شماره شبا (اختیاری)"
            addView(ibanInput)
        }, LinearLayout.LayoutParams(-1, dp(72)).apply { topMargin = dp(8) })

        val detected = TextView(this).apply {
            textSize = 13f
            setTextColor(primaryColor)
            setPadding(dp(4), dp(4), dp(4), dp(8))
        }
        root.addView(detected)

        fun updateDetected() {
            val bank = repo.detect(repo.normalizeCard(cardInput.text?.toString().orEmpty()))
            detected.text = bank?.let { "بانک شناسایی‌شده: " + it.persianName } ?: ""
        }

        cardInput.addTextChangedListener {
            updateDetected()
        }
        updateDetected()

        root.addView(MaterialButton(this).apply {
            text = "ذخیره کارت"
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
        }, LinearLayout.LayoutParams(-1, dp(52)).apply {
            topMargin = dp(14)
        })

        dialog.setContentView(scroll)
        dialog.show()
    }

    private fun showCardActions(card: BankCard) {
        val sheet = BottomSheetDialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(20), dp(16), dp(20), dp(28))
        }

        root.addView(TextView(this).apply {
            text = card.visual?.persianName ?: "کارت بانکی"
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
            setPadding(0, 0, 0, dp(10))
        })

        actionItem(root, sheet, "ویرایش کارت") { showEditor(card) }
        actionItem(root, sheet, "کپی شماره کارت") { copy(card.cardNumber) }
        if (card.iban.isNotBlank()) {
            actionItem(root, sheet, "کپی شماره شبا") { copy(card.iban) }
        }
        actionItem(root, sheet, "اشتراک‌گذاری") { share(card) }
        actionItem(root, sheet, "حذف کارت") {
            MaterialAlertDialogBuilder(this)
                .setTitle("حذف کارت")
                .setMessage("این کارت از Homa حذف شود؟")
                .setNegativeButton("انصراف", null)
                .setPositiveButton("حذف") { _, _ ->
                    Thread {
                        val result = repo.delete(card)
                        runOnUiThread {
                            if (result.isSuccess) load()
                            else Toast.makeText(
                                this@BankCardsActivity,
                                "حذف کارت انجام نشد",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }.start()
                }
                .show()
        }

        sheet.setContentView(root)
        sheet.show()
    }

    private fun actionItem(
        root: LinearLayout,
        sheet: BottomSheetDialog,
        label: String,
        action: () -> Unit
    ) {
        root.addView(MaterialButton(this).apply {
            text = label
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setOnClickListener {
                sheet.dismiss()
                action()
            }
        }, LinearLayout.LayoutParams(-1, dp(52)).apply {
            topMargin = dp(6)
        })
    }

    private fun copy(value: String) {
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("Homa", value))
        Toast.makeText(this, "کپی شد", Toast.LENGTH_SHORT).show()
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

    private fun themedIcon(resId: Int, description: String, tint: Int = textColor): ImageView {
        return ImageView(this).apply {
            setImageResource(resId)
            imageTintList = android.content.res.ColorStateList.valueOf(tint)
            contentDescription = description
            scaleType = ImageView.ScaleType.CENTER
            setPadding(dp(12), dp(12), dp(12), dp(12))
            isClickable = true
            isFocusable = true
        }
    }

    private fun roundedBackground(fill: Int, stroke: Int, strokeWidth: Int, radius: Int) =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(fill)
            setStroke(strokeWidth, stroke)
            cornerRadius = radius.toFloat()
        }

    private fun withAlpha(color: Int, alpha: Float): Int =
        Color.argb(
            (255f * alpha).toInt().coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )

    private fun themeColor(attr: Int): Int {
        val value = android.util.TypedValue()
        if (!theme.resolveAttribute(attr, value, true)) return Color.GRAY
        return if (value.resourceId != 0) {
            runCatching { androidx.core.content.ContextCompat.getColor(this, value.resourceId) }
                .getOrDefault(value.data)
        } else {
            value.data
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
