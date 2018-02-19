package smartcam

import software.amazon.awssdk.services.dynamodb.model.AttributeValue

data class VideoUploadResponse(val bucket: String, val key: String, val region: String)

data class Video(
        val camera_id: String,
        val start: Double,
        val end: Double,
        val width: Int,
        val height: Int,
        val bucket: String,
        val key: String,
        val region: String,
        var url: String?
) : DynamoClass {

    override fun toDynamoRecord(): HashMap<String, AttributeValue> =
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
}


fun videoFromDynamoItem(item: Map<String, AttributeValue>, url: String): Video =
        Video(item.get("camera_id")!!.s(),
                item.get("start")!!.n().toDouble(),
                item.get("end")!!.n().toDouble(),
                item.get("width")!!.n().toInt(),
                item.get("height")!!.n().toInt(),
                item.get("bucket")!!.s(),
                item.get("key")!!.s(),
                item.get("region")!!.s(),
                url
        )
