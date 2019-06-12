import com.synopsys.integration.exception.IntegrationException
import com.synopsys.integration.log.Slf4jIntLogger
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class DockerService {
    private val logger = Slf4jIntLogger(LoggerFactory.getLogger(javaClass))

    @Throws(IOException::class, InterruptedException::class, IntegrationException::class)
    fun installAndStartArtifactory(version: String, artifactoryPort: String): String {
        val artifactoryInstallProcess = installArtifactory(version)
        artifactoryInstallProcess.waitFor(5, TimeUnit.MINUTES)
        if (artifactoryInstallProcess.exitValue() != 0) {
            throw IntegrationException("Failed to install artifactory. Docker returned an exit code of ${artifactoryInstallProcess.exitValue()}")
        }

        val startArtifactoryProcess = startArtifactory(version, artifactoryPort)
        startArtifactoryProcess.waitFor(2, TimeUnit.MINUTES)
        if (startArtifactoryProcess.exitValue() != 0) {
            throw IntegrationException("Failed to start artifactory. Docker returned an exit code of ${startArtifactoryProcess.exitValue()}")
        }

        return startArtifactoryProcess.inputStream.convertToString()
    }

    @Throws(IOException::class)
    fun installArtifactory(version: String): Process {
        return runDockerCommand("docker", "pull", String.format("docker.bintray.io/jfrog/artifactory-pro:%s", version))
    }

    @Throws(IOException::class)
    fun startArtifactory(version: String, artifactoryPort: String): Process {
        return runDockerCommand(
            "docker", "run", "--name",
            String.format("artifactory-automation-%s", version),
            "-d", "-p",
            String.format("%s:%s", artifactoryPort, artifactoryPort),
            String.format("docker.bintray.io/jfrog/artifactory-pro:%s", version)
        )
    }

    @Throws(IOException::class)
    private fun runDockerCommand(vararg command: String): Process {
        logger.info("Running command: " + StringUtils.join(command, " "))
        val processBuilder = ProcessBuilder(*command)
        processBuilder.inheritIO()
        return processBuilder.start()
    }
}