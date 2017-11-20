package smartcam

import kotlinx.coroutines.experimental.future.await
import org.jetbrains.ktor.http.HttpStatusCode
import org.jetbrains.ktor.response.respond
import org.jetbrains.ktor.routing.Route
import org.jetbrains.ktor.routing.get
import org.jetbrains.ktor.routing.post
import smartcam.util.buildDynamoQueryRequest
import software.amazon.awssdk.services.dynamodb.*
import smartcam.util.putDynamoItem


fun Route.videos(cli: DynamoDBAsyncClient, defaultMaxMins: Long, table: String) {
    get("/cameras/{camera_id}/videos") {
        // return list of videos between `from`
        // and `to` parameters (defined in unix epoch millseconds);
        // if `from` and `to` not passed, return videos
        // with start time between (now - defaultMaxMins) and now;
        val from = call.parameters["from"]
        val to = call.parameters["to"]
        val pkValue = call.parameters["camera_id"]
        val queryRequest = buildDynamoQueryRequest("camera_id",
            pkValue,
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
            System.err.println("GET /cameras/{camera_id}/videos: $e")
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
    post("/videos") {
        putDynamoItem<Video>(call, cli, table)
    }
}

