package smartcam

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.DefaultHeaders
import io.ktor.response.*
import io.ktor.gson.*
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.application.call
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.*
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.features.CORS
import com.amazonaws.services.s3.*
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation


fun loadConfig(): Config = ConfigFactory.load()

fun Application.main() {

    val config = loadConfig()
    val videoBucket = config.getString("smartcamServe.videoBucket")
    val videoTable = config.getString("smartcamServe.videoTable")
    val detectionTable = config.getString("smartcamServe.detectionTable")
    val cameraTable = config.getString("smartcamServe.cameraTable")
    val region = Region.of(config.getString("smartcamServe.region"))
    val defaultMaxMins = config.getLong("smartcamServe.defaultQueryMaxMins")
    val dynamoCli: DynamoDBAsyncClient = DynamoDBAsyncClient.builder()
            .region(region)
            .build()
    val regionString: String = config.getString("smartcamServe.region")
    val s3Cli = AmazonS3ClientBuilder.standard()
            .withRegion(regionString)
            .build()

    val xm = TransferManagerBuilder.standard()
            .withS3Client(s3Cli)
            .build()

    install(DefaultHeaders)
    install(CallLogging)
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
            disableHtmlEscaping()
        }
    }
    install(CORS) {
        anyHost()
    }
    install(Routing) {
        get("/") {
            call.respondText("smartcam")
        }
        cameras(dynamoCli, cameraTable)
        videos(dynamoCli, s3Cli, xm, defaultMaxMins, videoBucket, videoTable)
        detections(dynamoCli, defaultMaxMins, detectionTable)
    }
}
