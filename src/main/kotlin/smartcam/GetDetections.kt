package smartcam

import org.jetbrains.ktor.locations.get
import org.jetbrains.ktor.routing.Route
import smartcam.util.buildDynamoQueryRequest
import software.amazon.awssdk.services.dynamodb.DynamoDBAsyncClient

fun Route.dynamoDetectionsQuery(cli: DynamoDBAsyncClient, defaultMaxMins: Long, table: String) {
    get<DynamoDetections> {
        val id = it.cameraId
        val from = call.parameters["from"]
        val to = call.parameters["to"]
        val queryRequest = buildDynamoQueryRequest(call, id, from, to, "time", defaultMaxMins, table)
        try {

        } catch (e: Throwable) {

        }
    }
}