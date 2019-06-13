import artifactory.ArtifactoryUser
import artifactory.SystemApiService
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfigBuilder
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
        configManager.getOrThrow(Config.ARTIFACTORY_LICENSE_PATH),
        configManager.getOrThrow(Config.BLACKDUCK_URL),
        configManager.getOrDefault(Config.BLACKDUCK_USERNAME, "sysadmin"),
        configManager.getOrDefault(Config.BLACKDUCK_PASSWORD, "blackduck"),
        configManager.getOrDefault(Config.BLACKDUCK_TRUST_CERT, "true")
    )
}

class Application(
    private val dockerService: DockerService,
    private val artifactoryBaseUrl: String,
    private val artifactoryPort: String,
    private val artifactoryUsername: String,
    private val artifactoryPassword: String,
    private val artifactoryVersion: String,
    private val artifactoryLicensePath: String,
    private val blackduckUrl: String,
    private val blackDuckUsername: String,
    private val blackDuckPassword: String,
    private val blackDuckTrustCert: String
) {
    private val logger: IntLogger = Slf4jIntLogger(LoggerFactory.getLogger(this.javaClass))

    init {
        logger.info("Building BlackDuckServerConfig.")
        val blackDuckServerConfig = BlackDuckServerConfigBuilder()
            .setUrl(blackduckUrl)
            .setUsername(blackDuckUsername)
            .setPassword(blackDuckPassword)
            .setTrustCert(blackDuckTrustCert.toBoolean())
            .build()

        logger.info("Loading Artifactory License.")
        val artifactoryLicenseFile = File(artifactoryLicensePath)
        val licenseText = FileInputStream(artifactoryLicenseFile).convertToString().replace("\n", "").replace(" ", "")

        logger.info("Installing and starting Artifactory version: $artifactoryVersion")
        val containerHash = dockerService.installAndStartArtifactory(artifactoryVersion, artifactoryPort)
        logger.info("Artifactory container: $containerHash")

        logger.info("Waiting for Artifactory startup.")
        val artifactoryUser = ArtifactoryUser(artifactoryUsername, artifactoryPassword)
        val artifactoryUrl = "$artifactoryBaseUrl:$artifactoryPort"
        val systemApiService = SystemApiService(artifactoryUser, artifactoryUrl)
        systemApiService.waitForSuccessfulStartup()

        logger.info("Applying Artifactory license.")
        systemApiService.applyLicense(licenseText)
    }
}

fun InputStream.convertToString(encoding: Charset = StandardCharsets.UTF_8): String = IOUtils.toString(this, encoding)
