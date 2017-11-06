package smartcam

import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonCreator

class Video {
    var camera_id: String
    var start: Float
    var end: Float
    var width: Int
    var height: Int
    var bucket: String
    var key: String
    var region: String
    constructor(
        @JsonProperty(value = "camera_id", required=true) camera_id: String,
        @JsonProperty(value = "start", required=true) start: Float,
        @JsonProperty(value = "end", required=true) end: Float,
        @JsonProperty(value = "width", required=true) width: Int,
        @JsonProperty(value = "height", required=true) height: Int,
        @JsonProperty(value = "bucket", required=true) bucket: String,
        @JsonProperty(value = "key", required=true) key: String,
        @JsonProperty(value = "region", required=true) region: String) {
        this.camera_id = camera_id
        this.start = start
        this.end = end
        this.width = width
        this.height = height
        this.bucket = bucket
        this.key = key
        this.region = region
    }
}


fun Video.toDynamoRecord(): HashMap<String, AttributeValue> =
    hashMapOf(
       "camera_id" to AttributeValue.builder().s(camera_id).build(),
        "start" to AttributeValue.builder().n(start.toString()).build(),
        "end" to AttributeValue.builder().n(end.toString()).build(),
        "width" to AttributeValue.builder().n(width.toString()).build(),
        "height" to AttributeValue.builder().n(height.toString()).build(),
        "bucket" to AttributeValue.builder().s(bucket).build(),
        "key" to AttributeValue.builder().s(key).build(),
        "region" to AttributeValue.builder().s(region).build()
    )

fun videoFromDynamoItem(item: Map<String, AttributeValue>): Video =
   Video(item.get("camera_id")!!.s(),
           item.get("start")!!.n().toFloat(),
           item.get("end")!!.n().toFloat(),
           item.get("width")!!.n().toInt(),
           item.get("height")!!.n().toInt(),
           item.get("bucket")!!.s(),
           item.get("key")!!.s(),
           item.get("region")!!.s()
   )
