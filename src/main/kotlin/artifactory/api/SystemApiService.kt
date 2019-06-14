package artifactory.api

import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.success
import com.synopsys.integration.exception.IntegrationException
import com.synopsys.integration.log.Slf4jIntLogger
import org.slf4j.LoggerFactory

class SystemApiService(artifactoryUser: ArtifactoryUser, baseUrl: String) : ArtifactoryApiService(artifactoryUser, baseUrl) {
    private val logger = Slf4jIntLogger(LoggerFactory.getLogger(this.javaClass))

    fun applyLicense(license: String): Response {
        return "/api/system/licenses".httpPost()
            .authenticate()
            .jsonBody("{ \"licenseKey\": \"$license\" }")
            .response { response ->
                response.failure { logger.warn(it.exception.message) }
                response.success { logger.info("Successfully applied license") }
            }
            .join()
            .validate()
    }

    fun pingArtifactory(): Response {
        return "/api/system/ping".httpGet()
            .response { response ->
                response.failure { logger.warn(it.exception.message) }
                response.success { logger.info("Artifactory successfully started.") }
            }
            .join()
            .validate()
    }

    fun waitForSuccessfulStartup() {
        var attempts = 0
        val maxAttempts = 20
        while (attempts < maxAttempts) {
            try {
                logger.info("Waiting for Artifactory startup... Checking again in 5 seconds.")
                Thread.sleep(5000L)
                pingArtifactory()
                break
            } catch (e: Exception) {
                // Ignored
            }
            attempts++
        }

        if (attempts >= maxAttempts) {
            throw IntegrationException("Waiting for startup timed out.")
        }
    }
}