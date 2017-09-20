package smartcam

import org.jetbrains.ktor.application.Application
import org.jetbrains.ktor.application.install
import org.jetbrains.ktor.features.DefaultHeaders
import org.jetbrains.ktor.response.respondText
import org.jetbrains.ktor.routing.Routing
import org.jetbrains.ktor.routing.get


fun Application.main() {
    install(DefaultHeaders)
    install(Routing) {
        get("/") {
            call.respondText("smartcam")
        }
    }
}