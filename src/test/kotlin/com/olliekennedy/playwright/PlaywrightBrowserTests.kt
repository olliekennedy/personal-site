package com.olliekennedy.playwright

import com.olliekennedy.app
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.playwright.Http4kBrowser
import org.http4k.playwright.LaunchPlaywrightBrowser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class PlaywrightBrowserTests {

    @RegisterExtension
    val playwright = LaunchPlaywrightBrowser(app)

    @Test
    fun `can browse app`(browser: Http4kBrowser) {
        with(browser.newPage()) {
            assertEquals("hello", navigateHome().text())
        }
    }
}

