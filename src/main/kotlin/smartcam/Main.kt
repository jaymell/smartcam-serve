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
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

fun loadConfig(): Config = ConfigFactory.load()

fun Application.main() {

    val config = loadConfig()
    val videoTable = config.getString("smartcamServe.videoTable")
    val detectionTable = config.getString("smartcamServe.detectionTable")
    val region = Region.of(config.getString("smartcamServe.region"))
    val defaultMaxMins = config.getLong("smartcamServe.defaultQueryMaxMins")
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
        get("/cameras/{cameraId}/videos") {
            // return list of videos between `from`
            // and `to` parameters (defined in unix epoch millseconds);
            // if `from` and `to` not passed, return videos
            // with start time between (now - defaultMaxMins) and now;
            val cameraId = call.parameters["cameraId"]
            val fromParam = call.parameters["from"]
            val toParam = call.parameters["to"]
            val nowUtc = ZonedDateTime.now(ZoneOffset.UTC)
            val fromTime = fromParam ?: nowUtc.minusMinutes(defaultMaxMins).toInstant().toEpochMilli()
            val toTime = toParam ?: nowUtc.toInstant().toEpochMilli()
            val eav = hashMapOf(
               ":val1" to AttributeValue.builder().s(cameraId).build(),
                ":val2" to AttributeValue.builder().n(fromTime.toString()).build(),
                ":val3" to AttributeValue.builder().n(toTime.toString()).build())
            val ean = hashMapOf(
                "#s" to "start")
            val queryRequest: QueryRequest = QueryRequest.builder()
                .tableName(videoTable)
                .expressionAttributeValues(eav)
                .expressionAttributeNames(ean)
                .keyConditionExpression(
                    "camera_id = :val1 and #s BETWEEN :val2 and :val3")
                .build()
            val resp = cli.query(queryRequest)
                .thenApply { it.items() }
                .thenApply{ it.map{ videoFromDynamoItem(it) } }
            resp.await()
            call.respond(resp)
        }
        get("/cameras/{cameraId}/detections") {
            // return list of object detections between `from`
            // and `to` parameters (defined in unix epoch millseconds);
            // if `from` and `to` not passed, return detections
            // with start time between (now - defaultMaxMins) and now;
            val cameraId = call.parameters["cameraId"]
            val fromParam = call.parameters["from"]
            val toParam = call.parameters["to"]
            val nowUtc = ZonedDateTime.now(ZoneOffset.UTC)
            val fromTime = fromParam ?: nowUtc.minusMinutes(defaultMaxMins).toInstant().toEpochMilli()
            val toTime = toParam ?: nowUtc.toInstant().toEpochMilli()
            val eav = hashMapOf(
               ":val1" to AttributeValue.builder().s(cameraId).build(),
                ":val2" to AttributeValue.builder().n(fromTime.toString()).build(),
                ":val3" to AttributeValue.builder().n(toTime.toString()).build())
             val ean = hashMapOf(
                "#t" to "time")
             val queryRequest: QueryRequest = QueryRequest.builder()
                .tableName(detectionTable)
                .expressionAttributeValues(eav)
                .expressionAttributeNames(ean)
                .keyConditionExpression(
                    "camera_id = :val1 and #t BETWEEN :val2 and :val3")
                .build()
            val resp = cli.query(queryRequest)
//                .thenApply { it.items() }
//                .thenApply{ it.map{ detectionFromDynamoItem(it) } }
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
                .tableName(videoTable)
                .item(videoItem)
                .build()
            val resp: CompletableFuture<PutItemResponse> = cli.putItem(videoPutRequest)
            resp.await()
            call.respondText(resp.toString())
        }
    }
}

