package smartcam

import org.jetbrains.ktor.response.respondText
import org.jetbrains.ktor.routing.Route
import org.jetbrains.ktor.routing.get
import org.jetbrains.ktor.routing.post
import smartcam.util.putDynamoItem
import software.amazon.awssdk.services.dynamodb.DynamoDBAsyncClient

fun Route.cameras(cli: DynamoDBAsyncClient,
                  defaultMaxMins: Long,
                  videoTable: String,
                  detectionTable: String,
                  cameraTable: String) {
    get("/cameras") {
        call.respondText("GET /cameras")
    }
    post("/cameras") {
        putDynamoItem<Camera>(call, cli, cameraTable)
    }
    videos(cli, defaultMaxMins, videoTable)
    detections(cli, defaultMaxMins, detectionTable)
}
