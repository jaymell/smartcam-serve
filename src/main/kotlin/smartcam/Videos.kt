package smartcam

import com.amazonaws.services.s3.AmazonS3
import io.ktor.application.call
import kotlinx.coroutines.experimental.future.await
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveChannel
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import smartcam.util.buildDynamoQueryRequest
import smartcam.util.getSignedS3Url
import software.amazon.awssdk.services.dynamodb.*
import smartcam.util.putDynamoItem
import java.nio.ByteBuffer

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
        val buf = ByteBuffer.allocate(1000000000)
        val a = call.receiveChannel()
        for ( i in a )
        call.receiveChannel().use {
            println("here bitch")
            while(true) {
                println("going to read")
                val i = it.read(buf)
                println("just finished a read")
                if (i == -1 ) break
                println("received stuff")
            }
        }
        call.respond(HttpStatusCode.OK)
    }
}

