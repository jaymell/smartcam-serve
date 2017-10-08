package smartcam

import software.amazon.awssdk.services.dynamodb.model.AttributeValue

data class Video(
    val camera_id: String,
    val start: Float,
    val end: Float,
    val width: Int,
    val height: Int,
    val bucket: String,
    val key: String,
    val region: String
)

fun Video.toDynamoRecord(): HashMap<String, AttributeValue> {
    return hashMapOf(
       "camera_id" to AttributeValue.builder().s(camera_id).build(),
        "time" to AttributeValue.builder().n(start.toString()).build(),
        "end" to AttributeValue.builder().n(end.toString()).build(),
        "width" to AttributeValue.builder().n(width.toString()).build(),
        "height" to AttributeValue.builder().n(height.toString()).build(),
        "bucket" to AttributeValue.builder().s(bucket).build(),
        "key" to AttributeValue.builder().s(key).build(),
        "region" to AttributeValue.builder().s(region).build()
    )
}
