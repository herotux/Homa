package com.goodwy.smsmessenger.activities

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.graphics.Typeface
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import android.util.TypedValue
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.goodwy.commons.dialogs.ConfirmationDialog
import com.goodwy.commons.extensions.*
import com.goodwy.commons.helpers.*
import com.goodwy.commons.models.SimpleContact
import com.goodwy.smsmessenger.adapters.ContactsAdapter
import com.goodwy.smsmessenger.databinding.ActivityConversationDetailsBinding
import com.goodwy.smsmessenger.dialogs.RenameConversationDialog
import com.goodwy.smsmessenger.extensions.*
import com.goodwy.smsmessenger.helpers.*
import com.goodwy.smsmessenger.messaging.isShortCodeWithLetters
import com.goodwy.smsmessenger.models.Conversation
import androidx.core.graphics.drawable.toDrawable

class ConversationDetailsActivity : SimpleActivity() {

    private var threadId: Long = 0L
    private var conversation: Conversation? = null
    private lateinit var participants: ArrayList<SimpleContact>

    private var buttonBg = Color.WHITE

    private val binding by viewBinding(ActivityConversationDetailsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupEdgeToEdge(padBottomSystem = listOf(binding.conversationDetailsNestedScrollview))

        initButton()

        threadId = intent.getLongExtra(THREAD_ID, 0L)
        ensureBackgroundThread {
            conversation = conversationsDB.getConversationWithThreadId(threadId)
            participants = if (conversation != null && conversation!!.isScheduled) {
                val message = messagesDB.getThreadMessages(conversation!!.threadId).firstOrNull()
                message?.participants ?: arrayListOf()
            } else {
                getThreadParticipants(threadId, null)
            }
            runOnUiThread {
                setupTopViews()
                setupTextViews()
                setupParticipants()
                updateButton()
                setupCustomNotifications()
                setupSharedContent()
            }
        }
    }

    private fun initButton() {
        val primaryColor = getProperPrimaryColor()
        val typeface = FontHelper.getTypeface(this)

        var drawableSMS = AppCompatResources.getDrawable(this, com.goodwy.commons.R.drawable.ic_phone_vector)
        drawableSMS = DrawableCompat.wrap(drawableSMS!!)
        DrawableCompat.setTint(drawableSMS, primaryColor)
        DrawableCompat.setTintMode(drawableSMS, PorterDuff.Mode.SRC_IN)
        binding.oneButton.apply {
            setCompoundDrawablesWithIntrinsicBounds(null, drawableSMS, null, null)
            setTypeface(typeface)
            setTextColor(primaryColor)
        }

        var drawableVideoCall = AppCompatResources.getDrawable(this, com.goodwy.commons.R.drawable.ic_person_rounded)
        drawableVideoCall = DrawableCompat.wrap(drawableVideoCall!!)
        DrawableCompat.setTint(drawableVideoCall, primaryColor)
        DrawableCompat.setTintMode(drawableVideoCall, PorterDuff.Mode.SRC_IN)
        binding.threeButton.apply {
            setCompoundDrawablesWithIntrinsicBounds(null, drawableVideoCall, null, null)
            setTypeface(typeface)
            setTextColor(primaryColor)
        }

        var drawableMail = AppCompatResources.getDrawable(this, com.goodwy.commons.R.drawable.ic_ios_share)
        drawableMail = DrawableCompat.wrap(drawableMail!!)
        DrawableCompat.setTint(drawableMail, primaryColor)
        DrawableCompat.setTintMode(drawableMail, PorterDuff.Mode.SRC_IN)
        binding.fourButton.apply {
            setCompoundDrawablesWithIntrinsicBounds(null, drawableMail, null, null)
            setTypeface(typeface)
            setTextColor(primaryColor)
        }

        arrayOf(
            binding.conversationNumber,
            binding.conversationBirthdays
        ).forEach {
            it.setTextColor(primaryColor)
        }
    }

