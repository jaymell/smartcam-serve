package smartcam

import com.amazonaws.services.s3.AmazonS3
import kotlinx.coroutines.experimental.future.await
import org.jetbrains.ktor.cio.toInputStream
import org.jetbrains.ktor.cio.toReadChannel
import org.jetbrains.ktor.http.HttpStatusCode
import org.jetbrains.ktor.request.receiveChannel
import org.jetbrains.ktor.response.respond
import org.jetbrains.ktor.routing.Route
import org.jetbrains.ktor.routing.get
import org.jetbrains.ktor.routing.post
import smartcam.util.buildDynamoQueryRequest
import smartcam.util.getSignedS3Url
import software.amazon.awssdk.services.dynamodb.*
import smartcam.util.putDynamoItem

fun Route.videos(dynamoCli: DynamoDBAsyncClient, s3Cli: AmazonS3, defaultMaxMins: Long, table: String) {
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
            val resp = dynamoCli.query(queryRequest)
                .thenApply { it.items() }
                .thenApply { it.map {
                        val url = getSignedS3Url(s3Cli, it["bucket"]!!.s(), it["key"]!!.s())
                        videoFromDynamoItem(it, url.toExternalForm())
                    }
                }
            resp.await()
            call.respond(resp)
        } catch (e: Throwable) {
            // FIXME:
            System.err.println("GET /cameras/{camera_id}/videos: $e")
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
    post("/videos") {
        putDynamoItem<Video>(call, dynamoCli, table)
    }
    post("/videodata") {
        var a = call.receiveChannel()
        a.toInputStream().use {
            val total = it.read()
            println("read $total bytes")
        }
        call.respond(HttpStatusCode.OK)
    }
}

