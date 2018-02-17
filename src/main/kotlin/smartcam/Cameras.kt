package smartcam

import io.ktor.application.call
import kotlinx.coroutines.experimental.future.await
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import smartcam.util.putDynamoItem
import software.amazon.awssdk.services.dynamodb.DynamoDBAsyncClient
import software.amazon.awssdk.services.dynamodb.model.ScanRequest
import java.util.concurrent.CompletableFuture

fun Route.cameras(dynamoCli: DynamoDBAsyncClient,
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
}
