import artifactory.api.ArtifactoryUser
import artifactory.api.SystemApiService
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfigBuilder
import com.synopsys.integration.exception.IntegrationException
import com.synopsys.integration.log.IntLogger
import com.synopsys.integration.log.Slf4jIntLogger
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
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
        configManager.getOrDefault(Config.ARTIFACTORY_LICENSE_PATH, ""),
        configManager.getOrThrow(Config.BLACKDUCK_URL),
        configManager.getOrDefault(Config.BLACKDUCK_USERNAME, "sysadmin"),
        configManager.getOrDefault(Config.BLACKDUCK_PASSWORD, "blackduck"),
        configManager.getOrDefault(Config.BLACKDUCK_TRUST_CERT, "true").toBoolean(),
        configManager.getOrDefault(Config.MANAGE_ARTIFACTORY, "true").toBoolean()
    )
}

class Application(
    dockerService: DockerService,
    artifactoryBaseUrl: String,
    artifactoryPort: String,
    artifactoryUsername: String,
    artifactoryPassword: String,
    artifactoryVersion: String,
    artifactoryLicensePath: String,
    blackduckUrl: String,
    blackDuckUsername: String,
    blackDuckPassword: String,
    blackDuckTrustCert: Boolean,
    manageArtifactory: Boolean
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
        val artifactoryUrl = "$artifactoryBaseUrl:$artifactoryPort"
        if (manageArtifactory) {
            logger.info("Loading Artifactory license.")
            if (artifactoryLicensePath.isBlank()) {
                throw IntegrationException("You have chosen to let automation manage Artifactory, but a license key file path was not supplied.")
            }
            val artifactoryLicenseFile = File(artifactoryLicensePath)
            val licenseText = FileInputStream(artifactoryLicenseFile)
                .convertToString()
                .replace("\n", "")
                .replace(" ", "")

            logger.info("Installing and starting Artifactory version: $artifactoryVersion")
            val containerHash = dockerService.installAndStartArtifactory(artifactoryVersion, artifactoryPort)
            logger.info("Artifactory container: $containerHash")

            logger.info("Waiting for Artifactory startup.")
            val systemApiService = SystemApiService(artifactoryUser, artifactoryUrl)
            systemApiService.waitForSuccessfulStartup()

            logger.info("Applying Artifactory license.")
            systemApiService.applyLicense(licenseText)
        } else {
            logger.info("Skipping Artifactory installation.")
        }
    }
}

fun InputStream.convertToString(encoding: Charset = StandardCharsets.UTF_8): String = IOUtils.toString(this, encoding)
