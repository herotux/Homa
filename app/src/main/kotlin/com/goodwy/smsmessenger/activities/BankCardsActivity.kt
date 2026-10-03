package com.goodwy.smsmessenger.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.goodwy.smsmessenger.features.bankcards.BankCard
import com.goodwy.smsmessenger.features.bankcards.BankCardsRepository
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class BankCardsActivity : androidx.appcompat.app.AppCompatActivity() {
    private lateinit var repo: BankCardsRepository
    private lateinit var cardsContainer: LinearLayout
    private lateinit var emptyState: LinearLayout
    private var cards = mutableListOf<BankCard>()

    private val bgColor = Color.rgb(246, 247, 252)
    private val surfaceColor get() = Color.WHITE
    private val primaryColor = Color.rgb(33, 150, 243)
    private val textColor = Color.rgb(35, 35, 38)
    private val secondaryTextColor = Color.rgb(105, 106, 112)

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        repo = BankCardsRepository(this)
        window.statusBarColor = bgColor
        window.navigationBarColor = Color.BLACK
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        buildPage()
        load()
    }

    private fun buildPage() {
        val root = FrameLayout(this).apply {
            setBackgroundColor(bgColor)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val scroll = ScrollView(this).apply {
            clipToPadding = false
            isFillViewport = false
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16), 0, dp(16), dp(24))
        }
        scroll.addView(content)

        content.addView(buildHeader(), LinearLayout.LayoutParams(-1, dp(70)))
        content.addView(buildFilters(), LinearLayout.LayoutParams(-1, dp(60)).apply {
            topMargin = dp(4)
            bottomMargin = dp(8)
        })

        emptyState = emptyState()
        content.addView(emptyState)

        cardsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        content.addView(cardsContainer, LinearLayout.LayoutParams(-1, -2))

        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
    }

    private fun buildHeader(): View {
        return FrameLayout(this).apply {
            layoutDirection = View.LAYOUT_DIRECTION_LTR

            addView(IconView(this@BankCardsActivity, IconType.SEARCH).apply {
                contentDescription = "جستجو"
                setOnClickListener { showSearchSheet() }
            }, FrameLayout.LayoutParams(dp(48), dp(56), Gravity.START or Gravity.CENTER_VERTICAL).apply {
                leftMargin = dp(2)
            })

            addView(TextView(this@BankCardsActivity).apply {
                text = "کارت‌ها"
                textSize = 24f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(textColor)
                layoutDirection = View.LAYOUT_DIRECTION_RTL
            }, FrameLayout.LayoutParams(-2, dp(56), Gravity.CENTER))

        }
    }

    private fun buildFilters(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        row.addView(filterChip("فعال", true), LinearLayout.LayoutParams(0, dp(48), 1f).apply {
            marginStart = dp(4)
        })
        row.addView(filterChip("پنهان شده", false), LinearLayout.LayoutParams(0, dp(48), 1.25f).apply {
            marginStart = dp(4)
            marginEnd = dp(4)
        })
        row.addView(filterChip("غیرفعال", false), LinearLayout.LayoutParams(0, dp(48), 1f).apply {
            marginEnd = dp(4)
        })
        return row
    }

    private fun filterChip(title: String, selected: Boolean): TextView {
        return TextView(this).apply {
            text = title
            textSize = 16f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT
            setTextColor(if (selected) Color.WHITE else textColor)
            background = GradientDrawable().apply {
                cornerRadius = dp(28).toFloat()
                if (selected) {
                    setColor(primaryColor)
                } else {
                    setColor(bgColor)
                    setStroke(dp(1), secondaryTextColor.adjustAlpha(0.55f))
                }
            }
            setOnClickListener {
                if (title == "فعال") return@setOnClickListener
                Toast.makeText(this@BankCardsActivity, "این فیلتر در نسخه بعد فعال می‌شود", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun load() {
        Thread {
            val loaded = repo.getCards()
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
            cardsContainer.addView(bankCardView(card), LinearLayout.LayoutParams(-1, dp(145)).apply {
                topMargin = if (index == 0) dp(4) else dp(12)
            })
        }

        if (cards.isNotEmpty()) {
            val pageRoot = ((cardsContainer.parent as? View)?.parent as? View)?.parent as? FrameLayout
            pageRoot?.findViewWithTag<View>("bank_cards_fab")?.let(pageRoot::removeView)
            pageRoot?.let { root ->
                val fab = MaterialCardView(this).apply {
                    tag = "bank_cards_fab"
                    radius = dp(30).toFloat()
                    cardElevation = dp(5).toFloat()
                    setCardBackgroundColor(primaryColor)
                    addView(IconView(this@BankCardsActivity, IconType.PLUS).apply {
                        setIconColor(Color.WHITE)
                    }, FrameLayout.LayoutParams(dp(58), dp(58)))
                    setOnClickListener { showEditor(null) }
                }
                root.addView(fab, FrameLayout.LayoutParams(dp(58), dp(58), Gravity.BOTTOM or Gravity.END).apply {
                    bottomMargin = dp(28)
                    marginEnd = dp(24)
                })
            }
        }
    }

    private fun bankCardView(card: BankCard): View {
        val cardRoot = MaterialCardView(this).apply {
            radius = dp(24).toFloat()
            cardElevation = dp(1).toFloat()
            setCardBackgroundColor(surfaceColor)
            strokeWidth = 0
            clipChildren = true
        }

        val frame = FrameLayout(this).apply {
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(surfaceColor)
        }

        frame.addView(CardPatternView(this), FrameLayout.LayoutParams(-1, -1))

        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16), dp(12), dp(18), dp(10))
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

        header.addView(TextView(this).apply {
            text = card.visual?.persianName ?: card.bankId
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, dp(42), 1f))

        main.addView(header)

        val numberRow = FrameLayout(this).apply {
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        numberRow.addView(TextView(this@BankCardsActivity).apply {
            text = repo.formatCard(card.cardNumber)
            textSize = 19f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = .045f
            textDirection = View.TEXT_DIRECTION_LTR
            gravity = Gravity.CENTER
            setTextColor(textColor)
        }, FrameLayout.LayoutParams(-1, dp(48)))
        numberRow.addView(IconView(this, IconType.COPY).apply {
            setIconColor(primaryColor)
            setOnClickListener { copy(card.cardNumber) }
        }, FrameLayout.LayoutParams(dp(42), dp(48), Gravity.END))
        main.addView(numberRow, LinearLayout.LayoutParams(-1, dp(48)).apply {
            topMargin = dp(4)
        })

        frame.addView(main, FrameLayout.LayoutParams(-1, -1).apply {
            leftMargin = dp(72)
            rightMargin = 0
        })

        val rail = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setBackgroundColor(surfaceColor)
        }

        rail.addView(IconView(this, IconType.SETTINGS).apply {
            setIconColor(primaryColor)
            setOnClickListener { showEditor(card) }
        }, LinearLayout.LayoutParams(dp(64), dp(64)))

        val divider = View(this).apply {
            setBackgroundColor(primaryColor.adjustAlpha(.22f))
        }
        rail.addView(divider, LinearLayout.LayoutParams(dp(1), 0, 1f))

        rail.addView(IconView(this, IconType.MORE).apply {
            setIconColor(primaryColor)
            setOnClickListener { showCardActions(card) }
        }, LinearLayout.LayoutParams(dp(64), dp(64)))

        frame.addView(rail, FrameLayout.LayoutParams(dp(72), -1, Gravity.LEFT))
        cardRoot.addView(frame)
        cardRoot.setOnClickListener { showCardActions(card) }
        return cardRoot
    }

    private fun emptyState() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(24), dp(45), dp(24), dp(18))
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
        root.addView(input, LinearLayout.LayoutParams(-1, dp(60)).apply { topMargin = dp(10) })
        root.addView(MaterialButton(this).apply {
            text = "بستن"
            setOnClickListener { sheet.dismiss() }
        }, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(10) })
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
        cardInput.addTextChangedListenerCompat { updateDetected() }
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
                            Toast.makeText(this@BankCardsActivity, result.exceptionOrNull()?.message ?: "ذخیره کارت انجام نشد", Toast.LENGTH_LONG).show()
                        }
                    }
                }.start()
            }
        }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(14) })

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
        if (card.iban.isNotBlank()) actionItem(root, sheet, "کپی شماره شبا") { copy(card.iban) }
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
                            else Toast.makeText(this@BankCardsActivity, "حذف کارت انجام نشد", Toast.LENGTH_SHORT).show()
                        }
                    }.start()
                }.show()
        }
        sheet.setContentView(root)
        sheet.show()
    }

    private fun actionItem(root: LinearLayout, sheet: BottomSheetDialog, label: String, action: () -> Unit) {
        root.addView(MaterialButton(this).apply {
            text = label
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setOnClickListener { sheet.dismiss(); action() }
        }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(6) })
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
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }, "اشتراک‌گذاری کارت"))
    }

    private fun TextView.addTextChangedListenerCompat(onChange: () -> Unit) {
        addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = onChange()
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun themeColor(attr: Int): Int {
        val value = android.util.TypedValue()
        if (!theme.resolveAttribute(attr, value, true)) return Color.GRAY
        return if (value.resourceId != 0) {
            runCatching { ContextCompat.getColor(this, value.resourceId) }.getOrDefault(value.data)
        } else value.data
    }

    private fun Int.adjustAlpha(factor: Float): Int {
        return Color.argb((Color.alpha(this) * factor).toInt(), Color.red(this), Color.green(this), Color.blue(this))
    }

    private enum class IconType {
        SEARCH, SETTINGS, MORE, COPY, CARD, PLUS, PROFILE, BOOKMARK, ACCOUNTS
    }

    private class IconView(context: Context, private val type: IconType) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = resources.displayMetrics.density * 2.2f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        private var iconColor = Color.DKGRAY

        fun setIconColor(color: Int) {
            iconColor = color
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            paint.color = iconColor
            val w = width.toFloat()
            val h = height.toFloat()
            val cx = w / 2f
            val cy = h / 2f
            val s = minOf(w, h) * .30f

            when (type) {
                IconType.SEARCH -> {
                    canvas.drawCircle(cx - s * .25f, cy - s * .2f, s * .62f, paint)
                    canvas.drawLine(cx + s * .2f, cy + s * .25f, cx + s * .78f, cy + s * .83f, paint)
                }
                IconType.SETTINGS -> drawGear(canvas, cx, cy, s)
                IconType.COPY -> {
                    canvas.drawRoundRect(RectF(cx - s * .62f, cy - s * .72f, cx + s * .25f, cy + s * .68f), s * .12f, s * .12f, paint)
                    canvas.drawRoundRect(RectF(cx - s * .18f, cy - s * .35f, cx + s * .72f, cy + s * .98f), s * .12f, s * .12f, paint)
                }
                IconType.MORE -> {
                    canvas.drawCircle(cx, cy - s * .55f, s * .13f, paint)
                    canvas.drawCircle(cx, cy, s * .13f, paint)
                    canvas.drawCircle(cx, cy + s * .55f, s * .13f, paint)
                }
                IconType.CARD -> {
                    canvas.drawRoundRect(RectF(cx - s * 1.1f, cy - s * .7f, cx + s * 1.1f, cy + s * .7f), s * .18f, s * .18f, paint)
                    canvas.drawLine(cx - s * 1.05f, cy - s * .15f, cx + s * 1.05f, cy - s * .15f, paint)
                    canvas.drawLine(cx - s * .7f, cy + s * .35f, cx - s * .15f, cy + s * .35f, paint)
                }
                IconType.PLUS -> {
                    canvas.drawLine(cx - s, cy, cx + s, cy, paint)
                    canvas.drawLine(cx, cy - s, cx, cy + s, paint)
                }
                IconType.PROFILE -> {
                    canvas.drawCircle(cx, cy - s * .5f, s * .45f, paint)
                    val p = Path()
                    p.moveTo(cx - s * .95f, cy + s * .9f)
                    p.quadTo(cx, cy + s * .1f, cx + s * .95f, cy + s * .9f)
                    canvas.drawPath(p, paint)
                }
                IconType.BOOKMARK -> {
                    val p = Path()
                    p.moveTo(cx - s * .65f, cy - s)
                    p.lineTo(cx + s * .65f, cy - s)
                    p.lineTo(cx + s * .65f, cy + s)
                    p.lineTo(cx, cy + s * .45f)
                    p.lineTo(cx - s * .65f, cy + s)
                    p.close()
                    canvas.drawPath(p, paint)
                }
                IconType.ACCOUNTS -> {
                    canvas.drawRoundRect(RectF(cx - s, cy - s * .7f, cx + s, cy + s * .7f), s * .15f, s * .15f, paint)
                    canvas.drawLine(cx - s * .65f, cy - s * .15f, cx + s * .65f, cy - s * .15f, paint)
                    canvas.drawCircle(cx + s * .45f, cy + s * .3f, s * .12f, paint)
                }
            }
        }

        private fun drawGear(canvas: Canvas, cx: Float, cy: Float, s: Float) {
            canvas.drawCircle(cx, cy, s * .55f, paint)
            canvas.drawCircle(cx, cy, s * .22f, paint)
            for (i in 0 until 8) {
                val a = Math.toRadians(i * 45.0)
                val x1 = cx + kotlin.math.cos(a).toFloat() * s * .68f
                val y1 = cy + kotlin.math.sin(a).toFloat() * s * .68f
                val x2 = cx + kotlin.math.cos(a).toFloat() * s * .95f
                val y2 = cy + kotlin.math.sin(a).toFloat() * s * .95f
                canvas.drawLine(x1, y1, x2, y2, paint)
            }
        }
    }

    private class CardPatternView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = resources.displayMetrics.density * 1.1f
            color = Color.rgb(205, 227, 244)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat()
            val h = height.toFloat()

            val p1 = Path()
            p1.moveTo(-20f, h * .2f)
            p1.cubicTo(w * .05f, h * .05f, w * .18f, h * .15f, w * .22f, h * .38f)
            p1.cubicTo(w * .28f, h * .68f, w * .18f, h * .95f, w * .38f, h + 15f)
            canvas.drawPath(p1, paint)

            val p2 = Path()
            p2.moveTo(w * .18f, -10f)
            p2.cubicTo(w * .32f, h * .15f, w * .55f, h * .08f, w * .62f, h * .34f)
            p2.cubicTo(w * .68f, h * .58f, w * .55f, h * .72f, w * .66f, h + 10f)
            canvas.drawPath(p2, paint)

            val p3 = Path()
            p3.moveTo(w * .55f, -10f)
            p3.cubicTo(w * .45f, h * .22f, w * .48f, h * .45f, w * .8f, h * .48f)
            p3.cubicTo(w * .98f, h * .5f, w * .98f, h * .72f, w + 20f, h * .8f)
            canvas.drawPath(p3, paint)

            canvas.drawRoundRect(RectF(w * .2f, h * .34f, w * .78f, h * .72f), 2f * d, 2f * d, paint)
        }
    }
}
