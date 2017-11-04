package smartcam

import kotlinx.coroutines.experimental.future.await
import org.jetbrains.ktor.application.Application
import org.jetbrains.ktor.application.log
import org.jetbrains.ktor.http.HttpStatusCode
import org.jetbrains.ktor.locations.get
import org.jetbrains.ktor.locations.location
import org.jetbrains.ktor.request.receive
import org.jetbrains.ktor.response.respond
import org.jetbrains.ktor.response.respondText
import org.jetbrains.ktor.routing.Route
import org.jetbrains.ktor.routing.get
import org.jetbrains.ktor.util.ValuesMap
import smartcam.util.buildDynamoQueryRequest
import software.amazon.awssdk.services.dynamodb.DynamoDBAsyncClient

fun Route.root() {
   get<Root> {
       call.respondText("smartcam")
   }
}

fun <DynamoItem> Route.dynamoRangeQuery(, r: String, cli: DynamoDBAsyncClient, defaultMaxMins: Long, table: String) {
    get(r) {
        // return list of videos between `from`
        // and `to` parameters (defined in unix epoch milliseconds);
        // if `from` and `to` not passed, return videos
        // with start time between (now - defaultMaxMins) and now;
        val id = t.cameraId
        val from = call.parameters["from"]
        val to = call.parameters["to"]
        val queryRequest = buildDynamoQueryRequest(call, id, from, to, "start", defaultMaxMins, table)
        try {
            val resp = cli.query(queryRequest)
                .thenApply { it.items() }
                .thenApply{ it.map{ t.fromDynamoItem(it) } }
            resp.await()
            call.respond(resp)
        } catch (e: Throwable) {
            // FIXME
            println("GET /cameras/{cameraId}/videos: $e")
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}