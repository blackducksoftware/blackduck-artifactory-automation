package artifactory

import artifactory.api.RepositoriesApiService
import artifactory.api.RepositoryConfiguration
import artifactory.api.RepositoryType
import artifactory.api.model.PackageType
import kotlin.random.Random

class RepositoryManager(private val repositoriesApiService: RepositoriesApiService) {
    fun createRepository(packageType: PackageType, repositoryType: RepositoryType): RepositoryConfiguration {
        val repositoryKey = "${packageType.packageType}-${Random.nextInt()}"
        repositoriesApiService.createRepository(repositoryKey, repositoryType, packageType)
        return retrieveRepository(repositoryKey)
    }

    fun retrieveRepository(repositoryKey: String): RepositoryConfiguration {
        return repositoriesApiService.getRepository(repositoryKey)
    }
}

