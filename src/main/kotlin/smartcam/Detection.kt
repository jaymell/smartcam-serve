package smartcam

import software.amazon.awssdk.services.dynamodb.model.AttributeValue

data class DetectionObject(val name: String)

data class Detection(
    val camera_id: String,
    val time: Float,
    val detections: Set<DetectionObject>
)

/*
fun detectionfromDynamoItem(item: Map<String, AttributeValue>): Detection =
    Detection(item.get("camera_id")!!.s(),
        item.get("time")!!.n().toFloat()
        item.get("detections")!!.
    )
*/