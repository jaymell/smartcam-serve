package smartcam

import org.jetbrains.ktor.application.Application
import org.jetbrains.ktor.application.install
import org.jetbrains.ktor.features.DefaultHeaders
import org.jetbrains.ktor.response.*
import org.jetbrains.ktor.gson.*
import org.jetbrains.ktor.request.receive
import org.jetbrains.ktor.routing.Routing
import org.jetbrains.ktor.routing.get
import org.jetbrains.ktor.routing.post
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.*
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbQueryExpression
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse
import java.time.*
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine
import kotlinx.coroutines.experimental.future.await


fun Application.main() {

    val table = "smartcam-videos"
    val region = Region.US_WEST_2
    val cli: DynamoDBAsyncClient = DynamoDBAsyncClient.builder()
        .region(region)
        .build()

    install(DefaultHeaders)
    install(GsonSupport) {
        setPrettyPrinting()
    }
    install(Routing) {

        get("/") {
            call.respondText("smartcam")
        }
        get("/cameras") {
            call.respondText("GET /cameras")
        }
        get("/camera/{cameraId}/videos") {
            // if no params passed, return for list 15 minutes?
            // take start and end as unix ms timestamp for range
            // to get videos from
            // query dynamodb given that
            // return results

//  		    val now = System.currentTimeMillis()
            val nowUtc = ZonedDateTime.now(ZoneOffset.UTC)
            val fiveDaysAgo = nowUtc.minusDays(50)
            val minTime = fiveDaysAgo.toInstant().toEpochMilli();
            val cameraId = call.parameters["cameraId"]
            val eav = hashMapOf(
               ":val1" to AttributeValue.builder().s(cameraId).build(),
                ":val2" to AttributeValue.builder().n(minTime.toString()).build())
//            val queryExpression = DynamoDbQueryExpression<Video>()
//                    .withKeyConditionExpression("camera_id = :val1 and time >= :val2")
//                    .withExpressionAttributeValues(eav)
            val queryRequest: QueryRequest = QueryRequest.builder()
                    .tableName(table)
                    .expressionAttributeValues(eav)
                    .keyConditionExpression("camera_id = :val1 and time >= :val2")
                    .build()

            val resp = cli.query(queryRequest)
            resp.await()
            call.respond(resp)
        }
        post("/videos") {
            // receive item, validate
            // it deserializes properly
            // insert into dynamo
            // if error, return 500
            // else return 200
            val video = call.receive<Video>()
            val videoItem = video.toDynamoRecord()
            val videoPutRequest: PutItemRequest = PutItemRequest.builder()
                .tableName(table)
                .item(videoItem)
                .build()
            val resp: CompletableFuture<PutItemResponse> = cli.putItem(videoPutRequest)
            resp.await()
            call.respondText(resp.toString())
        }
    }
}

