package smartcam

import org.jetbrains.ktor.response.respondText
import org.jetbrains.ktor.routing.Route
import org.jetbrains.ktor.routing.get
import software.amazon.awssdk.services.dynamodb.DynamoDBAsyncClient

fun Route.cameras(cli: DynamoDBAsyncClient, defaultMaxMins: Long, videoTable: String, detectionTable: String) {
    get("/cameras") {
        call.respondText("GET /cameras")
    }
    videos(cli, defaultMaxMins, videoTable)
    detections(cli, defaultMaxMins, detectionTable)
}
