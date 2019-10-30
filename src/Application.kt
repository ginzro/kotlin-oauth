package com.ginzro

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.html.*
import kotlinx.html.*
import kotlinx.css.*
import kotlin.random.Random

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

object ConstValue {
    const val CLIENT_ID = "oauth-client-1"
    const val CLIENT_SECRET = "oauth-client-secret-1"
    val REDIRECT_URI = "http://localhost:9000/callback"
}

object DB {
    val states: MutableMap<String, String> = mutableMapOf()
}


@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    routing {
        get("/") {
            call.respondHtml {
                body {
                    h1 { +"OAuth徹底入門 with Kotlin" }
                    a(href = "http://localhost:9000/authorize") { +"AUTHORIZE" }
                }
            }
        }

        get("/authorize") {
            call.respondRedirect(false) {
                host = "localhost"
                port = 9001
                path("authorize")

                parameters["response_type"] = "code"
                parameters["client_id"] = ConstValue.CLIENT_ID
                parameters["redirect_uri"] = ConstValue.REDIRECT_URI

                val state = getRandomString()
                DB.states[ConstValue.CLIENT_ID] = state
                parameters["state"] = state
            }
        }

        get("/callback") {
            val code = call.request.queryParameters["code"]
            val state = call.request.queryParameters["state"]

            call.respondHtml {
                body {
                    h1 { +"OAuth徹底入門 with Kotlin" }
                    h2 { +"response" }
                    ul {
                        li { +"code is $code" }
                        li { +"pre state is ${DB.states[ConstValue.CLIENT_ID]}" }
                        li { +"state is $state" }
                    }
                    h2 { +"next" }
                    a(href = "http://localhost:9001/token") { +"TOKEN" }
                }
            }
        }


        get("/html-dsl") {

            val name = call.request.queryParameters["name"]
            if (name != null) {
                call.respondText("$name HELLO WORLD!", contentType = ContentType.Text.Plain)
            } else {
                call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
            }

        }

        get("/styles.css") {
            call.respondCss {
                body {
                    backgroundColor = Color.red
                }
                p {
                    fontSize = 2.em
                }
                rule("p.myclass") {
                    color = Color.blue
                }
            }
        }
    }
}

fun FlowOrMetaDataContent.styleCss(builder: CSSBuilder.() -> Unit) {
    style(type = ContentType.Text.CSS.toString()) {
        +CSSBuilder().apply(builder).toString()
    }
}

fun CommonAttributeGroupFacade.style(builder: CSSBuilder.() -> Unit) {
    this.style = CSSBuilder().apply(builder).toString().trim()
}

suspend inline fun ApplicationCall.respondCss(builder: CSSBuilder.() -> Unit) {
    this.respondText(CSSBuilder().apply(builder).toString(), ContentType.Text.CSS)
}

fun getRandomString(): String {
    val charPool = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return (1..20)
        .map { Random.nextInt(0, charPool.size) }
        .map { charPool[it] }
        .joinToString("")
}


