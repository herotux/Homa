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
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.goodwy.smsmessenger.features.bankcards.BankCard
import com.goodwy.smsmessenger.features.bankcards.BankCardsRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class BankCardsActivity : AppCompatActivity() {
    private lateinit var repo: BankCardsRepository
    private lateinit var pager: ViewPager2
    private lateinit var adapter: CardAdapter
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
            setBackgroundColor(color(com.google.android.material.R.attr.colorSurface, Color.WHITE))
            setPadding(dp(16), dp(8), dp(16), dp(16))
        }
        val toolbar = MaterialToolbar(this).apply {
            title = "کارت‌های بانکی"
            setBackgroundColor(color(com.google.android.material.R.attr.colorSurface, Color.WHITE))
            setTitleTextColor(color(com.google.android.material.R.attr.colorOnSurface, Color.BLACK))
            navigationIcon = getDrawable(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            setNavigationOnClickListener { finish() }
            menu.add("⋮").setShowAsAction(2)
            setOnMenuItemClickListener { showMenu(); true }
        }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(64)))
        pager = ViewPager2(this).apply { clipToPadding = false; setPadding(dp(8), dp(12), dp(8), dp(12)); offscreenPageLimit = 2 }
        adapter = CardAdapter(); pager.adapter = adapter
        root.addView(pager, LinearLayout.LayoutParams(-1, dp(300)))
        root.addView(TextView(this).apply {
            text = "برای دیدن کارت‌های دیگر، صفحه را بکشید"; gravity = Gravity.CENTER; textSize = 12f
            setTextColor(color(com.google.android.material.R.attr.colorOnSurfaceVariant, Color.DKGRAY))
        })
        root.addView(MaterialButton(this).apply {
            text = "افزودن کارت جدید"; icon = getDrawable(android.R.drawable.ic_input_add)
            setOnClickListener { showWizard(null) }
        }, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(8) })
        setContentView(root)
    }

    private fun load() {
        Thread { cards = repo.getCards().toMutableList(); runOnUiThread { pager.post { adapter.notifyDataSetChanged() } } }.start()
    }

    private inner class CardAdapter : RecyclerView.Adapter<CardAdapter.Holder>() {
        inner class Holder(val box: LinearLayout) : RecyclerView.ViewHolder(box)
        override fun getItemCount() = cards.size
        override fun onCreateViewHolder(p: android.view.ViewGroup, t: Int) = Holder(LinearLayout(p.context).apply { setPadding(dp(4), dp(4), dp(4), dp(4)) })
        override fun onBindViewHolder(h: Holder, position: Int) {
            h.box.removeAllViews()
            if (position < cards.size) h.box.addView(cardView(cards[position]), LinearLayout.LayoutParams(-1, -1))
        }
    }

    private fun cardView(card: BankCard): View {
        val visual = card.visual
        val start = visual?.color ?: Color.rgb(58, 72, 90)
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(18), dp(22), dp(14))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(start, darken(start))).apply { cornerRadius = dp(26).toFloat() }
            elevation = dp(7).toFloat()
            setOnClickListener { showActions(card) }
            val top = LinearLayout(this@BankCardsActivity).apply { gravity = Gravity.CENTER_VERTICAL }
            top.addView(ImageView(this@BankCardsActivity).apply {
                visual?.logoResourceName?.let { resources.getIdentifier(it, "drawable", packageName).takeIf { id -> id != 0 }?.let(::setImageResource) }
                contentDescription = visual?.persianName.orEmpty()
            }, LinearLayout.LayoutParams(dp(42), dp(42)))
            top.addView(TextView(this@BankCardsActivity).apply {
                text = visual?.persianName ?: card.bankId; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE)
                setPadding(dp(10), 0, 0, 0)
            }, LinearLayout.LayoutParams(0, -2, 1f))
            top.addView(TextView(this@BankCardsActivity).apply {
                text = "⋮"; textSize = 28f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
                setOnClickListener { showActions(card) }
            }, LinearLayout.LayoutParams(dp(42), dp(44)))
            addView(top)
            addView(TextView(this@BankCardsActivity).apply {
                text = repo.formatCard(card.cardNumber); textSize = 21f; typeface = Typeface.MONOSPACE; letterSpacing = .06f
                gravity = Gravity.CENTER; textDirection = View.TEXT_DIRECTION_LTR; setTextColor(Color.WHITE); setPadding(0, dp(24), 0, dp(12))
            })
            addView(TextView(this@BankCardsActivity).apply {
                text = card.holderName.ifBlank { "نام صاحب کارت" }; textSize = 13f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE)
            })
            if (card.iban.isNotBlank()) addView(TextView(this@BankCardsActivity).apply {
                text = repo.formatIban(card.iban); textSize = 11f; gravity = Gravity.CENTER; textDirection = View.TEXT_DIRECTION_LTR
                setTextColor(Color.WHITE); setPadding(0, dp(6), 0, 0)
            })
        }
    }

    private fun showMenu() {
        val sheet = BottomSheetDialog(this); val root = sheetRoot("مدیریت کارت‌ها")
        item(root, sheet, "افزودن کارت جدید") { showWizard(null) }
        item(root, sheet, "مرتب‌سازی دلخواه") { sort() }
        sheet.setContentView(root); sheet.show()
    }

    private fun sort() {
        val sheet = BottomSheetDialog(this); val root = sheetRoot("مرتب‌سازی کارت‌ها")
        cards.forEachIndexed { i, c ->
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            row.addView(TextView(this).apply { text = c.visual?.persianName ?: c.bankId; textSize = 15f }, LinearLayout.LayoutParams(0, dp(52), 1f))
            row.addView(MaterialButton(this).apply { text = "↑"; isEnabled = i > 0; setOnClickListener { move(i, i - 1); sheet.dismiss(); sort() } }, LinearLayout.LayoutParams(dp(54), dp(52)))
            row.addView(MaterialButton(this).apply { text = "↓"; isEnabled = i < cards.lastIndex; setOnClickListener { move(i, i + 1); sheet.dismiss(); sort() } }, LinearLayout.LayoutParams(dp(54), dp(52)).apply { marginStart = dp(6) })
            root.addView(row)
        }
        sheet.setContentView(root); sheet.show()
    }

    private fun move(from: Int, to: Int) {
        if (from !in cards.indices || to !in cards.indices) return
        val c = cards.removeAt(from); cards.add(to, c); adapter.notifyDataSetChanged(); Thread { repo.reorder(cards) }.start()
    }

    private fun showActions(card: BankCard) {
        val sheet = BottomSheetDialog(this); val root = sheetRoot(card.visual?.persianName ?: "کارت بانکی")
        item(root, sheet, "ویرایش") { showWizard(card) }
        item(root, sheet, "کپی شماره کارت") { copy(card.cardNumber) }
        if (card.iban.isNotBlank()) item(root, sheet, "کپی شماره شبا") { copy(card.iban) }
        item(root, sheet, "اشتراک‌گذاری") { share(card) }
        item(root, sheet, "حذف کارت") {
            MaterialAlertDialogBuilder(this).setTitle("حذف کارت").setMessage("این کارت حذف شود؟").setNegativeButton("انصراف", null)
                .setPositiveButton("حذف") { _, _ -> Thread { repo.delete(card); runOnUiThread { load() } }.start() }.show()
        }
        sheet.setContentView(root); sheet.show()
    }

    private fun showWizard(existing: BankCard?) = CardWizard(existing).show()

    private inner class CardWizard(private val existing: BankCard?) {
        private val dialog = BottomSheetDialog(this@BankCardsActivity)
        private val root = LinearLayout(this@BankCardsActivity).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(20), dp(12), dp(20), dp(28)) }
        private val groups = Array(4) { EditText(this@BankCardsActivity).apply { inputType = InputType.TYPE_CLASS_NUMBER; gravity = Gravity.CENTER; textSize = 17f; hint = "••••" } }
        private val holder = TextInputEditText(this@BankCardsActivity)
        private val iban = TextInputEditText(this@BankCardsActivity)
        private var preview: LinearLayout? = null

        fun show() { buildCardStep(); dialog.setContentView(root); dialog.show(); if (existing == null) paste() else setCard(existing.cardNumber) }

        private fun header(title: String) {
            root.removeAllViews()
            root.addView(TextView(this@BankCardsActivity).apply { text = title; textSize = 20f; typeface = Typeface.DEFAULT_BOLD })
        }

        private fun addPreview() {
            preview = LinearLayout(this@BankCardsActivity).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(20), dp(16), dp(20), dp(14)) }
            root.addView(preview, LinearLayout.LayoutParams(-1, dp(190)).apply { topMargin = dp(12) })
        }

        private fun buildCardStep() {
            header("افزودن کارت · ۱ از ۳"); addPreview()
            val row = LinearLayout(this@BankCardsActivity).apply { gravity = Gravity.CENTER; layoutDirection = View.LAYOUT_DIRECTION_LTR }
            groups.forEach { row.addView(it, LinearLayout.LayoutParams(0, dp(56), 1f).apply { marginEnd = dp(6) }) }
            root.addView(row)
            root.addView(MaterialButton(this@BankCardsActivity).apply { text = "ادامه"; setOnClickListener { if (valid()) holderStep() } }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(14) })
            groups.forEach { it.addTextChangedListenerCompat { updatePreview() } }; updatePreview()
        }

        private fun holderStep() {
            header("افزودن کارت · ۲ از ۳"); addPreview()
            root.addView(TextInputLayout(this@BankCardsActivity).apply { hint = "نام صاحب کارت (اختیاری)"; addView(holder) }, LinearLayout.LayoutParams(-1, dp(72)).apply { topMargin = dp(14) })
            root.addView(MaterialButton(this@BankCardsActivity).apply { text = "ادامه"; setOnClickListener { ibanStep() } }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(14) }); updatePreview()
        }

        private fun ibanStep() {
            header("افزودن کارت · ۳ از ۳"); addPreview()
            root.addView(TextInputLayout(this@BankCardsActivity).apply { hint = "شماره شبا (اختیاری)"; addView(iban) }, LinearLayout.LayoutParams(-1, dp(72)).apply { topMargin = dp(14) })
            root.addView(MaterialButton(this@BankCardsActivity).apply { text = "ذخیره کارت"; setOnClickListener { save() } }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(14) }); updatePreview()
        }

        private fun valid(): Boolean {
            val c = groups.joinToString("") { repo.normalizeCard(it.text.toString()) }
            val ok = c.length == 16 && repo.validCard(c) && repo.detect(c) != null
            if (!ok) Toast.makeText(this@BankCardsActivity, "شماره کارت معتبر نیست یا بانک شناسایی نشد", Toast.LENGTH_SHORT).show()
            return ok
        }

        private fun save() {
            val c = groups.joinToString("") { repo.normalizeCard(it.text.toString()) }; val i = repo.normalizeIban(iban.text?.toString().orEmpty())
            if (!repo.validCard(c) || repo.detect(c) == null || (i.isNotBlank() && !repo.validIban(i))) { Toast.makeText(this@BankCardsActivity, "اطلاعات کارت معتبر نیست", Toast.LENGTH_SHORT).show(); return }
            Thread {
                val result = repo.save(existing, c, holder.text?.toString().orEmpty(), i)
                runOnUiThread { if (result.isSuccess) { dialog.dismiss(); load() } else Toast.makeText(this@BankCardsActivity, "ذخیره کارت انجام نشد", Toast.LENGTH_SHORT).show() }
            }.start()
        }

        private fun paste() {
            val value = repo.normalizeCard((getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip?.getItemAt(0)?.coerceToText(this@BankCardsActivity)?.toString().orEmpty())
            if (value.length == 16 && repo.detect(value) != null) setCard(value)
        }

        private fun setCard(value: String) { value.chunked(4).take(4).forEachIndexed { i, part -> groups[i].setText(part) }; updatePreview() }

        private fun updatePreview() {
            val target = preview ?: return; target.removeAllViews()
            val c = groups.joinToString("") { repo.normalizeCard(it.text.toString()) }; val v = repo.detect(c); val start = v?.color ?: Color.rgb(58,72,90)
            target.background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(start, darken(start))).apply { cornerRadius = dp(22).toFloat() }
            target.addView(TextView(this@BankCardsActivity).apply { text = v?.persianName ?: "بانک"; textSize = 14f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
            target.addView(TextView(this@BankCardsActivity).apply { text = repo.formatCard(c).ifBlank { "••••   ••••   ••••   ••••" }; textSize = 20f; typeface = Typeface.MONOSPACE; gravity = Gravity.CENTER; textDirection = View.TEXT_DIRECTION_LTR; setTextColor(Color.WHITE); setPadding(0, dp(34), 0, 0) })
        }
    }

    private fun copy(value: String) { (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Homa", value)); Toast.makeText(this, "کپی شد", Toast.LENGTH_SHORT).show() }
    private fun share(card: BankCard) { startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, repo.formatCard(card.cardNumber)) }, "اشتراک‌گذاری")) }
    private fun item(root: LinearLayout, sheet: BottomSheetDialog, text: String, action: () -> Unit) { root.addView(MaterialButton(this).apply { this.text = text; gravity = Gravity.START or Gravity.CENTER_VERTICAL; setOnClickListener { sheet.dismiss(); action() } }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(8) }) }
    private fun sheetRoot(title: String) = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(20),dp(16),dp(20),dp(28)); addView(TextView(this@BankCardsActivity).apply { text=title; textSize=20f; typeface=Typeface.DEFAULT_BOLD }) }
    private fun EditText.addTextChangedListenerCompat(onChange: () -> Unit) { addTextChangedListener(object: android.text.TextWatcher { override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int)=Unit; override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int)=onChange(); override fun afterTextChanged(s:android.text.Editable?)=Unit }) }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun color(attr:Int,fallback:Int):Int { val tv=android.util.TypedValue(); return if(theme.resolveAttribute(attr,tv,true)){if(tv.resourceId!=0)runCatching{getColor(tv.resourceId)}.getOrDefault(fallback) else tv.data}else fallback }
    private fun darken(c:Int)=Color.rgb((Color.red(c)*.72f).toInt(),(Color.green(c)*.72f).toInt(),(Color.blue(c)*.72f).toInt())
}
