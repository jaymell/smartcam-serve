package smartcam

import software.amazon.awssdk.services.dynamodb.model.AttributeValue

typealias DetectionObject = String

data class Detection(
    val camera_id: String,
    val time: Float,
    val detections: List<DetectionObject>
)

fun Detection.fromDynamoItem(item: Map<String, AttributeValue>): Detection =
    Detection(item.get("camera_id")!!.s(),
        item["time"]!!.n().toFloat(),
        item["detections"]!!.l().map { it.s() }
    )
