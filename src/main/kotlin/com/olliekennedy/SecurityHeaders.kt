package com.olliekennedy

import org.http4k.core.Filter
import org.http4k.core.Response

object SecurityHeaders {
    val Add = Filter { next ->
        { req ->
            val resp: Response = next(req)
            resp
                .header("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload")
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Type", "text/html; charset=utf-8")
                .header("X-Frame-Options", "DENY")
                .header("Referrer-Policy", "strict-origin-when-cross-origin")
                .header("Permissions-Policy", "geolocation=(), microphone=(), camera=()")
                .header("Content-Security-Policy",
                    "default-src 'self'; frame-ancestors 'none'; object-src 'none'; base-uri 'self';")
                .header("X-XSS-Protection", "0")
        }
    }
}
