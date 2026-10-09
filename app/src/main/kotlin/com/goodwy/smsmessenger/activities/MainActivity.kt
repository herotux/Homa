package com.goodwy.smsmessenger.activities

import com.goodwy.smsmessenger.activities.SearchActivity

import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.os.Bundle
import android.provider.Telephony
import android.speech.RecognizerIntent
import android.text.TextUtils
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.goodwy.commons.dialogs.ConfirmationAdvancedDialog
import com.goodwy.commons.dialogs.ConfirmationDialog
import com.goodwy.commons.dialogs.PermissionRequiredDialog
import com.goodwy.commons.extensions.*
import com.goodwy.commons.helpers.*
import com.google.android.material.appbar.AppBarLayout
import com.goodwy.smsmessenger.BuildConfig
import com.goodwy.smsmessenger.R
import com.goodwy.smsmessenger.adapters.ConversationsAdapter
import com.goodwy.smsmessenger.adapters.SearchResultsAdapter
import com.goodwy.smsmessenger.databinding.ActivityMainBinding
import com.goodwy.smsmessenger.extensions.*
import com.goodwy.smsmessenger.helpers.SEARCHED_MESSAGE_ID
import com.goodwy.smsmessenger.helpers.THREAD_ID
import com.goodwy.smsmessenger.helpers.THREAD_TITLE
import com.goodwy.smsmessenger.helpers.whatsNewList
import com.goodwy.smsmessenger.helpers.HomaDiagnostics
import com.goodwy.smsmessenger.models.Conversation
import com.goodwy.smsmessenger.models.Events
import com.goodwy.smsmessenger.models.Message
import com.goodwy.smsmessenger.models.SearchResult
import com.goodwy.smsmessenger.views.ThinkingOrbView
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

class MainActivity : SimpleActivity() {
    override var isSearchBarEnabled = true

    private val MAKE_DEFAULT_APP_REQUEST = 1

    private var storedPrimaryColor = 0
    private var storedTextColor = 0
    private var storedBackgroundColor = 0
    private var storedFontSize = 0
    private var storedEllipsizeMode = ELLIPSIZE_MODE_END
    private var lastSearchedText = ""
    private var bus: EventBus? = null
    private var isSpeechToTextAvailable = false
    private var conversationLoadToken = 0L
    private var scrollListenersAttached = false
    private var messengerInitialized = false
    private var providerRefreshInFlight = false
    private var initialMessageLoadingOverlay: FrameLayout? = null
    private var initialMessageLoadingText: TextView? = null
    private var initialMessageLoadingOrb: ThinkingOrbView? = null
    private var initialMessageLoadingTotal = 0
    private var initialMessageLoadingLoaded = 0

    private val binding by viewBinding(ActivityMainBinding::inflate)

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        appLaunched(BuildConfig.APPLICATION_ID)
        setupOptionsMenu()
        refreshMenuItems()

        binding.mainMenu.updateTitle(getString(R.string.messages))
        binding.mainMenu.searchBeVisibleIf(config.showSearchBar)
        setupEdgeToEdge(padBottomImeAndSystem = listOf(binding.conversationsList, binding.searchResultsList))

        if (config.changeColourTopBar) {
            val useSurfaceColor = isDynamicTheme() && !isSystemInDarkMode()
            setupSearchMenuScrollListener(
                scrollingView = binding.conversationsList,
                searchMenu = binding.mainMenu,
                surfaceColor = useSurfaceColor
            )
        }

        if (config.wasReminderWarningShown) checkWhatsNewDialog()
        storeStateVariables()
        attachScrollListenersOnce()
        HomaDiagnostics.log("MAIN_ON_CREATE", "activity=" + System.identityHashCode(this))

