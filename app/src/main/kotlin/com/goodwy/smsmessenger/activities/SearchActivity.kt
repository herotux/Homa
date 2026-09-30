package com.goodwy.smsmessenger.activities

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.View
import android.widget.PopupMenu
import com.goodwy.commons.extensions.*
import com.goodwy.smsmessenger.R
import com.goodwy.smsmessenger.adapters.SearchResultsAdapter
import com.goodwy.smsmessenger.databinding.ActivitySearchBinding
import com.goodwy.smsmessenger.extensions.*
import com.goodwy.smsmessenger.helpers.SEARCHED_MESSAGE_ID
import com.goodwy.smsmessenger.helpers.THREAD_ID
import com.goodwy.smsmessenger.helpers.THREAD_TITLE
import com.goodwy.smsmessenger.models.Conversation
import com.goodwy.smsmessenger.models.Message
import com.goodwy.smsmessenger.models.SearchResult
import java.util.ArrayList

class SearchActivity : SimpleActivity() {
    private val binding by viewBinding(ActivitySearchBinding::inflate)
    private var selectedTagId: Long? = null
    private var lastQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.root.setBackgroundColor(getProperBackgroundColor())
        updateTextColors(binding.root)
        setupEdgeToEdge(padBottomImeAndSystem = listOf(binding.resultsList))
        setupSearch()
        setupTagFilter()
        showPlaceholder(getString(R.string.search_type_at_least_two_characters))
    }

    override fun onResume() {
        super.onResume()
        binding.root.setBackgroundColor(getProperBackgroundColor())
        updateTextColors(binding.root)
    }

    override fun onBackPressedCompat(): Boolean {
        finish()
        return true
    }

    private fun setupSearch() {
        binding.searchMenu.updateTitle(getString(R.string.search))
        binding.searchMenu.searchBeVisibleIf(true)
        binding.searchMenu.requireToolbar().setNavigationOnClickListener { finish() }
        binding.searchMenu.setupMenu()
        binding.searchMenu.onSearchTextChangedListener = { text ->
            lastQuery = text
            runSearch()
        }
        binding.searchMenu.setText("")
        binding.searchMenu.clearSearch()
    }

    private fun setupTagFilter() {
        binding.tagFilter.setOnClickListener { showTagMenu(binding.tagFilter) }
    }

    private fun showTagMenu(anchor: View) {
        ensureBackgroundThread {
            val tags = try {
                getMessagesDB().AnnotationLabelsDao().getLabels()
            } catch (_: Exception) {
                emptyList()
            }

            runOnUiThread {
                PopupMenu(this, anchor).apply {
                    menu.add(Menu.NONE, 0, 0, getString(R.string.search_all_tags))
                    tags.forEachIndexed { index, tag ->
                        menu.add(Menu.NONE, index + 1, index + 1, "#${tag.name}")
                    }
                    if (tags.isEmpty()) {
                        menu.add(Menu.NONE, -1, 1, getString(R.string.search_no_tags)).isEnabled = false
                    }
                    setOnMenuItemClickListener { item ->
                        if (item.itemId == 0) {
                            selectedTagId = null
                            binding.tagFilter.text = getString(R.string.search_filter_tag)
                        } else {
                            tags.getOrNull(item.itemId - 1)?.let { tag ->
                                selectedTagId = tag.id
                                binding.tagFilter.text = "Tag: #${tag.name}"
                            }
                        }
                        runSearch()
                        true
                    }
                    show()
                }
            }
        }
    }

    private fun runSearch() {
        val query = lastQuery.trim()
        val tagId = selectedTagId
        if (query.length < 2 && tagId == null) {
            showPlaceholder(getString(R.string.search_type_at_least_two_characters))
            return
        }

        ensureBackgroundThread {
            val text = "%$query%"
            val textNoHash = "%${query.removePrefix("#")}%"
            val messages = try {
                if (tagId != null) messagesDB.getMessagesWithTextAndLabel(text, textNoHash, tagId)
                else messagesDB.getMessagesWithText(text, textNoHash)
            } catch (_: Exception) { emptyList() }

            val conversations = try {
                if (tagId != null) conversationsDB.getConversationsWithTextAndLabel(text, textNoHash, tagId)
                else conversationsDB.getConversationsWithText(text, textNoHash)
            } catch (_: Exception) { emptyList() }

            if (query == lastQuery && tagId == selectedTagId) {
                showResults(messages, conversations, query)
            }
        }
    }

    private fun showResults(messages: List<Message>, conversations: List<Conversation>, searchedText: String) {
        val results = ArrayList<SearchResult>()
        conversations.forEach { conversation ->
            results.add(SearchResult(
                messageId = -1,
                title = conversation.title,
                phoneNumber = conversation.phoneNumber,
                snippet = conversation.phoneNumber,
                date = (conversation.date * 1000L).formatDateOrTime(this, true, true),
                threadId = conversation.threadId,
                photoUri = conversation.photoUri,
                isCompany = conversation.isCompany,
                isBlocked = conversation.isBlocked
            ))
        }
        messages.sortedByDescending { it.id }.forEach { message ->
            var recipient = message.senderName
            if (recipient.isEmpty() && message.participants.isNotEmpty()) {
                recipient = TextUtils.join(", ", message.participants.map { it.name })
            }
            val participant = message.participants.firstOrNull()
            val phoneNumber = participant?.phoneNumbers?.firstOrNull()?.normalizedNumber ?: message.senderPhoneNumber
            val isCompany = message.participants.size == 1 && participant?.isABusinessContact() == true
            results.add(SearchResult(
                messageId = message.id,
                title = recipient.ifEmpty { phoneNumber ?: "" },
                phoneNumber = phoneNumber,
                snippet = message.body,
                date = (message.date * 1000L).formatDateOrTime(this, true, true),
                threadId = message.threadId,
                photoUri = message.senderPhotoUri,
                isCompany = isCompany
            ))
        }
        runOnUiThread {
            if (results.isEmpty()) {
                showPlaceholder(getString(R.string.search_no_results))
                return@runOnUiThread
            }
            binding.searchPlaceholder.beGone()
            binding.resultsList.beVisible()
            val current = binding.resultsList.adapter
            if (current == null) {
                SearchResultsAdapter(this, results, binding.resultsList, searchedText) {
                    hideKeyboard()
                    val result = it as SearchResult
                    startActivity(Intent(this, ThreadActivity::class.java).apply {
                        putExtra(THREAD_ID, result.threadId)
                        putExtra(THREAD_TITLE, result.title)
                        putExtra(SEARCHED_MESSAGE_ID, result.messageId)
                    })
                }.also { binding.resultsList.adapter = it }
            } else {
                (current as SearchResultsAdapter).updateItems(results, searchedText)
            }
        }
    }

    private fun showPlaceholder(text: String) {
        binding.resultsList.beGone()
        binding.searchPlaceholder.apply {
            this.text = text
            beVisible()
        }
    }
}
