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
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.experimental.future.await
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import smartcam.util.buildDynamoQueryRequest

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
            val queryRequest = buildDynamoQueryRequest(call, "start", defaultMaxMins, videoTable)
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
            val queryRequest = buildDynamoQueryRequest(call, "time", defaultMaxMins, detectionTable)
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

