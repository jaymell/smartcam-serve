package smartcam

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import io.ktor.application.call
import kotlinx.coroutines.experimental.future.await
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import smartcam.util.putDynamoItem
import software.amazon.awssdk.services.dynamodb.DynamoDBAsyncClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.ScanRequest
import software.amazon.awssdk.services.dynamodb.model.ScanResponse
import java.util.concurrent.CompletableFuture

fun Route.cameras(dynamoCli: DynamoDBAsyncClient,
                  s3Cli: AmazonS3,
                  defaultMaxMins: Long,
                  videoTable: String,
                  detectionTable: String,
                  cameraTable: String) {
    get("/cameras") {
        val scanRequest: ScanRequest = ScanRequest.builder()
                .tableName(cameraTable)
                .build()
        val resp: CompletableFuture<List<Camera>> = dynamoCli.scan(scanRequest)
                .thenApply{ it.items() }
                .thenApply{ it.map { cameraFromDynamoItem(it) } }
        resp.await()
        call.respond(resp)
    }
    post("/cameras") {
        putDynamoItem<Camera>(call, dynamoCli, cameraTable)
    }
    videos(dynamoCli, s3Cli, defaultMaxMins, videoTable)
    detections(dynamoCli, defaultMaxMins, detectionTable)

}
