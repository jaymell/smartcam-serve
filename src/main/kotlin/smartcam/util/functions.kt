package smartcam.util

import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import java.time.*

fun buildDynamoQueryRequest(pKey: String,
                            from: String?,
                            to: String?,
                            sortKey: String,
                            defaultMaxMins: Long,
                            tableName: String)
    : QueryRequest {
    val nowUtc = ZonedDateTime.now(ZoneOffset.UTC)
    val fromTime = from ?: nowUtc.minusMinutes(defaultMaxMins).toInstant().toEpochMilli()
    val toTime = to ?: nowUtc.toInstant().toEpochMilli()
    val eav = hashMapOf(
       ":val1" to AttributeValue.builder().s(pKey).build(),
        ":val2" to AttributeValue.builder().n(fromTime.toString()).build(),
        ":val3" to AttributeValue.builder().n(toTime.toString()).build())
    val queryRequest: QueryRequest = QueryRequest.builder()
       .tableName(tableName)
       .expressionAttributeValues(eav)
       .expressionAttributeNames(hashMapOf("#s" to sortKey, "#p" to pKey))
       .keyConditionExpression(
           "#p = :val1 and #s BETWEEN :val2 and :val3")
       .build()
    return queryRequest
}