package smartcam

import software.amazon.awssdk.services.dynamodb.model.AttributeValue

interface DynamoClass {
    fun toDynamoRecord(): HashMap<String, AttributeValue>
}