        checkAndDeleteOldRecycleBinMessages()
        clearAllMessagesIfNeeded {
            loadMessages()
        }
    }

    @SuppressLint("UnsafeIntentLaunch")
    override fun onResume() {
        super.onResume()

        if (config.needRestart || storedBackgroundColor != getProperBackgroundColor()) {
            finish()
            startActivity(intent)
            return
        }

        updateMenuColors()
        refreshMenuItems()

        getOrCreateConversationsAdapter().apply {
            if (storedPrimaryColor != getProperPrimaryColor()) {
                updatePrimaryColor()
            }

            if (storedTextColor != getProperTextColor()) {
                updateTextColor(getProperTextColor())
            }

            if (storedBackgroundColor != getProperBackgroundColor()) {
                updateBackgroundColor(getProperBackgroundColor())
            }

            if (storedFontSize != config.fontSize) {
                updateFontSize()
            }

            if (storedEllipsizeMode != config.ellipsizeMode) {
                updateEllipsizeMode()
            }

            updateDrafts()
        }

        updateTextColors(binding.mainCoordinator)
        binding.searchHolder.setBackgroundColor(getProperBackgroundColor())

        val properPrimaryColor = getProperPrimaryColor()
        binding.noConversationsPlaceholder2.setTextColor(properPrimaryColor)
        binding.noConversationsPlaceholder2.underlineText()
        binding.conversationsFastscroller.updateColors(getProperAccentColor())
        binding.conversationsProgressBar.setIndicatorColor(properPrimaryColor)
        binding.conversationsProgressBar.trackColor = properPrimaryColor.adjustAlpha(LOWER_ALPHA)
        checkShortcut()

        // Top bar scroll
        val params = binding.mainMenu.layoutParams as AppBarLayout.LayoutParams
        params.scrollFlags = if (config.hideTopBarWhenScroll) {
            AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
        } else 0
        binding.mainMenu.layoutParams = params

        if (config.wasReminderWarningShown) {
            checkErrorDialog()
        }
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }

    override fun onDestroy() {
        HomaDiagnostics.log("MAIN_ON_DESTROY", "activity=" + System.identityHashCode(this))
        super.onDestroy()
        config.needRestart = false
        bus?.unregister(this)
    }

    private fun attachScrollListenersOnce() {
        if (scrollListenersAttached) return
        scrollListenersAttached = true
        binding.conversationsList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING || newState == RecyclerView.SCROLL_STATE_SETTLING) hideKeyboard()
            }
        })
        binding.searchResultsList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING || newState == RecyclerView.SCROLL_STATE_SETTLING) hideKeyboard()
            }
        })
    }

    override fun onBackPressedCompat(): Boolean {
        return if (binding.mainMenu.isSearchOpen) {
            binding.mainMenu.closeSearch()
            true
        } else {
            appLockManager.lock()
            false
        }
    }

    private fun setupOptionsMenu() {
        binding.apply {
            mainMenu.requireToolbar().inflateMenu(R.menu.menu_main)
//            mainMenu.toggleHideOnScroll(config.hideTopBarWhenScroll)

            if (baseConfig.useSpeechToText) {
                isSpeechToTextAvailable = isSpeechToTextAvailable()
                mainMenu.showSpeechToText = isSpeechToTextAvailable
            }
            mainMenu.setupMenu()

            mainMenu.onSpeechToTextClickListener = {
                speechToText()
            }

            mainMenu.onSearchOpenListener = {
                startActivity(Intent(this@MainActivity, SearchActivity::class.java))
                mainMenu.closeSearch()
            }

            mainMenu.onSearchClosedListener = {
                fadeOutSearch()
            }

            mainMenu.onSearchTextChangedListener = { text ->
                if (text.isNotEmpty()) {
                    if (binding.searchHolder.alpha < 1f) {
                        binding.searchHolder.fadeIn()
                    }
                } else {
                    fadeOutSearch()
                }
                searchTextChanged(text)
                mainMenu.clearSearch()
            }

            mainMenu.requireToolbar().setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.show_recycle_bin -> launchRecycleBin()
                    R.id.show_archived -> launchArchivedConversations()
                    R.id.show_blocked_numbers -> showBlockedNumbers()
                    R.id.settings -> launchSettings()
                    R.id.about -> launchAbout()
                    else -> return@setOnMenuItemClickListener false
                }
                return@setOnMenuItemClickListener true
            }

            mainMenu.clearSearch()
        }
    }

    private fun refreshMenuItems() {
        binding.mainMenu.requireToolbar().menu.apply {
            findItem(R.id.show_recycle_bin).isVisible = config.useRecycleBin
            findItem(R.id.show_archived).isVisible = config.isArchiveAvailable
            findItem(R.id.show_blocked_numbers).title =
                if (config.showBlockedNumbers) getString(com.goodwy.strings.R.string.hide_blocked_numbers)
                else getString(com.goodwy.strings.R.string.show_blocked_numbers)
        }
    }

    private fun showBlockedNumbers() {
        config.showBlockedNumbers = !config.showBlockedNumbers
        binding.mainMenu.requireToolbar().menu.findItem(R.id.show_blocked_numbers).title =
            if (config.showBlockedNumbers) getString(com.goodwy.strings.R.string.hide_blocked_numbers)
            else getString(com.goodwy.strings.R.string.show_blocked_numbers)
//        runOnUiThread {
//            getRecentsFragment()?.refreshItems()
//        }
        initMessenger()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == MAKE_DEFAULT_APP_REQUEST) {
            if (resultCode == RESULT_OK) {
                askPermissions()
            } else {
                finish()
            }
        } else if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK) {
            if (resultData != null) {
                val res: ArrayList<String> =
                    resultData.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) as ArrayList<String>

                val speechToText =  Objects.requireNonNull(res)[0]
                if (speechToText.isNotEmpty()) {
                    binding.mainMenu.setText(speechToText)
                }
            }
        }
    }

    private fun storeStateVariables() {
        storedPrimaryColor = getProperPrimaryColor()
        storedTextColor = getProperTextColor()
        storedBackgroundColor = getProperBackgroundColor()
        storedFontSize = config.fontSize
        storedEllipsizeMode = config.ellipsizeMode
        config.needRestart = false
    }

    private fun updateMenuColors() {
        val useSurfaceColor = isDynamicTheme() && !isSystemInDarkMode()
        val backgroundColor = if (useSurfaceColor) getSurfaceColor() else getProperBackgroundColor()
        val statusBarColor = if (config.changeColourTopBar) getRequiredStatusBarColor(useSurfaceColor) else backgroundColor
        binding.mainMenu.updateColors(statusBarColor, scrollingView?.computeVerticalScrollOffset() ?: 0)
    }

    private fun loadMessages() {
        if (!config.wasReminderWarningShown) {
            ConfirmationAdvancedDialog(
                activity = this,
                messageId = R.string.warning_disclosure,
                fromHtml = true,
                positive = com.goodwy.strings.R.string.agree,
                negative = com.goodwy.strings.R.string.disagree
            ) {
                if (it) {
                    config.wasReminderWarningShown = true

                    if (isQPlus()) {
                        val roleManager = getSystemService(RoleManager::class.java)
                        if (roleManager!!.isRoleAvailable(RoleManager.ROLE_SMS)) {
                            if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                                askPermissions()
                            } else {
                                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                                startActivityForResult(intent, MAKE_DEFAULT_APP_REQUEST)
                            }
                        } else {
                            toast(com.goodwy.commons.R.string.unknown_error_occurred)
                            finish()
                        }
                    } else {
                        if (Telephony.Sms.getDefaultSmsPackage(this) == packageName) {
                            askPermissions()
                        } else {
                            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                            startActivityForResult(intent, MAKE_DEFAULT_APP_REQUEST)
                        }
                    }
                } else {
                    finish()
                }
            }
        } else {
            if (isQPlus()) {
                val roleManager = getSystemService(RoleManager::class.java)
                if (roleManager!!.isRoleAvailable(RoleManager.ROLE_SMS)) {
                    if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                        askPermissions()
                    } else {
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                        startActivityForResult(intent, MAKE_DEFAULT_APP_REQUEST)
                    }
                } else {
                    toast(com.goodwy.commons.R.string.unknown_error_occurred)
                    finish()
                }
            } else {
                if (Telephony.Sms.getDefaultSmsPackage(this) == packageName) {
                    askPermissions()
                } else {
                    val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                    intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                    startActivityForResult(intent, MAKE_DEFAULT_APP_REQUEST)
                }
            }
        }
    }

    // while SEND_SMS and READ_SMS permissions are mandatory, READ_CONTACTS is optional.
    // If we don't have it, we just won't be able to show the contact name in some cases
    private fun askPermissions() {
        handlePermission(PERMISSION_READ_SMS) {
            if (it) {
                handlePermission(PERMISSION_SEND_SMS) {
                    if (it) {
                        handlePermission(PERMISSION_READ_CONTACTS) {
                            handleNotificationPermission { granted ->
                                if (!granted) {
                                    PermissionRequiredDialog(
                                        activity = this,
                                        textId = com.goodwy.commons.R.string.allow_notifications_incoming_messages,
                                        positiveActionCallback = { openNotificationSettings() })
                                }
                            }

                            initMessenger()
                            bus = EventBus.getDefault()
                            try {
                                bus!!.register(this)
                            } catch (_: Exception) {
                            }
                        }
                    } else {
                        finish()
                    }
                }
            } else {
                finish()
            }
        }
    }

    private fun initMessenger() {
        if (messengerInitialized) return
        messengerInitialized = true
        getCachedConversations()
        binding.noConversationsPlaceholder2.setOnClickListener {
            launchNewConversation()
        }

        binding.conversationsFab.setOnClickListener {
            launchNewConversation()
        }
    }

    private fun getCachedConversations() {
        val token = ++conversationLoadToken
        HomaDiagnostics.log("MAIN_LOAD_START", "token=" + token)
        ensureBackgroundThread {
            val started = System.nanoTime()
            val conversations = try {
                HomaDiagnostics.timed("DB_CONVERSATIONS_CACHE") {
                    conversationsDB.getNonArchived().toMutableList() as ArrayList<Conversation>
                }
            } catch (e: Exception) {
                HomaDiagnostics.error("DB_CONVERSATIONS_CACHE_FAILED", e)
                ArrayList()
            }
            val archived = try {
                HomaDiagnostics.timed("DB_ARCHIVED_CACHE") { conversationsDB.getAllArchived() }
            } catch (e: Exception) {
                HomaDiagnostics.error("DB_ARCHIVED_CACHE_FAILED", e)
                emptyList()
            }
            HomaDiagnostics.log("MAIN_CACHE_READY", "token=" + token + " conversations=" + conversations.size + " archived=" + archived.size + " totalMs=" + ((System.nanoTime() - started) / 1_000_000))
            runOnUiThread {
                if (token != conversationLoadToken || isFinishing || isDestroyed) return@runOnUiThread
                setupConversations(conversations, cached = true)
                getNewConversations((conversations + archived).toMutableList() as ArrayList<Conversation>, token)
            }
            conversations.forEach { clearExpiredScheduledMessages(it.threadId) }
        }
    }

    private fun getNewConversations(cachedConversations: ArrayList<Conversation>, token: Long) {
        if (providerRefreshInFlight) {
            HomaDiagnostics.log("MAIN_PROVIDER_REFRESH_SKIPPED", "reason=in_flight token=" + token)
            return
        }
        providerRefreshInFlight = true
        val privateCursor = getMyContactsCursor(favoritesOnly = false, withPhoneNumbersOnly = true)
        ensureBackgroundThread {
            val started = System.nanoTime()
            var initialMessageLoaderShown = false
            try {
                // Show the initial import loader before the expensive provider
                // contacts/conversations work starts.
                val localMessageCount = runCatching { messagesDB.getCount() }.getOrDefault(0)
                val needsInitialMessageImport = config.appRunCount == 1 || localMessageCount == 0

                if (needsInitialMessageImport) {
                    val total = getProviderMessageCount()
                    if (total > 0) {
                        initialMessageLoadingTotal = total
                        initialMessageLoadingLoaded = 0
                        initialMessageLoaderShown = true
                        runOnUiThread { showInitialMessageLoadingIndicator() }
                    }
                }

                val privateContacts = MyContactsContentProvider.getSimpleContacts(this, privateCursor)
                HomaDiagnostics.log("MAIN_CONTACTS_READY", "token=" + token + " contacts=" + privateContacts.size)
                val conversations = getConversations(
                    privateContacts = privateContacts,
                    onConversationLoaded = { conversation ->
                        if (initialMessageLoaderShown) {
                            // Import each thread as soon as the Android provider exposes it.
                            // This couples the visible progress to the same pass that builds
                            // the conversation list instead of waiting for that pass to finish.
                            val loaded = getMessages(
                                conversation.threadId,
                                includeScheduledMessages = false,
                                limit = Int.MAX_VALUE,
                                onMessageLoaded = {
                                    initialMessageLoadingLoaded++
                                    if (initialMessageLoadingLoaded % 10 == 0) {
                                        updateInitialMessageLoadingProgress(initialMessageLoadingLoaded)
                                    }
                                }
                            )
                            loaded.chunked(30).forEach { batch ->
                                messagesDB.insertMessages(*batch.toTypedArray())
                            }
                        }
                    }
                )
                HomaDiagnostics.log("MAIN_PROVIDER_READY", "token=" + token + " conversations=" + conversations.size + " durationMs=" + ((System.nanoTime() - started) / 1_000_000))
                conversations.forEach { cloned ->
                    if (cachedConversations.none { it.threadId == cloned.threadId }) {
                        conversationsDB.insertOrUpdate(cloned)
                        cachedConversations.add(cloned)
                    }
                }
                cachedConversations.forEach { cached ->
                    val threadId = cached.threadId
                    val temporary = cached.isScheduled
                    val deleted = conversations.none { it.threadId == threadId }
                    if (deleted && !temporary) conversationsDB.deleteThreadId(threadId)
                    val replacement = conversations.find { it.phoneNumber == cached.phoneNumber }
                    if (temporary && replacement != null) {
                        conversationsDB.deleteThreadId(threadId)
                        messagesDB.getScheduledThreadMessages(threadId).forEach { message ->
                            messagesDB.insertOrUpdate(message.copy(threadId = replacement.threadId))
                        }
                        insertOrUpdateConversation(replacement, cached)
                    }
                }
                cachedConversations.forEach { cached ->
                    val fresh = conversations.find {
                        it.threadId == cached.threadId && !Conversation.areContentsTheSame(cached, it)
                    }
                    if (fresh != null) insertOrUpdateConversation(fresh)
                }
                val allConversations = conversationsDB.getNonArchived() as ArrayList<Conversation>
                runOnUiThread {
                    if (token != conversationLoadToken || isFinishing || isDestroyed) return@runOnUiThread
                    HomaDiagnostics.log("MAIN_UI_REFRESH", "token=" + token + " conversations=" + allConversations.size)
                    setupConversations(allConversations)
                }
                if (initialMessageLoaderShown) {
                    updateInitialMessageLoadingProgress(initialMessageLoadingLoaded)
                }
            } catch (e: Exception) {
                HomaDiagnostics.error("MAIN_REFRESH_FAILED", e)
            } finally {
                providerRefreshInFlight = false
                if (initialMessageLoaderShown) {
                    hideInitialMessageLoadingIndicator()
                }
                HomaDiagnostics.log("MAIN_LOAD_END", "token=" + token + " durationMs=" + ((System.nanoTime() - started) / 1_000_000))
            }
        }
    }

    private fun getProviderMessageCount(): Int {
        fun count(uri: android.net.Uri, idColumn: String): Int {
            return runCatching {
                contentResolver.query(uri, arrayOf(idColumn), null, null, null)?.use { it.count } ?: 0
            }.getOrDefault(0)
        }

        return count(Telephony.Sms.CONTENT_URI, Telephony.Sms._ID) +
            count(Telephony.Mms.CONTENT_URI, Telephony.Mms._ID)
    }

    private fun setupInitialMessageLoadingIndicator() {
        if (initialMessageLoadingOverlay != null) return

        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val overlay = FrameLayout(this).apply {
            setBackgroundColor(Color.argb(45, 0, 0, 0))
            isClickable = false
            isFocusable = false
            elevation = dp(30).toFloat()
            translationZ = dp(30).toFloat()
        }

        val card = com.google.android.material.card.MaterialCardView(this).apply {
            radius = 28f * density
            cardElevation = 16f * density
            setCardBackgroundColor(getSurfaceColor())
            strokeWidth = 0
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(24), dp(28), dp(24))
        }

        val orb = ThinkingOrbView(this)
        content.addView(orb, LinearLayout.LayoutParams(dp(120), dp(120)))

        val text = TextView(this).apply {
            setTextColor(getProperTextColor())
            textSize = 15f
            gravity = Gravity.CENTER
            includeFontPadding = false
            text = getString(R.string.loading_messages)
            setPadding(0, dp(10), 0, 0)
        }
        content.addView(
            text,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        card.addView(content)

        overlay.addView(
            card,
            FrameLayout.LayoutParams(
                dp(260),
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )

        binding.root.addView(
            overlay,
            androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams(
                androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams.MATCH_PARENT,
                androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams.MATCH_PARENT
            )
        )

        overlay.bringToFront()
        card.bringToFront()

        initialMessageLoadingOverlay = overlay
        initialMessageLoadingText = text
        initialMessageLoadingOrb = orb
    }

    private fun showInitialMessageLoadingIndicator() {
        if (initialMessageLoadingOverlay == null) setupInitialMessageLoadingIndicator()
        initialMessageLoadingOverlay?.visibility = View.VISIBLE
        initialMessageLoadingOverlay?.bringToFront()
        initialMessageLoadingOrb?.resumeAnimation()
        updateInitialMessageLoadingProgress(initialMessageLoadingLoaded)
    }

    private fun updateInitialMessageLoadingProgress(loaded: Int) {
        val total = initialMessageLoadingTotal
        val safeLoaded = if (total > 0) loaded.coerceIn(0, total) else loaded.coerceAtLeast(0)
        runOnUiThread {
            initialMessageLoadingText?.text = if (total > 0) {
                getString(R.string.messages_loading_progress, safeLoaded, total)
            } else {
                getString(R.string.loading_messages)
            }
        }
    }

    private fun hideInitialMessageLoadingIndicator() {
        runOnUiThread {
            initialMessageLoadingOrb?.pauseAnimation()
            initialMessageLoadingOverlay?.visibility = View.GONE
        }
    }

    private fun getOrCreateConversationsAdapter(): ConversationsAdapter {
        if (isDynamicTheme() && !isSystemInDarkMode()) {
            binding.conversationsList.setBackgroundColor(getSurfaceColor())
        }

        var currAdapter = binding.conversationsList.adapter
        if (currAdapter == null) {
            hideKeyboard()
            currAdapter = ConversationsAdapter(
                activity = this,
                recyclerView = binding.conversationsList,
                onRefresh = { notifyDatasetChanged() },
                itemClick = { handleConversationClick(it) }
            )

            binding.conversationsList.adapter = currAdapter
            if (areSystemAnimationsEnabled) {
                binding.conversationsList.scheduleLayoutAnimation()
            }
        }
        return currAdapter as ConversationsAdapter
    }

    private fun setupConversations(
        conversations: ArrayList<Conversation>,
        cached: Boolean = false,
    ) {
        val sortedConversations = if (config.unreadAtTop) {
            conversations.sortedWith(
                compareByDescending<Conversation> {
                    config.pinnedConversations.contains(it.threadId.toString())
                }
                    .thenBy { it.read }
                    .thenByDescending { it.date }
            ).toMutableList() as ArrayList<Conversation>
        } else {
            conversations.sortedWith(
                compareByDescending<Conversation> {
                    config.pinnedConversations.contains(it.threadId.toString())
                }
                    .thenByDescending { it.date }
                    .thenByDescending { it.isGroupConversation } // Group chats at the top
            ).toMutableList() as ArrayList<Conversation>
        }

        if (cached && config.appRunCount == 1) {
            // there are no cached conversations on the first run so we show the
            // loading placeholder and progress until we are done loading from telephony
            showOrHideProgress(conversations.isEmpty())
        } else {
            showOrHideProgress(false)
            showOrHidePlaceholder(conversations.isEmpty())
        }

        try {
            HomaDiagnostics.log("MAIN_SETUP_CONVERSATIONS", "count=" + sortedConversations.size + " cached=" + cached)
            getOrCreateConversationsAdapter().apply {
                updateConversations(sortedConversations) {
                    if (!cached) {
                        showOrHidePlaceholder(currentList.isEmpty())
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun showOrHideProgress(show: Boolean) {
        if (show) {
            binding.conversationsProgressBar.show()
            binding.noConversationsPlaceholder.beVisible()
            binding.noConversationsPlaceholder.text = getString(R.string.loading_messages)
        } else {
            binding.conversationsProgressBar.hide()
            binding.noConversationsPlaceholder.beGone()
        }
    }

    private fun showOrHidePlaceholder(show: Boolean) {
        binding.conversationsFastscroller.beGoneIf(show)
        binding.noConversationsPlaceholder.beVisibleIf(show)
        binding.noConversationsPlaceholder.text = getString(R.string.no_conversations_found)
        binding.noConversationsPlaceholder2.beVisibleIf(show)
    }

    private fun fadeOutSearch() {
        binding.searchHolder.animate()
            .alpha(0f)
            .setDuration(SHORT_ANIMATION_DURATION)
            .withEndAction {
                binding.searchHolder.beGone()
                searchTextChanged("", true)
            }.start()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun notifyDatasetChanged() {
        getOrCreateConversationsAdapter().notifyDataSetChanged()
    }

    private fun handleConversationClick(any: Any) {
        Intent(this, ThreadActivity::class.java).apply {
            val conversation = any as Conversation
            putExtra(THREAD_ID, conversation.threadId)
            putExtra(THREAD_TITLE, conversation.title)
            startActivity(this)
        }
    }

    private fun launchNewConversation() {
        hideKeyboard()
        Intent(this, NewConversationActivity::class.java).apply {
            startActivity(this)
        }
    }

    private fun checkShortcut() {
        val iconColor = getProperPrimaryColor()
        if (config.lastHandledShortcutColor != iconColor) {
            val newConversation = getCreateNewContactShortcut(iconColor)

            val manager = getSystemService(ShortcutManager::class.java)
            try {
                manager.dynamicShortcuts = listOf(newConversation)
                config.lastHandledShortcutColor = iconColor
            } catch (_: Exception) {
            }
        }
    }

    @SuppressLint("NewApi")
    private fun getCreateNewContactShortcut(iconColor: Int): ShortcutInfo {
        val newEvent = getString(R.string.new_conversation)
        val drawable =
            AppCompatResources.getDrawable(this, R.drawable.shortcut_plus)

        (drawable as LayerDrawable).findDrawableByLayerId(R.id.shortcut_plus_background)
            .applyColorFilter(iconColor)
        val bmp = drawable.convertToBitmap()

        val intent = Intent(this, NewConversationActivity::class.java)
        intent.action = Intent.ACTION_VIEW
        return ShortcutInfo.Builder(this, "new_conversation")
            .setShortLabel(newEvent)
            .setLongLabel(newEvent)
            .setIcon(Icon.createWithBitmap(bmp))
            .setIntent(intent)
            .setRank(0)
            .build()
    }

    private fun searchTextChanged(text: String, forceUpdate: Boolean = false) {
        if (!binding.mainMenu.isSearchOpen && !forceUpdate) {
            return
        }

        lastSearchedText = text
        binding.searchPlaceholder2.beGoneIf(text.length >= 2)
        if (text.length >= 2) {
            ensureBackgroundThread {
                val searchQuery = "%$text%"
                val labelQuery = "%" + text.removePrefix("#") + "%"
                val messages = messagesDB.getMessagesWithText(searchQuery, labelQuery)
                val conversations = conversationsDB.getConversationsWithText(searchQuery, labelQuery)
                if (text == lastSearchedText) {
                    showSearchResults(messages, conversations, text)
                }
            }
        } else {
            binding.searchPlaceholder.beVisible()
            binding.searchResultsList.beGone()
        }
        binding.mainMenu.clearSearch()
    }

    private fun showSearchResults(
        messages: List<Message>,
        conversations: List<Conversation>,
        searchedText: String,
    ) {
        val searchResults = ArrayList<SearchResult>()
        conversations.forEach { conversation ->
            val date = (conversation.date * 1000L).formatDateOrTime(
                context = this,
                hideTimeOnOtherDays = true,
                showCurrentYear = true
            )

            val searchResult = SearchResult(
                messageId = -1,
                title = conversation.title,
                phoneNumber = conversation.phoneNumber,
                snippet = conversation.phoneNumber,
                date = date,
                threadId = conversation.threadId,
                photoUri = conversation.photoUri,
                isCompany = conversation.isCompany,
                isBlocked = conversation.isBlocked
            )
            searchResults.add(searchResult)
        }

        messages.sortedByDescending { it.id }.forEach { message ->
            var recipient = message.senderName
            if (recipient.isEmpty() && message.participants.isNotEmpty()) {
                val participantNames = message.participants.map { it.name }
                recipient = TextUtils.join(", ", participantNames)
            }

            val phoneNumber = message.participants.firstOrNull()!!.phoneNumbers.firstOrNull()!!.normalizedNumber
            val date = (message.date * 1000L).formatDateOrTime(
                context = this,
                hideTimeOnOtherDays = true,
                showCurrentYear = true
            )
            val isCompany =
                if (message.participants.size == 1) message.participants.first().isABusinessContact() else false

            val searchResult = SearchResult(
                messageId = message.id,
                title = recipient,
                phoneNumber = phoneNumber,
                snippet = message.body,
                date = date,
                threadId = message.threadId,
                photoUri = message.senderPhotoUri,
                isCompany = isCompany
            )
            searchResults.add(searchResult)
        }

        runOnUiThread {
            binding.searchResultsList.beVisibleIf(searchResults.isNotEmpty())
            binding.searchPlaceholder.beVisibleIf(searchResults.isEmpty())

            val currAdapter = binding.searchResultsList.adapter
            if (currAdapter == null) {
                SearchResultsAdapter(this, searchResults, binding.searchResultsList, searchedText) {
                hideKeyboard()
                    Intent(this, ThreadActivity::class.java).apply {
                        putExtra(THREAD_ID, (it as SearchResult).threadId)
                        putExtra(THREAD_TITLE, it.title)
                        putExtra(SEARCHED_MESSAGE_ID, it.messageId)
                        startActivity(this)
                    }
                }.apply {
                    binding.searchResultsList.adapter = this
                }
            } else {
                (currAdapter as SearchResultsAdapter).updateItems(searchResults, searchedText)
            }
        }
    }

    private fun launchRecycleBin() {
        hideKeyboard()
        startActivity(Intent(applicationContext, RecycleBinConversationsActivity::class.java))
    }

    private fun launchArchivedConversations() {
        hideKeyboard()
        startActivity(Intent(applicationContext, ArchivedConversationsActivity::class.java))
    }

    private fun launchSettings() {
        hideKeyboard()
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refreshConversations(@Suppress("unused") event: Events.RefreshConversations) {
        refreshConversationsFromCache()
    }

    private fun refreshConversationsFromCache() {
        val token = ++conversationLoadToken
        ensureBackgroundThread {
            try {
                val conversations = conversationsDB.getNonArchived()
                    .toMutableList() as ArrayList<Conversation>
                runOnUiThread {
                    if (token != conversationLoadToken || isFinishing || isDestroyed) return@runOnUiThread
                    setupConversations(conversations)
                }
            } catch (e: Exception) {
                HomaDiagnostics.error("MAIN_CACHE_REFRESH_FAILED", e)
            }
        }
    }

    private fun checkWhatsNewDialog() {
        whatsNewList().apply {
            checkWhatsNew(this, BuildConfig.VERSION_CODE)
        }
    }

    private fun checkErrorDialog() {
        if (baseConfig.lastError != "") {
            ConfirmationDialog(
                this,
                "An error occurred while the application was running. Please send us this error so we can fix it.",
                positive = com.goodwy.commons.R.string.send_email
            ) {
                val appName = getString(R.string.app_name)
                val versionName = BuildConfig.VERSION_NAME
                val body = "$appName($versionName) : LastError"
                val address = getMyMailString()
                val lastError = baseConfig.lastError

                val emailIntent = Intent(ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
                    putExtra(Intent.EXTRA_SUBJECT, body)
                    putExtra(Intent.EXTRA_TEXT, lastError)

                    // set the type for better compatibility
                    type = "message/rfc822"
                }

                try {
                    startActivity(Intent.createChooser(emailIntent, "Send email"))
                } catch (_: ActivityNotFoundException) {
                    toast(com.goodwy.commons.R.string.no_app_found)
                } catch (e: Exception) {
                    showErrorToast(e)
                }

                baseConfig.lastError = ""
            }
        }
    }
}