package smartcam

import kotlinx.coroutines.experimental.future.await
import org.jetbrains.ktor.application.ApplicationCall
import org.jetbrains.ktor.http.HttpStatusCode
import org.jetbrains.ktor.request.receive
import org.jetbrains.ktor.request.receiveParameters
import org.jetbrains.ktor.response.respond
import org.jetbrains.ktor.response.respondText
import org.jetbrains.ktor.routing.Route
import org.jetbrains.ktor.routing.get
import org.jetbrains.ktor.routing.post
import smartcam.util.buildDynamoQueryRequest
import software.amazon.awssdk.services.dynamodb.*
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse
import java.util.concurrent.CompletableFuture
import com.fasterxml.jackson.module.kotlin.*
import org.jetbrains.ktor.request.receiveText


fun Route.videos(cli: DynamoDBAsyncClient, defaultMaxMins: Long, table: String) {
    get("/cameras/{cameraId}/videos") {
        // return list of videos between `from`
        // and `to` parameters (defined in unix epoch millseconds);
        // if `from` and `to` not passed, return videos
        // with start time between (now - defaultMaxMins) and now;
        val from = call.parameters["from"]
        val to = call.parameters["to"]
        val queryRequest = buildDynamoQueryRequest("cameraId",
            from,
            to,
            "start",
            defaultMaxMins,
            table)
        try {
            val resp = cli.query(queryRequest)
                .thenApply { it.items() }
                .thenApply { it.map { videoFromDynamoItem(it) } }
            resp.await()
            call.respond(resp)
        } catch (e: Throwable) {
            // FIXME:
            System.err.println("GET /cameras/{cameraId}/videos: $e")
            call.respond(HttpStatusCode.InternalServerError)
        }

    }
    post("/videos") {
        try {
            println("1")
            val mapper = jacksonObjectMapper()
            println("2")
            val rawVideo = call.receiveText()
            println("3")
            val video = mapper.readValue<Video>(rawVideo)
            println("4")
            val videoItem = video.toDynamoRecord()
            val videoPutRequest: PutItemRequest = PutItemRequest.builder()
                    .tableName(table)
                    .item(videoItem)
                    .build()
            // nullable b/c of mockito:
            val resp: CompletableFuture<PutItemResponse>? = cli.putItem(videoPutRequest)
            resp?.await()
            call.respondText(resp.toString())
        } catch(e: MissingKotlinParameterException) {
            call.respond(HttpStatusCode.BadRequest)
        } catch (e: Exception) {
            // FIXME:
            System.err.println("POST /videos: $e")
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}

