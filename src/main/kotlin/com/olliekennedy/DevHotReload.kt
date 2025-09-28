package com.olliekennedy

import org.http4k.core.HttpHandler
import org.http4k.hotreload.HotReloadServer
import org.http4k.hotreload.HotReloadable

/**
 * Development entrypoint which starts the http4k HotReloadServer.
 * It watches the classpath for changes so code edits are reflected without restarting.
 */
class ReloadableHttpApp : HotReloadable<HttpHandler> {
    override fun create() = app
}

fun main() {
    // Start hot reload server (defaults to same port as normal Jetty server if configured internally; logs actual port)
    val server = HotReloadServer.http<ReloadableHttpApp>().start()
    println("Hot reload dev server started on http://localhost:${server.port()} (Ctrl+C to stop)")
}

