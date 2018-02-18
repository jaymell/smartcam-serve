package smartcam

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.transfer.TransferManager
import io.ktor.application.call
import kotlinx.coroutines.experimental.future.await
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveChannel
import io.ktor.request.receiveStream
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import kotlinx.coroutines.experimental.async
import smartcam.util.*
import software.amazon.awssdk.services.dynamodb.*
import smartcam.util.putDynamoItem
import org.apache.commons.codec.digest.DigestUtils;
import java.io.FileInputStream
import java.io.RandomAccessFile

fun Route.videos(dynamoCli: DynamoDBAsyncClient,
                 s3Cli: AmazonS3,
                 xm: TransferManager,
                 defaultMaxMins: Long,
                 bucket: String,
                 table: String) {
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
        try {
            val f = createTempFile()
            f.copyInputStreamToFile(call.receiveStream())
            val key = DigestUtils.md5Hex(FileInputStream(f))
            println("Uploading to bucket: $bucket, key $key")
            putS3Object(xm, bucket, key, f, hashMapOf())
            try {
                f.delete()
            } catch(e: Exception) {
               System.err.println("ERROR: Failed to delete temp file")
            }
            call.respondText { key }
        } catch (e: Exception) {
            System.err.println("POST /videodata failed")
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}

