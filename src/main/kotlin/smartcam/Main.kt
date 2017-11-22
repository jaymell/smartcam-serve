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
import org.jetbrains.ktor.features.CORS


fun loadConfig(): Config = ConfigFactory.load()

fun Application.main() {

    val config = loadConfig()
    val videoTable = config.getString("smartcamServe.videoTable")
    val detectionTable = config.getString("smartcamServe.detectionTable")
    val cameraTable = config.getString("smartcamServe.cameraTable")
    val region = Region.of(config.getString("smartcamServe.region"))
    val defaultMaxMins = config.getLong("smartcamServe.defaultQueryMaxMins")
    val cli: DynamoDBAsyncClient = DynamoDBAsyncClient.builder()
        .region(region)
        .build()

    install(DefaultHeaders)
    install(GsonSupport) {
        setPrettyPrinting()
    }
    install(CORS) {
        anyHost()
    }
    install(Routing) {
        get("/") {
            call.respondText("smartcam")
        }
        cameras(cli, defaultMaxMins, videoTable, detectionTable, cameraTable)
    }
}
