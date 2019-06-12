package artifactory

import com.synopsys.integration.exception.IntegrationException
import com.synopsys.integration.log.Slf4jIntLogger
import com.synopsys.integration.rest.HttpMethod
import com.synopsys.integration.rest.client.IntHttpClient
import com.synopsys.integration.rest.request.Request
import com.synopsys.integration.rest.request.Response
import org.apache.commons.io.IOUtils
import org.apache.http.entity.ContentType
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*

class ArtifactoryApiService(
    private val httpClient: IntHttpClient,
    private val artifactoryUser: ArtifactoryUser,
    private val url: URL
) {
    private val logger = Slf4jIntLogger(LoggerFactory.getLogger(javaClass))

    @Throws(IntegrationException::class)
    fun execute(request: Request): Response {
        return httpClient.execute(request)
    }

    @Throws(URISyntaxException::class, MalformedURLException::class)
    fun generateDefaultRequestBuilder(
        httpMethod: HttpMethod = HttpMethod.GET,
        contentType: ContentType = ContentType.APPLICATION_JSON,
        path: String
    ): Request.Builder {
        val builder = Request.Builder()
        builder.method(httpMethod)

        var finalPath = "/artifactory"
        if (!path.startsWith("/")) {
            finalPath += "/"
        }
        finalPath += path

        val endpoint = URL(url, finalPath)
        builder.uri(endpoint.toURI().toString())
        builder.mimeType(contentType.mimeType)
        builder.addAdditionalHeader("Authorization", "Basic ${artifactoryUser.getEncodedCredentials()}")
        return builder
    }

    @Throws(IntegrationException::class, IOException::class)
    fun validateResponse(request: Request, response: Response) {
        if (response.isStatusCodeError!!) {
            val content = IOUtils.toString(response.content, StandardCharsets.UTF_8)
            throw IntegrationException("{$request.uri} responded with a status code of ${response.statusCode}: $content")
        }
    }
}

data class ArtifactoryUser(val username: String, val password: String) {
    fun getEncodedCredentials(): String {
        val credentials = "$username:$password"
        return Base64.getEncoder().encodeToString(credentials.toByteArray())
    }
}