    override fun onResume() {
        super.onResume()
        buttonBg = if ((isLightTheme() || isGrayTheme()) && !isDynamicTheme()) Color.WHITE else getSurfaceColor()

        updateTextColors(binding.conversationDetailsHolder)

        val primaryColor = getProperPrimaryColor()
        binding.conversationNameHeading.setTextColor(primaryColor)
        binding.conversationNumber.setTextColor(primaryColor)
        binding.conversationBirthdays.setTextColor(primaryColor)
        binding.membersHeading.setTextColor(primaryColor)
        updateBackgroundColors()
        setupMenu()
    }

    private fun updateBackgroundColors() {
        if (isLightTheme() && !isDynamicTheme()) {
            val colorToWhite = getSurfaceColor()
            supportActionBar?.setBackgroundDrawable(colorToWhite.toDrawable())
            window.decorView.setBackgroundColor(colorToWhite)
            window.statusBarColor = colorToWhite
            binding.contactActionsHolder.setBackgroundColor(colorToWhite)
            binding.collapsingToolbar.setBackgroundColor(colorToWhite)
        } else {
            val properBackgroundColor = getProperBackgroundColor()
            window.decorView.setBackgroundColor(properBackgroundColor)
            binding.contactActionsHolder.setBackgroundColor(properBackgroundColor)
            binding.collapsingToolbar.setBackgroundColor(properBackgroundColor)
        }

        binding.apply {
            arrayOf(
                oneButton, threeButton, fourButton,
                membersWrapper, conversationNumberContainer, conversationBirthdaysContainer,
                customNotificationsHolder, blockButton
            ).forEach {
                it.background.setTint(buttonBg)
            }
        }
    }

