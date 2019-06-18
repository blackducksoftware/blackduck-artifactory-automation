package test.inspection

import artifactory.RepositoryManager
import artifactory.api.BlackDuckPluginApiService
import artifactory.api.PropertiesApiService
import artifactory.api.RepositoryConfiguration
import artifactory.api.RepositoryType
import artifactory.api.model.PackageType
import com.synopsys.integration.blackduck.artifactory.modules.inspection.InspectionModuleProperty
import org.artifactory.repo.RepoPathFactory
import plugin.BlackDuckPluginManager
import test.Test
import test.TestResult
import test.TestSequence

class RepositoryInitializationTest(
    private val repositoryManager: RepositoryManager,
    private val blackDuckPluginManager: BlackDuckPluginManager,
    private val blackDuckPluginApiService: BlackDuckPluginApiService,
    private val propertiesApiService: PropertiesApiService
) : TestSequence() {
    private lateinit var repositoryConfiguration: RepositoryConfiguration

    override fun setup() {
        repositoryConfiguration = repositoryManager.createRepository(PackageType.Defaults.PYPI, RepositoryType.REMOTE)
        blackDuckPluginManager.updateProperties(Pair(InspectionModuleProperty.REPOS, repositoryConfiguration.key))
    }

    @Test
    fun test(): TestResult {
        blackDuckPluginApiService.blackDuckInitializeRepositories()
        val repoKeyPath = RepoPathFactory.create(repositoryConfiguration.key)
        val itemProperties = propertiesApiService.getProperties(repoKeyPath)

//        itemProperties.properties[BlackDuckArtifactoryProperty.]

        return TestResult("Test name", true, "It passed!")
    }

    override fun tearDown() {

    }
}