package artifactory.api

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.isClientError
import com.github.kittinunf.fuel.core.isServerError
import com.synopsys.integration.exception.IntegrationException

abstract class ArtifactoryApiService(
    private val artifactoryUser: ArtifactoryUser,
    private val baseUrl: String
) {
    init {
        FuelManager.instance.basePath = "$baseUrl/artifactory"
    }

    fun Request.authenticate(): Request {
        return this.authentication().basic(artifactoryUser.username, artifactoryUser.password)
    }
}

fun com.github.kittinunf.fuel.core.Response.validate(): com.github.kittinunf.fuel.core.Response {
    if (this.isClientError || this.isServerError || this.statusCode < 0) {
        throw IntegrationException("Status Code: ${this.statusCode}, Message: ${this.responseMessage}")
    }
    return this
}

data class ArtifactoryUser(val username: String, val password: String)