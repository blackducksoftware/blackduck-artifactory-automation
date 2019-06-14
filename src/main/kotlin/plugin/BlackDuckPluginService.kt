package plugin

import com.synopsys.integration.blackduck.artifactory.configuration.GeneralProperty
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig
import com.synopsys.integration.exception.IntegrationException
import com.synopsys.integration.log.Slf4jIntLogger
import docker.DockerService
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.util.*

class BlackDuckPluginService(private val dockerService: DockerService) {
    private val logger = Slf4jIntLogger(LoggerFactory.getLogger(javaClass))

    fun installPlugin(zipFile: File, blackDuckServerConfig: BlackDuckServerConfig, containerHash: String) {
        logger.info("Unzipping plugin.")
        val unzippedPluginDirectory = unzipFile(zipFile, zipFile.parentFile)
        val propertiesFile = File(unzippedPluginDirectory, "lib/blackDuckPlugin.properties")

        logger.info("Rewriting properties.")
        setProperties(propertiesFile, blackDuckServerConfig)

        logger.info("Uploading plugin files.")
        val uploadProcesses = mutableListOf<Process>()
        unzippedPluginDirectory.listFiles().forEach {
            val process = dockerService.uploadFile(containerHash, it, "/opt/jfrog/artifactory/etc/plugins")
            uploadProcesses.add(process)
        }
        uploadProcesses.forEach {
            it.waitFor()
        }

        logger.info("Fixing permissions.")
        val chownProcess = dockerService.chownFile(containerHash, "artifactory", "artifactory", "/opt/jfrog/artifactory/etc/plugins/")
        chownProcess.waitFor()
        val chmodProcess = dockerService.chmodFile(containerHash, "777", "/opt/jfrog/artifactory/etc/plugins/")
        chmodProcess.waitFor()
    }

    private fun setProperties(propertiesFile: File, blackDuckServerConfig: BlackDuckServerConfig) {
        val properties = Properties()
        val propertiesInputStream = propertiesFile.inputStream()
        properties.load(propertiesInputStream)
        propertiesInputStream.close()

        properties[GeneralProperty.URL.key] = blackDuckServerConfig.blackDuckUrl.toString()
        val credentialsOptional = blackDuckServerConfig.credentials
        var username = ""
        var password = ""
        credentialsOptional.ifPresent { credentials ->
            credentials.username.ifPresent { username = it }
            credentials.password.ifPresent { password = it }
        }

        properties[GeneralProperty.USERNAME.key] = username
        properties[GeneralProperty.PASSWORD.key] = password
        properties[GeneralProperty.API_TOKEN.key] = blackDuckServerConfig.apiToken.orElse("")
        properties[GeneralProperty.TIMEOUT.key] = blackDuckServerConfig.timeout.toString()
        properties[GeneralProperty.PROXY_HOST.key] = blackDuckServerConfig.proxyInfo.host.orElse("")
        properties[GeneralProperty.PROXY_PORT.key] = blackDuckServerConfig.proxyInfo.port.toString()
        properties[GeneralProperty.PROXY_USERNAME.key] = blackDuckServerConfig.proxyInfo.username.orElse("")
        properties[GeneralProperty.PROXY_PASSWORD.key] = blackDuckServerConfig.proxyInfo.password.orElse("")
        properties[GeneralProperty.TRUST_CERT.key] = blackDuckServerConfig.isAlwaysTrustServerCertificate.toString()

        properties.store(FileOutputStream(propertiesFile), "Modified automation properties")
    }

    private fun unzipFile(zipFile: File, outputDirectory: File): File {
        val process = ProcessBuilder()
            .command("unzip", "-o", zipFile.canonicalPath, "-d", outputDirectory.canonicalPath)
            .inheritIO()
            .start()

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw IntegrationException("unzip returned a non-zero exit code: $exitCode")
        }

        return File(zipFile.parent, zipFile.name.replace(".zip", ""))
    }
}