package smartcam

import com.amazonaws.services.s3.AmazonS3
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*
import io.kotlintest.specs.ShouldSpec
import io.kotlintest.specs.StringSpec
import com.nhaarman.mockito_kotlin.*
import io.ktor.cio.readChannel
import io.ktor.features.DefaultHeaders
import io.ktor.routing.Routing
import software.amazon.awssdk.services.dynamodb.DynamoDBAsyncClient
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse
import java.util.concurrent.CompletableFuture
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.request.httpMethod

fun Application.test() {
    val dynamoCli: DynamoDBAsyncClient = mock()
    val s3Cli: AmazonS3 = mock()
    val xm
    whenever(
            dynamoCli.putItem(PutItemRequest.builder()
                    .tableName("testTable")
                    .build()))
            .thenReturn(CompletableFuture.completedFuture(PutItemResponse.builder().build()))
    install(DefaultHeaders)
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
            disableHtmlEscaping()
        }
    }
    install(Routing) {
        cameras(dynamoCli, "testTable1")
        videos(dynamoCli, s3Cli, mock {}, 1, "testBucket", "testTable")
        detections(dynamoCli, 1, "testTable")
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

class PostVideoData : StringSpec() {
    RequestResponseBuilder()
    init {
        "posting file should return 200" {
            withTestApplication(Application::test) {
                with(handleRequest(HttpMethod.Post, "/videodata") {
                    addHeader("Transfer-Encoding", "chunked")
                })
            }
        }
        "posting nothing should return 400" {

        }
    }
}

class PostCamera : StringSpec() {
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