    private fun setupCustomNotifications() = binding.apply {
        val textColor = getProperTextColor()
        customNotificationsButtonChevron.setColorFilter(textColor)
        notificationsIcon.setColorFilter(textColor)
        customNotifications.isChecked = config.customNotifications.contains(threadId.toString())

        if (customNotifications.isChecked) {
            customNotificationsButtonHolder.alpha = 1f
            customNotificationsButtonHolder.isEnabled = true
        } else {
            customNotificationsButtonHolder.alpha = 0.6f
            customNotificationsButtonHolder.isEnabled = false
        }

        customNotificationsWrapper.setOnClickListener {
            customNotifications.toggle()
            if (customNotifications.isChecked) {
                customNotificationsButtonHolder.alpha = 1f
                customNotificationsButtonHolder.isEnabled = true
                config.addCustomNotificationsByThreadId(threadId)
                createNotificationChannel()
            } else {
                customNotificationsButtonHolder.alpha = 0.6f
                customNotificationsButtonHolder.isEnabled = false
                config.removeCustomNotificationsByThreadId(threadId)
                removeNotificationChannel()
            }
        }

        customNotificationsButtonHolder.setOnClickListener {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, threadId.hashCode().toString())
                startActivity(this)
            }
        }
    }

    private fun createNotificationChannel() {
        val name = conversation?.title
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
            .build()

        NotificationChannel(threadId.hashCode().toString(), name, NotificationManager.IMPORTANCE_HIGH).apply {
            setBypassDnd(false)
            enableLights(true)
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                audioAttributes
            )
            enableVibration(true)
            notificationManager.createNotificationChannel(this)
        }
    }

    private fun removeNotificationChannel() {
        notificationManager.deleteNotificationChannel(threadId.hashCode().toString())
    }

    private fun setupSharedContent() {
        val holder = binding.conversationDetailsHolder
        if (holder.findViewWithTag<View>("homa_shared_content") != null) return

        val section = LinearLayout(this).apply {
            tag = "homa_shared_content"
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(18), dp(4), dp(20))
        }

        section.addView(TextView(this).apply {
            text = "محتوای به‌اشتراک‌گذاشته‌شده"
            setTextColor(getProperTextColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(8), dp(8), dp(8), dp(12))
        })

        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
        }

        val linksTab = TextView(this)
        val banksTab = TextView(this)

        fun styleTab(tab: TextView, selected: Boolean) {
            tab.setTextColor(if (selected) getProperPrimaryColor() else getProperTextColor())
            tab.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            tab.gravity = Gravity.CENTER
            tab.setPadding(dp(8), dp(12), dp(8), dp(12))
            tab.background = AppCompatResources.getDrawable(this, com.goodwy.commons.R.drawable.button_white_bg_10dp)
            tab.background?.setTint(getProperBackgroundColor())
            tab.alpha = if (selected) 1f else 0.72f
        }

        // Match URLs and bank identifiers even when SMS text uses Persian/Arabic digits.
        val linkRegex = Regex(
            """(?i)(?:https?://|www\.)[^\s<>\\"'\]\)]+"""
        )
        val bankRegex = Regex(
            """(?i)(?<![0-9])(?:IR\s?[0-9]{2}(?:[\s-]?[0-9]){22}|[0-9]{4}(?:[\s-]?[0-9]{4}){3}|[0-9]{16}|[0-9]{10,26})(?![0-9])"""
        )

        fun normalizeDigits(value: String): String = buildString(value.length) {
            value.forEach { char ->
                val digit = when (char) {
                    in '۰'..'۹' -> char.code - '۰'.code
                    in '٠'..'٩' -> char.code - '٠'.code
                    else -> -1
                }
                append(if (digit >= 0) ('0'.code + digit).toChar() else char)
            }
        }

        fun render(isBanks: Boolean) {
            linksTab.text = "لینک‌ها"
            banksTab.text = "شماره‌های بانکی"
            styleTab(linksTab, !isBanks)
            styleTab(banksTab, isBanks)
            content.removeAllViews()

            val messages = try {
                messagesDB.getThreadMessages(threadId)
            } catch (e: Exception) {
                Log.e("HomaSharedContent", "Failed to load messages for thread $threadId", e)
                emptyList()
            }

            val rows: List<Triple<com.goodwy.smsmessenger.models.Message, String, String>> = if (isBanks) {
                messages.flatMap { message ->
                    val body = normalizeDigits(message.body)
                    bankRegex.findAll(body)
                        .map { it.value.trim() }
                        .map { value -> value.replace(Regex("""[\\s-]"""), "") }
                        .filter { value ->
                            val digits = value.filter { it in '0'..'9' }
                            digits.length in 10..26 &&
                                (value.startsWith("IR", ignoreCase = true) && digits.length == 24 ||
                                    digits.length == 16 || digits.length in 10..26)
                        }
                        .map { Triple(message, it, "شماره بانکی") }
                        .toList()
                }.distinctBy { it.second.filter { c -> c.isDigit() } }
            } else {
                messages.flatMap { message ->
                    val body = message.body
                    linkRegex.findAll(body)
                        .map { match ->
                            match.value.trimEnd('.', ',', ')', ']', '،', '"', '\'')
                        }
                        .map { Triple(message, it, "لینک به‌اشتراک‌گذاشته‌شده") }
                        .toList()
                }.distinctBy { it.second }
            }

            if (rows.isEmpty()) {
                content.addView(TextView(this).apply {
                    text = if (isBanks) "شماره کارت، حساب یا شبایی در این گفتگو پیدا نشد." else "لینکی در این گفتگو پیدا نشد."
                    setTextColor(getProperTextColor())
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    setPadding(dp(12), dp(18), dp(12), dp(18))
                })
                return
            }

            rows.forEach { (message, value, _) ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(10), dp(10), dp(10), dp(10))
                    background = AppCompatResources.getDrawable(this@ConversationDetailsActivity, com.goodwy.commons.R.drawable.button_white_bg_10dp)
                    background?.setTint(getProperBackgroundColor())
                    isClickable = true
                    isFocusable = true
                }

                row.addView(ImageView(this).apply {
                    setImageResource(if (isBanks) com.goodwy.commons.R.drawable.ic_phone_vector else com.goodwy.commons.R.drawable.ic_ios_share)
                    setColorFilter(getProperPrimaryColor())
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                }, LinearLayout.LayoutParams(dp(52), dp(52)))

                val texts = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(12), 0, dp(4), 0)
                }

                texts.addView(TextView(this).apply {
                    text = if (isBanks) value else {
                        try {
                            Uri.parse(value).host?.removePrefix("www.") ?: value
                        } catch (_: Exception) {
                            value
                        }
                    }
                    setTextColor(getProperTextColor())
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    maxLines = 2
                })

                texts.addView(TextView(this).apply {
                    text = if (isBanks) "لمس طولانی: کپی شماره · لمس: رفتن به پیام" else value
                    setTextColor(getProperPrimaryColor())
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    maxLines = 2
                })

                row.addView(texts, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

                row.setOnClickListener {
                    startActivity(Intent(this, ThreadActivity::class.java).apply {
                        putExtra(THREAD_ID, threadId)
                        putExtra(SEARCHED_MESSAGE_ID, message.id)
                    })
                }
                row.setOnLongClickListener {
                    copyToClipboard(value)
                    true
                }

                content.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) })
            }
        }

        linksTab.setOnClickListener { render(false) }
        banksTab.setOnClickListener { render(true) }

        tabs.addView(linksTab, LinearLayout.LayoutParams(0, -2, 1f))
        tabs.addView(banksTab, LinearLayout.LayoutParams(0, -2, 1f))

        section.addView(tabs)
        section.addView(content)
        holder.addView(section)

        render(false)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun setupTextViews() {
        binding.conversationName.apply {
            ResourcesCompat.getDrawable(
                resources,
                com.goodwy.commons.R.drawable.ic_edit_vector, theme
            )?.apply {
                applyColorFilter(getProperTextColor())
                setCompoundDrawablesWithIntrinsicBounds(null, null, this, null)
            }

            text = conversation?.title
            setOnClickListener {
                RenameConversationDialog(
                    this@ConversationDetailsActivity,
                    conversation!!
                ) { title ->
                    text = title
                    ensureBackgroundThread {
                        conversation = renameConversation(conversation!!, newTitle = title)
                    }
                }
            }
        }
    }

    private fun setupParticipants() {
        binding.membersWrapper.beVisibleIf(participants.size > 1)
        val adapter = ContactsAdapter(this, participants, binding.participantsRecyclerview) {
            val contact = it as SimpleContact
            val address = contact.phoneNumbers.first().normalizedNumber
            getContactFromAddress(address) { simpleContact ->
                if (simpleContact != null) {
                    runOnUiThread {
                        startContactDetailsIntentRecommendation(simpleContact)
                    }
                }
            }
        }
        binding.participantsRecyclerview.adapter = adapter
    }

    private fun setupTopViews() {
        val title = conversation?.title
        val threadTitle = if (!title.isNullOrEmpty()) title else participants.getThreadTitle()

        if (conversation != null) {
            if ((threadTitle == conversation!!.phoneNumber || conversation!!.isCompany) && conversation!!.photoUri == "") {
                val drawable =
                    if (conversation!!.isCompany) SimpleContactsHelper(this).getColoredCompanyIcon(threadTitle)
                    else SimpleContactsHelper(this).getColoredContactIcon(threadTitle)
                binding.topConversationDetails.conversationDetailsImage.setImageDrawable(drawable)
            } else {
                if (!isDestroyed || !isFinishing) {
                    val placeholder = if (participants.size > 1) {
                        SimpleContactsHelper(this).getColoredGroupIcon(threadTitle)
                    } else {
                        null
                    }
                    SimpleContactsHelper(this).loadContactImage(conversation!!.photoUri, binding.topConversationDetails.conversationDetailsImage, threadTitle, placeholder)
                }
            }
        } else {
            if (!isDestroyed || !isFinishing) {
                val placeholder = if (participants.size > 1) {
                    SimpleContactsHelper(this).getColoredGroupIcon(threadTitle)
                } else {
                    null
                }
                SimpleContactsHelper(this).loadContactImage("", binding.topConversationDetails.conversationDetailsImage, threadTitle, placeholder)
            }
        }

        binding.topConversationDetails.conversationDetailsName.apply {
            text = threadTitle
            setTextColor(getProperTextColor())
            setOnClickListener {
                if (conversation != null) {
                    RenameConversationDialog(this@ConversationDetailsActivity, conversation!!) { title ->
                        text = title
                        ensureBackgroundThread {
                            conversation = renameConversation(conversation!!, newTitle = title)
                        }
                    }
                }
            }
            setOnLongClickListener {
                copyToClipboard(threadTitle)
                true
            }
        }
    }

    private fun updateButton() = binding.apply {
        val primaryColor = getProperPrimaryColor()

        oneButton.alpha = if (participants.size == 1 && !isSpecialNumber()) 1f else 0.5f
        oneButton.isClickable = participants.size == 1 && !isSpecialNumber()
        if (participants.size == 1 && !isSpecialNumber()) {
            oneButton.setOnClickListener { dialNumber() }
            oneButton.setOnLongClickListener { toast(com.goodwy.commons.R.string.call); true; }
        }

        if (participants.size == 1) {
            val contact = participants.firstOrNull()
            if (contact != null) {
                val address = contact.phoneNumbers.first().normalizedNumber

                conversationNumberContainer.beVisible()
                conversationNumberContainer.setOnClickListener {
                    copyToClipboard(address)
                }
                conversationNumber.setPhoneNumberText(address)
                conversationNumber.setTextColor(primaryColor)

                getContactFromAddress(address) { simpleContact ->
                    if (simpleContact != null) {
                        runOnUiThread {
                            threeButton.alpha = 1f
                            threeButton.isClickable = true
                            threeButton.setOnClickListener { startContactDetailsIntentRecommendation(simpleContact) }
                            threeButton.setOnLongClickListener { toast(com.goodwy.commons.R.string.contact_details); true; }
                            topConversationDetails.conversationDetailsImage.setOnClickListener { startContactDetailsIntentRecommendation(simpleContact) }

                            val phoneNumber = simpleContact.phoneNumbers.firstOrNull { it.normalizedNumber == address }
                            if (phoneNumber != null) {
                                conversationNumberTypeContainer.beVisible()
                                conversationNumberType.apply {
                                    beVisible()
                                    //text = contact.phoneNumbers.filter { it.normalizedNumber == getCurrentPhoneNumber()}.toString()
                                    val phoneNumberType = phoneNumber.type
                                    val phoneNumberLabel = phoneNumber.label
                                    text = getPhoneNumberTypeText(phoneNumberType, phoneNumberLabel)
                                }
                                conversationFavoriteIcon.apply {
                                    beVisibleIf(phoneNumber.isPrimary)
                                    applyColorFilter(getProperTextColor())
                                }
                            }

                            if (simpleContact.birthdays.firstOrNull() != null) {
                                val monthName = getDateFormatFromDateString(this@ConversationDetailsActivity, simpleContact.birthdays.first(), "yyyy-MM-dd")
                                conversationBirthdaysContainer.beVisible()
                                conversationBirthdaysPress.setOnClickListener {
                                    if (monthName != null) copyToClipboard(monthName)
                                }
                                conversationBirthdaysTitle.apply {
                                    setTextColor(getProperTextColor())
                                }
                                conversationBirthdays.apply {
                                    text = monthName
                                    setTextColor(primaryColor)
                                }
                            }
                        }
                    } else {
//                        threeButton.alpha = 0.5f
//                        threeButton.isClickable = false
                        val drawable = ResourcesCompat.getDrawable(resources, com.goodwy.commons.R.drawable.ic_add_person_vector, theme)?.apply {
                            applyColorFilter(getProperPrimaryColor())
                        }
                        root.post {
                            threeButton.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null)
                            threeButton.setText(com.goodwy.commons.R.string.add_contact)
                            topConversationDetails.conversationDetailsImage.contentDescription = getString(com.goodwy.commons.R.string.add_contact)

                            arrayOf(
                                threeButton, topConversationDetails.conversationDetailsImage
                            ).forEach {
                                it.setOnClickListener {
                                    Intent().apply {
                                        action = Intent.ACTION_INSERT_OR_EDIT
                                        type = "vnd.android.cursor.item/contact"
                                        putExtra(KEY_PHONE, address)
                                        launchActivityIntent(this)
                                    }
                                }
                            }
                            threeButton.setOnLongClickListener { toast(com.goodwy.commons.R.string.add_contact); true; }
                        }
                    }
                }
            } else {
                threeButton.alpha = 0.5f
                threeButton.isClickable = false
            }
        } else {
            threeButton.alpha = 0.5f
            threeButton.isClickable = false
        }

        fourButton.apply {
            setOnClickListener {
                launchShare()
            }
            setOnLongClickListener { toast(com.goodwy.commons.R.string.share); true; }
        }

        val red = resources.getColor(com.goodwy.commons.R.color.red_missed, theme)
        val isBlockNumbers = isBlockNumbers()
        val blockColor = if (isBlockNumbers) { primaryColor } else { red }
        blockButton.apply {
            setTypeface(FontHelper.getTypeface(this@ConversationDetailsActivity))
            setTextColor(blockColor)
            val unblockText = if (participants.size == 1) com.goodwy.strings.R.string.unblock_number else com.goodwy.strings.R.string.unblock_numbers
            val blockText = if (participants.size == 1) com.goodwy.commons.R.string.block_number else com.goodwy.commons.R.string.block_numbers
            blockButton.text = if (isBlockNumbers) { resources.getString(unblockText) } else { resources.getString(blockText)}
            blockButton.setOnClickListener {
                blockNumber()
            }
        }
    }

    private fun launchShare() {
        val numbers = participants.getAddresses()
        val numbersString = TextUtils.join(", ", numbers)
        Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, numbersString)
            putExtra(Intent.EXTRA_TEXT, numbersString)
            type = "text/plain"
            startActivity(Intent.createChooser(this, getString(com.goodwy.commons.R.string.invite_via)))
        }
    }

    private fun isBlockNumbers(): Boolean {
        return participants.getAddresses().any { isNumberBlocked(it, getBlockedNumbers()) }
    }

    private fun blockNumber() {
        val numbers = participants.getAddresses()
        val numbersString = TextUtils.join(", ", numbers)
        val isBlockNumbers = isBlockNumbers()
        val baseString = if (isBlockNumbers) com.goodwy.strings.R.string.unblock_confirmation else com.goodwy.commons.R.string.block_confirmation
        val question = String.format(resources.getString(baseString), numbersString)

        ConfirmationDialog(this, question) {
            ensureBackgroundThread {
                numbers.forEach {
                    if (isBlockNumbers) {
                        deleteBlockedNumber(it)
                        val blockText = if (participants.size == 1) com.goodwy.commons.R.string.block_number else com.goodwy.commons.R.string.block_numbers
                        binding.blockButton.text = getString(blockText)
                        binding.blockButton.setTextColor(resources.getColor(com.goodwy.commons.R.color.red_missed, theme))
                    } else {
                        addBlockedNumber(it)
                        val unblockText = if (participants.size == 1) com.goodwy.strings.R.string.unblock_number else com.goodwy.strings.R.string.unblock_numbers
                        binding.blockButton.text = getString(unblockText)
                        binding.blockButton.setTextColor(getProperPrimaryColor())
                    }
                }
                refreshMessages()
//                startActivity(Intent(this, MainActivity::class.java))
//                finish()
            }
        }
    }

    private fun setupMenu() {
        val contrastColor = getProperBackgroundColor().getContrastColor()
        val itemColor = if (baseConfig.topAppBarColorIcon) getProperPrimaryColor() else contrastColor
        binding.conversationDetailsToolbar.setNavigationIconTint(itemColor)
        binding.conversationDetailsToolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun isSpecialNumber(): Boolean {
        val addresses = participants.getAddresses()
        return addresses.any { isShortCodeWithLetters(it) }
    }

    private fun dialNumber() {
        val phoneNumber = participants.first().phoneNumbers.first().normalizedNumber
        dialNumber(phoneNumber)
    }
}
