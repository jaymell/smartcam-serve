package smartcam

import com.fasterxml.jackson.module.kotlin.*
import io.kotlintest.specs.ShouldSpec
import io.kotlintest.matchers.shouldThrow

class TestVideoCreation : ShouldSpec() {
    init {
        val mapper = jacksonObjectMapper()
        should("missing string parameter should throw") {
             val rawString = """
                {"camera_id": "1",
                    "start": 1509228056000.0,
                    "end": 1509228056001.0,
                    "width": 1,
                    "height": 2,
                    "bucket": "testbucket",
                    "region": "testregion" }"""
            shouldThrow<MissingKotlinParameterException> {
                mapper.readValue<Video>(rawString)
            }
        }
//        should("missing float parameter should throw") {
//            val rawString = """
//                {"camera_id": "1",
//                    "end": 1509228056001.0,
//                    "width": 1,
//                    "height": 2,
//                    "bucket": "testbucket",
//                    "key": "testkey",
//                    "region": "testregion" }"""
//            shouldThrow<MissingKotlinParameterException> {
//                mapper.readValue<Video>(rawString)
//            }
//        }
    }
}