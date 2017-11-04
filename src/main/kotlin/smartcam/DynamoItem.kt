package smartcam

import software.amazon.awssdk.services.dynamodb.model.AttributeValue

interface DynamoItem<T> {
    val cameraId: String
    fun fromDynamoItem(item: Map<String, AttributeValue>): T
}
