package com.olliekennedy

import app
import org.http4k.core.HttpHandler
import org.http4k.hotreload.HotReloadServer
import org.http4k.hotreload.HotReloadable

class ReloadableHttpApp : HotReloadable<HttpHandler> {
    override fun create() = app
}

fun main() {
    HotReloadServer.http<ReloadableHttpApp>().start()
}
