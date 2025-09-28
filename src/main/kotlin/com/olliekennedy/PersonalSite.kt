package com.olliekennedy

import com.olliekennedy.formats.JacksonMessage
import com.olliekennedy.formats.jacksonMessageLens
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

val app: HttpHandler = routes(
    "/" bind GET to {
        Response(OK).body("Hello, my name is Ollie and this is my website. Enjoy.")
    },

    "/ping" bind GET to {
        Response(OK).body("pong")
    },

    "/formats/json/jackson" bind GET to {
        Response(OK).with(jacksonMessageLens of JacksonMessage("Barry", "Hello there!"))
    },

    "/templates/handlebars" bind GET to {
        val renderer = HandlebarsTemplates().CachingClasspath()
        val view = Body.viewModel(renderer, TEXT_HTML).toLens()
        val viewModel = HandlebarsViewModel("Hello there!")
        Response(OK).with(view of viewModel)
    },

    "/testing/hamkrest" bind GET to {request ->
        Response(OK).body("Echo '${request.bodyString()}'")
    }
)

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 9000
    val printingApp: HttpHandler = PrintRequest().then(app)
    printingApp.asServer(Jetty(port)).start()
    println("Server started on $port")
}
