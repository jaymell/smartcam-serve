package smartcam

import com.fasterxml.jackson.module.kotlin.*
import kotlin.reflect.jvm.*
import kotlinx.coroutines.experimental.future.await
import org.jetbrains.ktor.http.HttpStatusCode
import org.jetbrains.ktor.response.respond
import org.jetbrains.ktor.routing.Route
import org.jetbrains.ktor.routing.get
import smartcam.util.buildDynamoQueryRequest
import software.amazon.awssdk.services.dynamodb.*

fun Route.detections(cli: DynamoDBAsyncClient, defaultMaxMins: Long, table: String) {
    get("/cameras/{camera_id}/detections") {
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
            "time",
            defaultMaxMins,
            table)
        try {
            val resp = cli.query(queryRequest)
                    .thenApply { it.items() }
                    .thenApply { it.map { detectionFromDynamoItem(it) } }
            resp.await()
            call.respond(resp)
        } catch (e: Throwable) {
            // FIXME:
            System.err.println("GET /cameras/{camera_id}/detections: $e")
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}