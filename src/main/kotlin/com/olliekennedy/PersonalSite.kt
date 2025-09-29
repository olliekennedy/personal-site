package com.olliekennedy

import com.olliekennedy.models.HandlebarsViewModel
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.DebuggingFilters.PrintRequest
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.viewModel

val renderer = HandlebarsTemplates().CachingClasspath()
val view = Body.viewModel(renderer, TEXT_HTML).toLens()

private fun rawRoutes(): HttpHandler = routes(
    "/" bind GET to {
        Response(OK)
            .with(view of HandlebarsViewModel("Hello, my name is Ollie and this is my website. Enjoy."))
    },
)

fun buildApp(debug: Boolean = false): HttpHandler {
    val core = SecurityHeaders.Add.then(rawRoutes())
    return if (debug) PrintRequest().then(core) else core
}

val app: HttpHandler = buildApp()

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 9000
    buildApp(debug = true).asServer(Jetty(port)).start()
    println("Server started on $port")
}
