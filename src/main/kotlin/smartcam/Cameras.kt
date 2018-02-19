package smartcam

import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
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
                .thenApply { it.items() }
                .thenApply { it.map { cameraFromDynamoItem(it) } }
        resp.await()
        call.respond(resp)
    }
    post("/cameras") {
        try {
            val mapper = jacksonObjectMapper()
            val rawText = call.receiveText()
            val obj = mapper.readValue<Camera>(rawText)
            putDynamoItem<Camera>(obj, dynamoCli, cameraTable)
            call.respond(HttpStatusCode.OK)
        } catch (e: MissingKotlinParameterException) {
            call.respond(HttpStatusCode.BadRequest)
        } catch (e: Exception) {
            System.err.println("ERROR putting item: $e")
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}
