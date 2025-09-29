package com.olliekennedy

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.contains
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Request
import org.junit.jupiter.api.Test

class SecurityHeadersTests {

    private val response by lazy { app(Request(Method.GET, "/")) }

    @Test
    fun `adds expected security headers`() {
        val expected = mapOf(
            "Strict-Transport-Security" to "max-age=31536000; includeSubDomains; preload",
            "X-Content-Type-Options" to "nosniff",
            "X-Frame-Options" to "DENY",
            "Referrer-Policy" to "strict-origin-when-cross-origin",
            "Permissions-Policy" to "geolocation=(), microphone=(), camera=()",
            "Content-Security-Policy" to "default-src 'self'; frame-ancestors 'none'; object-src 'none'; base-uri 'self';",
            "X-XSS-Protection" to "0",
        )

        expected.forEach { (headerName, value) ->
            assertThat(
                "Missing or incorrect header: $headerName",
                response.header(headerName),
                equalTo(value)
            )
        }
    }

    @Test
    fun `csp is restrictive`() {
        val csp = response.header("Content-Security-Policy") ?: ""
        assertThat(csp, contains("default-src 'self'".toRegex()))
        assertThat(csp, !contains("unsafe-inline".toRegex()))
        assertThat(csp, !contains("unsafe-eval".toRegex()))
    }
}
