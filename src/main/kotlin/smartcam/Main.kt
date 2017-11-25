package smartcam

import org.jetbrains.ktor.application.Application
import org.jetbrains.ktor.application.install
import org.jetbrains.ktor.features.DefaultHeaders
import org.jetbrains.ktor.response.*
import org.jetbrains.ktor.gson.*
import org.jetbrains.ktor.routing.Routing
import org.jetbrains.ktor.routing.get
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.*
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.ktor.features.CORS
import com.amazonaws.services.s3.*


fun loadConfig(): Config = ConfigFactory.load()

fun Application.main() {

    val config = loadConfig()
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

    install(DefaultHeaders)
    install(GsonSupport) {
        setPrettyPrinting()
        disableHtmlEscaping()
    }
    install(CORS) {
        anyHost()
    }
    install(Routing) {
        get("/") {
            call.respondText("smartcam")
        }
        cameras(dynamoCli, s3Cli, defaultMaxMins, videoTable, detectionTable, cameraTable)
    }
}
