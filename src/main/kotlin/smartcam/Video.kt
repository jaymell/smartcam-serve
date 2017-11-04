package smartcam

import software.amazon.awssdk.services.dynamodb.model.AttributeValue

class Video(
    override val cameraId: String,
    val start: Float,
    val end: Float,
    val width: Int,
    val height: Int,
    val bucket: String,
    val key: String,
    val region: String
) : DynamoItem<Video> {
   override fun fromDynamoItem(item: Map<String, AttributeValue>): Video =
   Video(item.get("camera_id")!!.s(),
           item.get("start")!!.n().toFloat(),
           item.get("end")!!.n().toFloat(),
           item.get("width")!!.n().toInt(),
           item.get("height")!!.n().toInt(),
           item.get("bucket")!!.s(),
           item.get("key")!!.s(),
           item.get("region")!!.s()
   )
}

fun Video.toDynamoRecord(): HashMap<String, AttributeValue> =
    hashMapOf(
       "camera_id" to AttributeValue.builder().s(cameraId).build(),
        "start" to AttributeValue.builder().n(start.toString()).build(),
        "end" to AttributeValue.builder().n(end.toString()).build(),
        "width" to AttributeValue.builder().n(width.toString()).build(),
        "height" to AttributeValue.builder().n(height.toString()).build(),
        "bucket" to AttributeValue.builder().s(bucket).build(),
        "key" to AttributeValue.builder().s(key).build(),
        "region" to AttributeValue.builder().s(region).build()
    )


