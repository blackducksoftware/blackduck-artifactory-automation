package artifactory.api

import com.github.kittinunf.fuel.core.*
import com.github.kittinunf.fuel.core.extensions.authentication
import com.synopsys.integration.exception.IntegrationException

abstract class ArtifactoryApiService(
    internal val fuelManager: FuelManager,
    private val artifactoryUser: ArtifactoryUser
) {
    fun Request.authenticate(): Request {
        return this.authentication().basic(artifactoryUser.username, artifactoryUser.password)
    }
}

fun Response.validate(): Response {
    if (this.isClientError || this.isServerError || this.statusCode < 0) {
        throw IntegrationException("Status Code: ${this.statusCode}, Message: ${this.responseMessage}")
    }
    return this
}

data class ArtifactoryUser(val username: String, val password: String)