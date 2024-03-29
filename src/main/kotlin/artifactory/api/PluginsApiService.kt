package artifactory.api

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.success
import com.synopsys.integration.log.Slf4jIntLogger
import org.slf4j.LoggerFactory

class PluginsApiService(fuelManager: FuelManager) : ArtifactoryApiService(fuelManager) {
    private val logger = Slf4jIntLogger(LoggerFactory.getLogger(javaClass))

    fun reloadPlugins(): Response {
        return fuelManager.post("/api/plugins/reload")
            .response { response ->
                response.failure { logger.warn(it.exception.message) }
                response.success { logger.info("Artifactory successfully reloaded plugins.") }
            }
            .join()
            .validate()
    }
}