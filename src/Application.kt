package com.ginzro

import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.html.*
import kotlinx.html.*
import kotlinx.css.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.apache.commons.codec.binary.Base64
import java.math.BigInteger
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import kotlin.random.Random

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

object ConstValue {
  const val CLIENT_ID = "oauth-client-1"
  const val CLIENT_SECRET = "oauth-client-secret-1"
  val REDIRECT_URI = "http://localhost:9000/callback"
  lateinit var TOKEN: String
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

      if (code == null || state == null || state != DB.states[ConstValue.CLIENT_ID]) {
        throw Exception("invalid authorization code")
      }

      val req = Request.Builder().apply {
        val utf8 = StandardCharsets.UTF_8.toString()
        val clientIdByteArray = URLEncoder.encode(ConstValue.CLIENT_ID, utf8)
        val secretByteArray = URLEncoder.encode(ConstValue.CLIENT_SECRET, utf8)
        addHeader("Content-Type", "application/x-www-form-urlencoded")
        val idAndPass = Base64.encodeBase64String("$clientIdByteArray:$secretByteArray".toByteArray())
        addHeader("Authorization", "Basic $idAndPass")
        val body = "grant_type=authorization_code&code=$code&redirect_uri${ConstValue.REDIRECT_URI}".toRequestBody()
        method("POST", body)
        url("http://localhost:9001/token")
      }.build()
      val res = OkHttpClient().newCall(req).execute()
      val responseBody = Gson().fromJson(res.body!!.string(), TokenResp::class.java)!!
      ConstValue.TOKEN = responseBody.access_token
      call.respondHtml {
        body {
          h1 { +"OAuth徹底入門 with Kotlin" }
          h2 { +"response" }
          ul {
            li { +"code is $code" }
            li { +"pre state is ${DB.states[ConstValue.CLIENT_ID]}" }
            li { +"state is $state" }
            li { +"token is ${responseBody.access_token}" }
            li { +"token type is ${responseBody.token_type}" }
          }
          h2 { +"next" }
          a(href = "http://localhost:9000/protected_resource") { +"GET PROTECTED RESOURCE" }
        }
      }
    }

    get("/protected_resource") {
      val req = Request.Builder().apply {
        addHeader("Content-Type", "application/x-www-form-urlencoded")
        addHeader("Authorization", "Bearer ${ConstValue.TOKEN}")
        url("http://localhost:9002/resource")
        method("POST", "".toRequestBody())
      }.build()
      val res = OkHttpClient().newCall(req).execute()
      call.respondHtml {
        body {
          h1 { +"OAuth徹底入門 with Kotlin" }
          h2 { +"protected resource" }
          ul {
            li { +"data is ${res.body?.string()}" }
          }
        }
      }
    }
  }
}

fun getRandomString(): String {
  /*
   生成されるトークン(および、エンドユーザーが扱うことを想定していないほかのクレデンシャル)
   について攻撃者が推測できる可能性は 2−128 以下にしなくてはならず(MUST)、
   そして、2−160 以 下にするべきです(SHOULD)。
  */
  return BigInteger(160, SecureRandom()).toString(32)
}


data class TokenResp(
  val access_token: String,
  val token_type: String,
  val scope: String
)
