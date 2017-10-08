package smartcam

import org.jetbrains.ktor.application.Application
import org.jetbrains.ktor.application.install
import org.jetbrains.ktor.features.DefaultHeaders
import org.jetbrains.ktor.response.*
import org.jetbrains.ktor.gson.*
import org.jetbrains.ktor.request.receive
//import org.jetbrains.ktor.request.receiveParameters
import org.jetbrains.ktor.routing.Routing
import org.jetbrains.ktor.routing.get
import org.jetbrains.ktor.routing.post
import software.amazon.awssdk.regions.Region
//import org.jetbrains.ktor.util.cast
import software.amazon.awssdk.services.dynamodb.*
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse
import java.util.concurrent.CompletableFuture

fun Application.main() {
    val table = "smartcam-videos"
    val region = Region.US_WEST_2

    install(DefaultHeaders)
    install(GsonSupport) {
        setPrettyPrinting()
    }
    install(Routing) {
        get("/") {
            call.respondText("smartcam")
        }
        get("/videos") {
            call.respondText("GET /videos")
            // if no params passed, return for list 15 minutes?
            // take start and end as unix ms timestamp for range
            // to get videos from
            // query dynamodb given that
            // return results
        }
        post("/videos") {
            // receive item, validate
            // it deserializes properly
            // insert into dynamo
            // if error, return 500
            // else return 200
            val video = call.receive<Video>()
            val cli: DynamoDBAsyncClient = DynamoDBAsyncClient.builder()
                .region(region)
                .build()
            val videoItem = video.toDynamoRecord()
            val videoPutRequest: PutItemRequest = PutItemRequest.builder()
                .tableName(table)
                .item(videoItem)
                .build()
            val resp: CompletableFuture<PutItemResponse> = cli.putItem(videoPutRequest)
            resp.get()
            call.respondText("done")
        }
    }
}

