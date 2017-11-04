package smartcam.util

import org.jetbrains.ktor.application.ApplicationCall
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import java.time.*

fun buildDynamoQueryRequest(call: ApplicationCall, pKey: String?, fromParam: String?, toParam: String?, sortKey: String, defaultMaxMins: Long, tableName: String)
    : QueryRequest {
    val nowUtc = ZonedDateTime.now(ZoneOffset.UTC)
    val fromTime = fromParam ?: nowUtc.minusMinutes(defaultMaxMins).toInstant().toEpochMilli()
    val toTime = toParam ?: nowUtc.toInstant().toEpochMilli()
    val eav = hashMapOf(
       ":val1" to AttributeValue.builder().s(pKey).build(),
        ":val2" to AttributeValue.builder().n(fromTime.toString()).build(),
        ":val3" to AttributeValue.builder().n(toTime.toString()).build())
    val ean = hashMapOf(
        "#s" to sortKey)
    return QueryRequest.builder()
       .tableName(tableName)
       .expressionAttributeValues(eav)
       .expressionAttributeNames(ean)
       .keyConditionExpression(
           "camera_id = :val1 and #s BETWEEN :val2 and :val3")
       .build()
}