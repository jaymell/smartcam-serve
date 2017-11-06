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
        val video = call.receive<Video>()
        println("cmaera: ${video.camera_id}")
        println("start: ${video.start}")
        val videoItem = video.toDynamoRecord()
        val videoPutRequest: PutItemRequest = PutItemRequest.builder()
                .tableName(table)
                .item(videoItem)
                .build()
        try {
            // nullable b/c of mockito:
            val resp: CompletableFuture<PutItemResponse>? = cli.putItem(videoPutRequest)
            resp?.await()
            call.respondText(resp.toString())
        } catch (e: Throwable) {
            // FIXME:
            System.err.println("POST /videos: $e")
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}

