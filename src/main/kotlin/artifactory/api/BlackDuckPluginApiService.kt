package artifactory.api

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Response

/**
 * An API service for all the endpoints created by the plugin.
 */
class BlackDuckPluginApiService(fuelManager: FuelManager) : ArtifactoryApiService(fuelManager) {
    fun reloadPlugin(): Response {
        return fuelManager.post("/api/plugins/execute/blackDuckReload")
            .response()
            .second
            .validate()
    }

    fun blackDuckInitializeRepositories(): Response {
        return fuelManager.post("/api/plugins/execute/blackDuckInitializeRepositories")
            .response()
            .second
            .validate()
    }
}