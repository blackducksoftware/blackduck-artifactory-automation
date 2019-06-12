package artifactory

import com.synopsys.integration.exception.IntegrationException
import com.synopsys.integration.log.Slf4jIntLogger
import com.synopsys.integration.rest.HttpMethod
import com.synopsys.integration.rest.body.StringBodyContent
import org.apache.http.entity.ContentType
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URISyntaxException

class SystemApiService(private val artifactoryApiService: ArtifactoryApiService) {
    private val logger = Slf4jIntLogger(LoggerFactory.getLogger(this.javaClass))

    @Throws(IntegrationException::class, IOException::class, URISyntaxException::class)
    fun applyLicense(license: String) {
        val builder = artifactoryApiService.generateDefaultRequestBuilder(
            HttpMethod.POST,
            ContentType.APPLICATION_JSON,
            "/api/system/licenses"
        )
        val body = "{ \"licenseKey\": \"$license\" }"
        builder.bodyContent(StringBodyContent(body))
        val request = builder.build()
        artifactoryApiService.execute(request).use { response ->
            artifactoryApiService.validateResponse(request, response)
        }
    }

    @Throws(IOException::class, URISyntaxException::class, IntegrationException::class)
    fun pingArtifactory() {
        val request = artifactoryApiService.generateDefaultRequestBuilder(
            HttpMethod.GET,
            ContentType.TEXT_PLAIN,
            "/api/system/ping"
        ).build()
        artifactoryApiService.execute(request).use { response ->
            artifactoryApiService.validateResponse(request, response)
        }
    }

    @Throws(InterruptedException::class, IntegrationException::class)
    fun waitForSuccessfulStartup() {
        var attempts = 0
        val maxAttempts = 20
        while (attempts < maxAttempts) {
            try {
                pingArtifactory()
                break
            } catch (e: Exception) {
                logger.debug(e.message, e)
                logger.info("Waiting for Artifactory startup... Checking again in 10 seconds.")
                Thread.sleep(10000L)
            }

            attempts++
        }

        if (attempts >= maxAttempts) {
            throw IntegrationException("Waiting for startup timed out.")
        }
    }
}