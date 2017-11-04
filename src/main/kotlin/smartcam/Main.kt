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
import org.jetbrains.ktor.application.log
import org.jetbrains.ktor.http.HttpStatusCode
import org.jetbrains.ktor.locations.*
import smartcam.util.buildDynamoQueryRequest

fun loadConfig(): Config = ConfigFactory.load()

@location("/")
class Root()

@location()
class DynamoVideos(val cameraId: String)

@location("/cameras/{cameraId}/detections")
class DynamoDetections(val cameraId: String)

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
    install(Locations)
    install(GsonSupport) {
        setPrettyPrinting()
    }
    install(Routing) {
        root()
        dynamoRangeQuery(Video, "/cameras/{cameraId}/videos", cli, defaultMaxMins, videoTable)
        dynamoRangeQuery(Detection, )
        dynamoVideosQuery(cli, defaultMaxMins, videoTable)
        dynamoDetectionsQuery(cli, defaultMaxMins, detectionTable)
        get("/cameras") {
            call.respondText("GET /cameras")
        }
//            // return list of object detections between `from`
//            // and `to` parameters (defined in unix epoch millseconds);
//            // if `from` and `to` not passed, return detections
//            // with start time between (now - defaultMaxMins) and now;
//            val queryRequest = buildDynamoQueryRequest(call, "time", defaultMaxMins, detectionTable)
//            try {
//                val resp = cli.query(queryRequest)
//                    .thenApply { it.items() }
//                    .thenApply{ it.map{ detectionfromDynamoItem(it) } }
//                resp.await()
//                call.respond(resp)
//            }
//            catch (e: Throwable) {
//                log.error("GET /cameras/{cameraId}/detections: $e")
//                call.respond(HttpStatusCode.InternalServerError)
//            }
//        }
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
            try {
                val resp: CompletableFuture<PutItemResponse> = cli.putItem(videoPutRequest)
                resp.await()
                call.respondText(resp.toString())
            } catch (e: Throwable) {
                log.error("GET /videos: $e")
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }
}

