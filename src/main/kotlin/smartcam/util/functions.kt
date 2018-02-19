package smartcam.util

import com.amazonaws.HttpMethod
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.CopyObjectRequest
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.transfer.TransferManager
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.experimental.future.await
import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.response.respondText
import kotlinx.coroutines.experimental.async
import smartcam.DynamoClass
import software.amazon.awssdk.services.dynamodb.DynamoDBAsyncClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
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
            .scanIndexForward(false)
            .build()
    return queryRequest
}

suspend inline fun <reified T : DynamoClass> putDynamoItem(obj: T,
                                                           cli: DynamoDBAsyncClient,
                                                           table: String): PutItemResponse? {
    val dynamoItem = obj.toDynamoRecord()
    val putRequest: PutItemRequest = PutItemRequest.builder()
            .tableName(table)
            .item(dynamoItem)
            .build()
    // nullable b/c of mockito:
    val resp: CompletableFuture<PutItemResponse>? = cli.putItem(putRequest)
    return resp?.await()
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

suspend fun putS3Object(s3Client: TransferManager, bucket: String, key: String, f: File,
                        userMetadata: MutableMap<String, String> = hashMapOf(), contentType: String? = null) {
    val metadata = ObjectMetadata()
    metadata.contentLength = f.length()
    userMetadata.forEach { k, v ->
        metadata.addUserMetadata(k, v)
    }
    contentType.let {
        metadata.setContentType(contentType)
    }
    val r = s3Client.upload(
            PutObjectRequest(bucket, key, FileInputStream(f), metadata))
    async { r.waitForCompletion() }.await()
}

/*
suspend fun moveS3Object(s3Client: AmazonS3, bucketName: String, srcKey: String, destKey: String) {
    // blocking code, but not yet practical to use aws sd2 v2 client:
    s3Client.copyObject(CopyObjectRequest(bucketName, srcKey, bucketName, destKey))
    s3Client.deleteObject(bucketName, srcKey)
}
*/

fun File.copyInputStreamToFile(inputStream: InputStream) {
    inputStream.use { input ->
        this.outputStream().use { fileOut ->
            input.copyTo(fileOut)
        }
    }
}
