package smartcam

import software.amazon.awssdk.services.dynamodb.model.AttributeValue

data class Camera(val camera_id: String) : DynamoClass {
    override fun toDynamoRecord(): HashMap<String, AttributeValue> =
        hashMapOf(
            "camera_id" to AttributeValue.builder().s(camera_id).build()
        )
}

