import artifactory.api.ArtifactoryUser
import artifactory.api.PluginsApiService
import artifactory.api.SystemApiService
import com.github.kittinunf.fuel.core.FuelManager
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfigBuilder
import com.synopsys.integration.exception.IntegrationException
import com.synopsys.integration.log.IntLogger
import com.synopsys.integration.log.Slf4jIntLogger
import docker.DockerService
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import plugin.BlackDuckPluginService
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

fun main() {
    val dockerService = DockerService()
    val configManager = ConfigManager()

    Application(
        dockerService,
        configManager.getOrDefault(Config.ARTIFACTORY_BASEURL, "http://localhost"),
        configManager.getOrDefault(Config.ARTIFACTORY_PORT, "8081"),
        configManager.getOrDefault(Config.ARTIFACTORY_USERNAME, "admin"),
        configManager.getOrDefault(Config.ARTIFACTORY_PASSWORD, "password"),
        configManager.getOrDefault(Config.ARTIFACTORY_VERSION, "latest"),
        File(configManager.getOrDefault(Config.ARTIFACTORY_LICENSE_PATH, "")),
        configManager.getOrThrow(Config.BLACKDUCK_URL),
        configManager.getOrDefault(Config.BLACKDUCK_USERNAME, "sysadmin"),
        configManager.getOrDefault(Config.BLACKDUCK_PASSWORD, "blackduck"),
        configManager.getOrDefault(Config.BLACKDUCK_TRUST_CERT, "true").toBoolean(),
        configManager.getOrDefault(Config.MANAGE_ARTIFACTORY, "true").toBoolean(),
        File(configManager.getOrThrow(Config.PLUGIN_ZIP_PATH)),
        configManager.getOrDefault(Config.PLUGIN_LOGGING_LEVEL, "DEBUG")
    )
}

class Application(
    dockerService: DockerService,
    artifactoryBaseUrl: String,
    artifactoryPort: String,
    artifactoryUsername: String,
    artifactoryPassword: String,
    artifactoryVersion: String,
    artifactoryLicenseFile: File,
    blackduckUrl: String,
    blackDuckUsername: String,
    blackDuckPassword: String,
    blackDuckTrustCert: Boolean,
    manageArtifactory: Boolean,
    pluginZipFile: File,
    pluginLoggingLevel: String
) {
    private val logger: IntLogger = Slf4jIntLogger(LoggerFactory.getLogger(this.javaClass))

    init {
        logger.info("Verifying Black Duck server config.")
        val blackDuckServerConfig = BlackDuckServerConfigBuilder()
            .setUrl(blackduckUrl)
            .setUsername(blackDuckUsername)
            .setPassword(blackDuckPassword)
            .setTrustCert(blackDuckTrustCert)
            .build()
        if (!blackDuckServerConfig.canConnect(logger)) {
            throw IntegrationException("Failed to connect the Black Duck server at $blackduckUrl.")
        }

        val artifactoryUser = ArtifactoryUser(artifactoryUsername, artifactoryPassword)
        val artifactoryUrl = "$artifactoryBaseUrl:$artifactoryPort/artifactory"
        val fuelManager = FuelManager()
        fuelManager.basePath = artifactoryUrl

        if (manageArtifactory) {
            logger.info("Loading Artifactory license.")
            if (!artifactoryLicenseFile.exists()) {
                throw IntegrationException("You have chosen to let automation manage Artifactory, but a the ARTIFACTORY_LICENSE_PATH supplied at ${artifactoryLicenseFile.absolutePath} does not exist.")
            }
            val licenseText = FileInputStream(artifactoryLicenseFile)
                .convertToString()
                .replace("\n", "")
                .replace(" ", "")

            logger.info("Validating plugin zip file.")
            if (pluginZipFile.exists()) {
                logger.info("Plugin zip file found at ${pluginZipFile.canonicalPath}")
            } else {
                throw IntegrationException("You have chosen to let automation manage Artifactory, but the plugin zip file at ${pluginZipFile.absolutePath} does not exist.")
            }

            logger.info("Installing and starting Artifactory version: $artifactoryVersion")
            val containerHash = dockerService.installAndStartArtifactory(artifactoryVersion, artifactoryPort)
//            val containerHash = "artifactory-automation-latest"
            logger.info("Artifactory container: $containerHash")

            logger.info("Waiting for Artifactory startup.")
            val systemApiService = SystemApiService(fuelManager, artifactoryUser)
            systemApiService.waitForSuccessfulStartup()

            logger.info("Applying Artifactory license.")
            systemApiService.applyLicense(licenseText)

            logger.info("Installing plugin.")
            val pluginsApiService = PluginsApiService(fuelManager, artifactoryUser)
            val blackDuckPluginService = BlackDuckPluginService(dockerService)
            blackDuckPluginService.installPlugin(containerHash, pluginZipFile, blackDuckServerConfig, pluginLoggingLevel)
            //Thread.sleep(10000L)
            //pluginsApiService.reloadPlugins()
            logger.info("Successfully installed the plugin.")

            println(dockerService.getArtifactoryLogs(containerHash).convertToString())
        } else {
            logger.info("Skipping Artifactory installation.")
        }
    }
}

fun InputStream.convertToString(encoding: Charset = StandardCharsets.UTF_8): String = IOUtils.toString(this, encoding)
