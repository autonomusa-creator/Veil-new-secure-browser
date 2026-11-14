package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import com.example.core.SearchEngine
import com.example.ui.BrowserTab
import com.example.ui.components.TopAddressBar
import com.example.ui.theme.VeilTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VeilTopBarUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun topAddressBar_allowsTypingAndSubmittingUrlOrSearchQuery() {
        var submittedInput = ""

        composeRule.setContent {
            VeilTheme {
                TopAddressBar(
                    activeTab = BrowserTab(id = "tab-1"),
                    adBlockEnabled = true,
                    searchEngine = SearchEngine.DUCKDUCKGO,
                    bookmarks = emptyList(),
                    onSubmitInput = { submittedInput = it },
                    onSelectSearchEngine = {},
                    onOpenPrivacyInspector = {},
                    onOpenSecurityDetails = {},
                    onReloadOrStop = {}
                )
            }
        }

        val inputNode = composeRule.onNodeWithTag("top_url_input")
        inputNode.assertIsDisplayed()
        inputNode.performClick()
        inputNode.performTextInput("eff.org")
        inputNode.performImeAction()

        assertEquals("eff.org", submittedInput)
    }

    @Test
    fun topAddressBar_goButtonSubmitsSearchQuery() {
        var submittedInput = ""

        composeRule.setContent {
            VeilTheme {
                TopAddressBar(
                    activeTab = BrowserTab(id = "tab-1"),
                    adBlockEnabled = true,
                    searchEngine = SearchEngine.DUCKDUCKGO,
                    bookmarks = emptyList(),
                    onSubmitInput = { submittedInput = it },
                    onSelectSearchEngine = {},
                    onOpenPrivacyInspector = {},
                    onOpenSecurityDetails = {},
                    onReloadOrStop = {}
                )
            }
        }

        val inputNode = composeRule.onNodeWithTag("top_url_input")
        inputNode.performClick()
        inputNode.performTextInput("open source privacy")
        composeRule.onNodeWithTag("top_url_go_button").performClick()

        assertEquals("open source privacy", submittedInput)
    }
}
