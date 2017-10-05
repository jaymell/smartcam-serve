package smartcam

import org.jetbrains.ktor.application.Application
import org.jetbrains.ktor.application.install
import org.jetbrains.ktor.features.DefaultHeaders
import org.jetbrains.ktor.response.*
import org.jetbrains.ktor.gson.*
import org.jetbrains.ktor.request.receive
import org.jetbrains.ktor.request.receiveParameters
import org.jetbrains.ktor.routing.Routing
import org.jetbrains.ktor.routing.get
import org.jetbrains.ktor.routing.post
import org.jetbrains.ktor.util.cast


fun Application.main() {
    install(DefaultHeaders)
    install(GsonSupport) {
      setPrettyPrinting()
    }
    install(Routing) {
        get("/") {
            call.respondText("smartcam")
        }
        post("/videos") {
            val stuff = call.receive<Video>()
            println(stuff)
            call.respond(stuff)
        }
    }
}

