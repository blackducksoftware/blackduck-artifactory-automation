import artifactory.ArtifactoryApiService
import artifactory.ArtifactoryUser
import com.synopsys.integration.log.IntLogger
import com.synopsys.integration.log.Slf4jIntLogger
import com.synopsys.integration.rest.client.IntHttpClient
import com.synopsys.integration.rest.proxy.ProxyInfo
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URL
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
        configManager.getOrThrow(Config.ARTIFACTORY_LICENSE_PATH)
    )
}

class Application(
    private val dockerService: DockerService,
    private val artifactoryBaseUrl: String,
    private val artifactoryPort: String,
    private val artifactoryUsername: String,
    private val artifactoryPassword: String,
    private val artifactoryVersion: String,
    private val artifactoryLicensePath: String
) {
    private val logger: IntLogger = Slf4jIntLogger(LoggerFactory.getLogger(this.javaClass))

    init {
        logger.info("Loading Artifactory License")
        val artifactoryLicenseFile = File(artifactoryLicensePath)
        val licenseText = FileInputStream(artifactoryLicenseFile).convertToString().replace("\n", "").replace(" ", "")

        val artifactoryUser = ArtifactoryUser(artifactoryUsername, artifactoryPassword)
        val httpClientLogger = Slf4jIntLogger(LoggerFactory.getLogger(ArtifactoryApiService::class.java))
        val httpClient = IntHttpClient(httpClientLogger, 200, true, ProxyInfo.NO_PROXY_INFO)
        val artifactoryUrl = URL("$artifactoryBaseUrl:$artifactoryPort")
        val artifactoryApiService = ArtifactoryApiService(httpClient, artifactoryUser, artifactoryUrl)
//        logger.info("Installing and starting artifactory version: $artifactoryVersion")
//        val containerHash = dockerService.installAndStartArtifactory(artifactoryVersion, artifactoryPort)
//        logger.info("Artifactory container: $containerHash")
//
//        logger.info("Waiting for startup.")
//        val systemApiService = SystemApiService(artifactoryApiService)
//        systemApiService.waitForSuccessfulStartup()
//
//        logger.info("Applying Artifactory license.")
//        systemApiService.applyLicense(licenseText)
    }
}

fun InputStream.convertToString(encoding: Charset = StandardCharsets.UTF_8): String = IOUtils.toString(this, encoding)
