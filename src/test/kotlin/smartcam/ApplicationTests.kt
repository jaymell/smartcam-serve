package smartcam

import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.http.*
import org.jetbrains.ktor.testing.*
//import org.jetbrains.ktor.tests.*
import kotlin.test.*
//import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.ShouldSpec


//class GetRoot : ShouldSpec() {
//    init {
//        should("return something worthless") {
//           withTestApplication(Application::main) {
//               with(handleRequest(HttpMethod.Get, "/")) {
//                   assertEquals(HttpStatusCode.OK, response.status())
//                   assertEquals("smartcam", response.content)
//               }
//           }
//        }
//    }
//}
//
//class GetVideos : ShouldSpec() {
//     init {
//        should("do something cool if cameraId isn't a number") {
//           withTestApplication(Application::main) {
//               with(handleRequest(HttpMethod.Get, "/cameras/one/videos")) {
//                   assertEquals(HttpStatusCode.OK, response.status())
//               }
//           }
//        }
//    }
//}
