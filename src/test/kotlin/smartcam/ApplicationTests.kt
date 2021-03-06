package smartcam

import com.amazonaws.services.s3.AmazonS3
import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.http.*
import org.jetbrains.ktor.testing.*
import kotlin.test.*
import io.kotlintest.specs.ShouldSpec
import io.kotlintest.specs.StringSpec
import com.nhaarman.mockito_kotlin.*
import org.jetbrains.ktor.features.DefaultHeaders
import org.jetbrains.ktor.gson.GsonSupport
import org.jetbrains.ktor.routing.Routing
import software.amazon.awssdk.services.dynamodb.DynamoDBAsyncClient
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse
import java.util.concurrent.CompletableFuture
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import software.amazon.awssdk.util.json.JacksonUtils

fun Application.test() {
    val dynamoCli: DynamoDBAsyncClient = mock()
    val s3Cli: AmazonS3 = mock()
    whenever(
        dynamoCli.putItem(PutItemRequest.builder()
            .tableName("testTable")
            .build()))
        .thenReturn(CompletableFuture.completedFuture(PutItemResponse.builder().build()))
    install(DefaultHeaders)
    install(GsonSupport) {
        setPrettyPrinting()
    }
    install(Routing) {
        cameras(dynamoCli, s3Cli, 15, "testTable1", "testTable2", "testTable3")
    }
}

class GetRoot : ShouldSpec() {
    init {
        should("return something worthless") {
            withTestApplication(Application::main) {
                with(handleRequest(HttpMethod.Get, "/")) {
                    assertEquals(HttpStatusCode.OK, response.status())
                    assertEquals("smartcam", response.content)
                }
            }
        }
    }
}

class PostVideo : StringSpec() {
    init {
        "good data should get a good response" {
           withTestApplication(Application::test) {
               with(handleRequest(HttpMethod.Post, "/videos") {
                   addHeader("Content-Type", "application/json")
                   body = """
                       {"camera_id": "1",
                        "start": 1509228056000.0,
                        "end": 1509228056001.0,
                        "width": 1,
                        "height": 2,
                        "bucket": "testbucket",
                        "key": "testkey",
                        "region": "testregion" }"""
               }) {
                   assertEquals(HttpStatusCode.OK, response.status())
               }
           }
        }
        "bad data should return a 400" {
            withTestApplication(Application::test) {
               with(handleRequest(HttpMethod.Post, "/videos") {
                   addHeader("Content-Type", "application/json")
                   body = """ {"camera_id": "1", "start": 1509228056000.0} """
               }) {
                   assertEquals(HttpStatusCode.BadRequest, response.status())
               }
           }
        }
    }
}

class PostCamera: StringSpec() {
    init {
        "posting nonsense object should fail" {
            withTestApplication(Application::test) {
                with(handleRequest(HttpMethod.Post, "/cameras") {
                    addHeader("Content-Type", "application/json")
                    body = """ { "test": "thisShouldFail" } """
                }) {
                    assertEquals(HttpStatusCode.BadRequest, response.status())
                }
            }
        }
    }
}
