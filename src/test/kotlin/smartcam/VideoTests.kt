package smartcam

import io.kotlintest.matchers.shouldThrow
import io.kotlintest.specs.ShouldSpec
import com.fasterxml.jackson.module.kotlin.*

class TestVideoCreation : ShouldSpec() {
    init {
        should("do something") {
            val mapper = jacksonObjectMapper()
            val rawString = """
                       {"camera_id": "1",
                        "start": 1509228056000.0,
                        "width": 1,
                        "height": 2,
                        "bucket": "testbucket",
                        "key": "testkey",
                        "region": "testregion" }"""
            val myVideo = mapper.readValue<Video>(rawString)
            System.err.println("myVideo: ${myVideo}")
        }
    }
}