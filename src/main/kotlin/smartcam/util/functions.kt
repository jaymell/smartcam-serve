package smartcam.util

import com.amazonaws.HttpMethod
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.experimental.future.await
import org.jetbrains.ktor.application.ApplicationCall
import org.jetbrains.ktor.http.HttpStatusCode
import org.jetbrains.ktor.request.receiveText
import org.jetbrains.ktor.response.respond
import org.jetbrains.ktor.response.respondText
import smartcam.DynamoClass
import software.amazon.awssdk.services.dynamodb.DynamoDBAsyncClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import java.net.URL
import java.time.*
import java.util.concurrent.CompletableFuture

fun buildDynamoQueryRequest(pKeyName: String,
                            pKeyValue: String?,
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
       ":val1" to AttributeValue.builder().s(pKeyValue).build(),
        ":val2" to AttributeValue.builder().n(fromTime.toString()).build(),
        ":val3" to AttributeValue.builder().n(toTime.toString()).build())
    val queryRequest: QueryRequest = QueryRequest.builder()
       .tableName(tableName)
       .expressionAttributeValues(eav)
       .expressionAttributeNames(hashMapOf("#s" to sortKey, "#p" to pKeyName))
       .keyConditionExpression(
           "#p = :val1 and #s BETWEEN :val2 and :val3")
       .build()
    return queryRequest
}

inline suspend fun <reified T: DynamoClass> putDynamoItem(call: ApplicationCall,
                                                          cli: DynamoDBAsyncClient,
                                                          table: String) {
    try {
        val mapper = jacksonObjectMapper()
        val rawText = call.receiveText()
        val obj = mapper.readValue<T>(rawText)
        val dynamoItem = obj.toDynamoRecord()
        val putRequest: PutItemRequest = PutItemRequest.builder()
            .tableName(table)
            .item(dynamoItem)
            .build()
        // nullable b/c of mockito:
        val resp: CompletableFuture<PutItemResponse>? = cli.putItem(putRequest)
        resp?.await()
        call.respondText(resp.toString())
    } catch (e: MissingKotlinParameterException) {
        call.respond(HttpStatusCode.BadRequest)
    } catch (e: Exception) {
        // FIXME:
        System.err.println("ERROR putting item: $e")
        call.respond(HttpStatusCode.InternalServerError)
    }
}

fun getSignedS3Url(s3client: AmazonS3, bucketName: String, objectKey: String): URL {
    val expiration = java.util.Date()
    var milliSeconds = expiration.time
    milliSeconds += (1000 * 60 * 60).toLong() // Add 1 hour.
    expiration.time = milliSeconds

    val generatePresignedUrlRequest = GeneratePresignedUrlRequest(bucketName, objectKey)
    generatePresignedUrlRequest.setMethod(HttpMethod.GET)
    generatePresignedUrlRequest.setExpiration(expiration)

    return s3client.generatePresignedUrl(generatePresignedUrlRequest)
}