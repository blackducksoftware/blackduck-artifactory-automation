package plugin

import com.synopsys.integration.blackduck.artifactory.configuration.ConfigurationProperty
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
    private val dockerPluginsDirectory = "/opt/jfrog/artifactory/etc/plugins"

    fun installPlugin(zipFile: File, blackDuckServerConfig: BlackDuckServerConfig, containerHash: String) {
        logger.info("Unzipping plugin.")
        val unzippedPluginDirectory = unzipFile(zipFile, zipFile.parentFile)
        val propertiesFile = File(unzippedPluginDirectory, "lib/blackDuckPlugin.properties")

        logger.info("Uploading plugin files.")
        val uploadProcesses = mutableListOf<Process>()
        unzippedPluginDirectory.listFiles().forEach {
            val process = dockerService.uploadFile(containerHash, it, dockerPluginsDirectory)
            uploadProcesses.add(process)
        }
        uploadProcesses.forEach {
            it.waitFor()
        }

        logger.info("Rewriting properties.")
        initializeProperties(containerHash, propertiesFile, blackDuckServerConfig)

        logger.info("Updating logback.xml for logger purposes.")
        // TODO: Modify the logback.xml file
    }

    private fun initializeProperties(containerHash: String, propertiesFile: File, blackDuckServerConfig: BlackDuckServerConfig) {
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

        updateProperties(
            containerHash,
            propertiesFile,
            Pair(GeneralProperty.USERNAME, username),
            Pair(GeneralProperty.PASSWORD, password),
            Pair(GeneralProperty.API_TOKEN, blackDuckServerConfig.apiToken.orElse("")),
            Pair(GeneralProperty.TIMEOUT, blackDuckServerConfig.timeout.toString()),
            Pair(GeneralProperty.PROXY_HOST, blackDuckServerConfig.proxyInfo.host.orElse("")),
            Pair(GeneralProperty.PROXY_PORT, blackDuckServerConfig.proxyInfo.port.toString()),
            Pair(GeneralProperty.PROXY_USERNAME, blackDuckServerConfig.proxyInfo.username.orElse("")),
            Pair(GeneralProperty.PROXY_PASSWORD, blackDuckServerConfig.proxyInfo.password.orElse("")),
            Pair(GeneralProperty.TRUST_CERT, blackDuckServerConfig.isAlwaysTrustServerCertificate.toString())
        )
    }

    private fun updateProperties(containerHash: String, propertiesFile: File, vararg propertyPairs: Pair<ConfigurationProperty, String>) {
        val properties = Properties()
        val propertiesInputStream = propertiesFile.inputStream()
        properties.load(propertiesInputStream)
        propertiesInputStream.close()

        propertyPairs.forEach {
            properties[it.first.key] = it.second
        }

        properties.store(FileOutputStream(propertiesFile), "Modified automation properties")

        dockerService.uploadFile(containerHash, propertiesFile, "$dockerPluginsDirectory/lib/")
        fixPermissions(containerHash)
    }

    fun fixPermissions(containerHash: String) {
        logger.info("Fixing permissions.")
        val chownProcess = dockerService.chownFile(containerHash, "artifactory", "artifactory", dockerPluginsDirectory)
        chownProcess.waitFor()
        val chmodProcess = dockerService.chmodFile(containerHash, "777", dockerPluginsDirectory)
        chmodProcess.waitFor()
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