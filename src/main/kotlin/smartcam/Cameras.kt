package smartcam

import kotlinx.coroutines.experimental.future.await
import org.jetbrains.ktor.response.respond
import org.jetbrains.ktor.response.respondText
import org.jetbrains.ktor.routing.Route
import org.jetbrains.ktor.routing.get
import org.jetbrains.ktor.routing.post
import smartcam.util.putDynamoItem
import software.amazon.awssdk.services.dynamodb.DynamoDBAsyncClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.ScanRequest
import software.amazon.awssdk.services.dynamodb.model.ScanResponse
import java.util.concurrent.CompletableFuture

fun Route.cameras(cli: DynamoDBAsyncClient,
                  defaultMaxMins: Long,
                  videoTable: String,
                  detectionTable: String,
                  cameraTable: String) {
    get("/cameras") {
        val scanRequest: ScanRequest = ScanRequest.builder()
                .tableName(cameraTable)
                .build()
        val resp: CompletableFuture<List<Camera>> = cli.scan(scanRequest)
                .thenApply{ it.items() }
                .thenApply{ it.map { cameraFromDynamoItem(it) } }
        resp.await()
        call.respond(resp)
    }
    post("/cameras") {
        putDynamoItem<Camera>(call, cli, cameraTable)
    }
    videos(cli, defaultMaxMins, videoTable)
    detections(cli, defaultMaxMins, detectionTable)
}
