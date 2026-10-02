package com.goodwy.smsmessenger.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.goodwy.smsmessenger.features.bankcards.BankCard
import com.goodwy.smsmessenger.features.bankcards.BankCardsRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class BankCardsActivity : AppCompatActivity() {
    private lateinit var repo: BankCardsRepository
    private lateinit var pager: ViewPager2
    private lateinit var adapter: CardAdapter
    private lateinit var content: LinearLayout
    private lateinit var emptyState: LinearLayout
    private lateinit var pageCount: TextView
    private var detailsContainer: LinearLayout? = null
    private var cards = mutableListOf<BankCard>()

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        repo = BankCardsRepository(this)
        buildPage()
        load()
    }

    private fun buildPage() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(themeColor(com.google.android.material.R.attr.colorSurface))
        }

        val toolbar = MaterialToolbar(this).apply {
            title = "کارت‌های بانکی"
            setTitleTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface))
            setBackgroundColor(themeColor(com.google.android.material.R.attr.colorSurface))
            navigationIcon = ContextCompat.getDrawable(this@BankCardsActivity, androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            navigationIcon?.setTint(themeColor(com.google.android.material.R.attr.colorOnSurface))
            setNavigationOnClickListener { finish() }
            menu.add("مرتب‌سازی").apply { setShowAsAction(0) }
            setOnMenuItemClickListener {
                showSortSheet()
                true
            }
        }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(64)))

        val scroll = ScrollView(this).apply { clipToPadding = false; isFillViewport = true }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16), dp(12), dp(16), dp(96))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        emptyState = emptyState()
        content.addView(emptyState)

        pager = ViewPager2(this).apply {
            clipToPadding = false
            clipChildren = false
            setPadding(dp(10), dp(8), dp(10), dp(8))
            offscreenPageLimit = 2
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    updatePageIndicator(position)
                    rebuildDetails(position)
                }
            })
        }
        adapter = CardAdapter()
        pager.adapter = adapter
        content.addView(pager, LinearLayout.LayoutParams(-1, dp(250)).apply { topMargin = dp(8) })

        pageCount = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 12f
            setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
        }
        content.addView(pageCount, LinearLayout.LayoutParams(-1, dp(28)))

        content.addView(MaterialButton(this).apply {
            text = "افزودن کارت جدید"
            icon = ContextCompat.getDrawable(this@BankCardsActivity, android.R.drawable.ic_input_add)
            iconTint = android.content.res.ColorStateList.valueOf(themeColor(com.google.android.material.R.attr.colorOnPrimary))
            setOnClickListener { showEditor(null) }
        }, LinearLayout.LayoutParams(-1, dp(52)).apply {
            topMargin = dp(4)
            bottomMargin = dp(20)
        })

        content.addView(TextView(this).apply {
            text = "اطلاعات کارت"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface))
            setPadding(dp(4), dp(4), dp(4), dp(8))
        }, LinearLayout.LayoutParams(-1, dp(40)))

        detailsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        content.addView(detailsContainer, LinearLayout.LayoutParams(-1, -2))
        setContentView(root)
    }

    private fun emptyState() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(24), dp(42), dp(24), dp(18))
        addView(TextView(this@BankCardsActivity).apply {
            text = "هنوز کارتی اضافه نشده"
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface))
        })
        addView(TextView(this@BankCardsActivity).apply {
            text = "کارت بانکی خود را اضافه کنید تا شماره کارت و شبا را سریع در دسترس داشته باشید."
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(0, dp(8), 0, dp(18))
        })
        addView(MaterialButton(this@BankCardsActivity).apply {
            text = "افزودن اولین کارت"
            setOnClickListener { showEditor(null) }
        }, LinearLayout.LayoutParams(-2, dp(48)))
    }

    private fun load() {
        Thread {
            val loaded = repo.getCards()
            runOnUiThread {
                cards = loaded.toMutableList()
                adapter.notifyDataSetChanged()
                val hasCards = cards.isNotEmpty()
                emptyState.visibility = if (hasCards) View.GONE else View.VISIBLE
                pager.visibility = if (hasCards) View.VISIBLE else View.GONE
                pageCount.visibility = if (cards.size > 1) View.VISIBLE else View.GONE
                if (hasCards) {
                    pager.setCurrentItem(0, false)
                    updatePageIndicator(0)
                    rebuildDetails(0)
                } else {
                    detailsContainer?.removeAllViews()
                }
            }
        }.start()
    }

    private fun updatePageIndicator(position: Int) {
        pageCount.text = if (cards.size > 1) (position + 1).toString() + " از " + cards.size else ""
    }

    private fun rebuildDetails(position: Int) {
        val target = detailsContainer ?: return
        target.removeAllViews()
        val card = cards.getOrNull(position) ?: return

        addDetailRow(target, "بانک", card.visual?.persianName ?: card.bankId, null)
        addDetailRow(target, "شماره کارت", repo.formatCard(card.cardNumber), card.cardNumber)
        addDetailRow(target, "صاحب کارت", card.holderName.ifBlank { "ثبت نشده" }, null)
        if (card.iban.isNotBlank()) addDetailRow(target, "شماره شبا", repo.formatIban(card.iban), card.iban)

        val actions = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        actions.addView(MaterialButton(this).apply {
            text = "کپی کارت"
            setOnClickListener { copy(card.cardNumber) }
        }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(6) })
        actions.addView(MaterialButton(this).apply {
            text = "اشتراک‌گذاری"
            setOnClickListener { share(card) }
        }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginStart = dp(6) })
        target.addView(actions, LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(8) })

        target.addView(TextView(this).apply {
            text = "مدیریت کارت"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(themeColor(com.google.android.material.R.attr.colorPrimary))
            setPadding(0, dp(12), 0, dp(12))
            setOnClickListener { showCardActions(card) }
        })
    }

    private fun addDetailRow(parent: LinearLayout, label: String, value: String, copyValue: String?) {
        val row = MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(themeColor(com.google.android.material.R.attr.colorSurfaceVariant))
            strokeWidth = 0
        }
        val box = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16), dp(10), dp(12), dp(10))
        }
        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        texts.addView(TextView(this).apply {
            text = label
            textSize = 12f
            setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
        })
        texts.addView(TextView(this).apply {
            text = value
            textSize = 15f
            typeface = if (label == "شماره کارت" || label == "شماره شبا") Typeface.MONOSPACE else Typeface.DEFAULT
            textDirection = if (label == "شماره کارت" || label == "شماره شبا") View.TEXT_DIRECTION_LTR else View.TEXT_DIRECTION_INHERIT
            setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface))
            setPadding(0, dp(3), 0, 0)
        })
        box.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        if (copyValue != null) {
            box.addView(TextView(this).apply {
                text = "کپی"
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(themeColor(com.google.android.material.R.attr.colorPrimary))
                setOnClickListener { copy(copyValue) }
            }, LinearLayout.LayoutParams(dp(52), dp(42)))
        }
        row.addView(box)
        parent.addView(row, LinearLayout.LayoutParams(-1, dp(72)).apply { bottomMargin = dp(8) })
    }

    private inner class CardAdapter : RecyclerView.Adapter<CardAdapter.Holder>() {
        inner class Holder(val root: LinearLayout) : RecyclerView.ViewHolder(root)
        override fun getItemCount() = cards.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(LinearLayout(parent.context).apply { setPadding(dp(4), dp(4), dp(4), dp(4)) })
        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.root.removeAllViews()
            holder.root.addView(bankCardView(cards[position]), LinearLayout.LayoutParams(-1, -1))
        }
    }

    private fun bankCardView(card: BankCard): View {
        val visual = card.visual
        val accent = visual?.color ?: themeColor(com.google.android.material.R.attr.colorPrimary)
        return MaterialCardView(this).apply {
            radius = dp(24).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(accent)
            strokeWidth = 0
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(20), dp(16), dp(20), dp(16))
                background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(accent, darken(accent)))
                val header = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
                header.addView(ImageView(context).apply {
                    visual?.logoResourceName?.let {
                        resources.getIdentifier(it, "drawable", packageName).takeIf { id -> id != 0 }?.let(::setImageResource)
                    }
                    contentDescription = visual?.persianName ?: "بانک"
                    setPadding(dp(4), dp(4), dp(4), dp(4))
                }, LinearLayout.LayoutParams(dp(44), dp(44)))
                header.addView(TextView(context).apply {
                    text = visual?.persianName ?: card.bankId
                    textSize = 15f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.WHITE)
                    setPadding(dp(10), 0, 0, 0)
                }, LinearLayout.LayoutParams(0, -2, 1f))
                header.addView(TextView(context).apply {
                    text = "⋮"
                    textSize = 24f
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    setOnClickListener { showCardActions(card) }
                }, LinearLayout.LayoutParams(dp(36), dp(44)))
                addView(header)
                addView(TextView(context).apply {
                    text = repo.formatCard(card.cardNumber)
                    textSize = 21f
                    typeface = Typeface.MONOSPACE
                    letterSpacing = .06f
                    gravity = Gravity.CENTER
                    textDirection = View.TEXT_DIRECTION_LTR
                    setTextColor(Color.WHITE)
                    setPadding(0, dp(22), 0, dp(14))
                })
                val bottom = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
                bottom.addView(TextView(context).apply {
                    text = card.holderName.ifBlank { "صاحب کارت" }
                    textSize = 13f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.WHITE)
                }, LinearLayout.LayoutParams(0, -2, 1f))
                bottom.addView(TextView(context).apply {
                    text = if (card.iban.isBlank()) "کارت بانکی" else "شبا ثبت شده"
                    textSize = 11f
                    setTextColor(Color.WHITE)
                    alpha = .86f
                })
                addView(bottom)
            })
            setOnClickListener { showCardActions(card) }
        }
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
            setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface))
        })
        root.addView(TextView(this).apply {
            text = "شماره کارت را وارد کنید؛ بانک به‌صورت خودکار شناسایی می‌شود."
            textSize = 13f
            setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
            setPadding(0, dp(6), 0, dp(16))
        })

        val cardInput = TextInputEditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "6037 9918 1234 5678"
            text = existing?.let(repo::formatCard)
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
            text = existing?.holderName
        }
        root.addView(TextInputLayout(this).apply {
            hint = "نام صاحب کارت (اختیاری)"
            addView(holderInput)
        }, LinearLayout.LayoutParams(-1, dp(72)).apply { topMargin = dp(8) })

        val ibanInput = TextInputEditText(this).apply {
            hint = "IR..."
            text = existing?.let { if (it.iban.isBlank()) "" else repo.formatIban(it.iban) }
            textDirection = View.TEXT_DIRECTION_LTR
        }
        root.addView(TextInputLayout(this).apply {
            hint = "شماره شبا (اختیاری)"
            addView(ibanInput)
        }, LinearLayout.LayoutParams(-1, dp(72)).apply { topMargin = dp(8) })

        val detected = TextView(this).apply {
            textSize = 13f
            setTextColor(themeColor(com.google.android.material.R.attr.colorPrimary))
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
            setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface))
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

    private fun showSortSheet() {
        val sheet = BottomSheetDialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(20), dp(16), dp(20), dp(28))
        }
        root.addView(TextView(this).apply {
            text = "مرتب‌سازی کارت‌ها"
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface))
            setPadding(0, 0, 0, dp(12))
        })
        cards.forEachIndexed { index, card ->
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            row.addView(TextView(this).apply {
                text = (index + 1).toString() + ". " + (card.visual?.persianName ?: card.bankId)
                textSize = 15f
                setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface))
            }, LinearLayout.LayoutParams(0, dp(52), 1f))
            row.addView(MaterialButton(this).apply {
                text = "↑"
                isEnabled = index > 0
                setOnClickListener { moveCard(index, index - 1); sheet.dismiss() }
            }, LinearLayout.LayoutParams(dp(54), dp(48)))
            row.addView(MaterialButton(this).apply {
                text = "↓"
                isEnabled = index < cards.lastIndex
                setOnClickListener { moveCard(index, index + 1); sheet.dismiss() }
            }, LinearLayout.LayoutParams(dp(54), dp(48)).apply { marginStart = dp(6) })
            root.addView(row)
        }
        sheet.setContentView(root)
        sheet.show()
    }

    private fun moveCard(from: Int, to: Int) {
        if (from !in cards.indices || to !in cards.indices) return
        val card = cards.removeAt(from)
        cards.add(to, card)
        adapter.notifyDataSetChanged()
        pager.setCurrentItem(to, false)
        Thread { repo.reorder(cards.toList()) }.start()
        rebuildDetails(to)
        updatePageIndicator(to)
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
            append("\\nشماره کارت: ").append(repo.formatCard(card.cardNumber))
            if (card.holderName.isNotBlank()) append("\\nصاحب کارت: ").append(card.holderName)
            if (card.iban.isNotBlank()) append("\\nشماره شبا: ").append(repo.formatIban(card.iban))
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

    private fun darken(color: Int): Int = Color.rgb(
        (Color.red(color) * .72f).toInt(),
        (Color.green(color) * .72f).toInt(),
        (Color.blue(color) * .72f).toInt()
    )
}
