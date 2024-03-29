package artifactory.api

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.gson.responseObject
import com.google.gson.annotations.SerializedName
import org.artifactory.repo.RepoPath

class PropertiesApiService(fuelManager: FuelManager) : ArtifactoryApiService(fuelManager) {
    fun getProperties(repoPath: RepoPath): ItemProperties {
        val responseResult = fuelManager.get("/api/storage/${repoPath.toPath()}")
            .responseObject<ItemProperties>()
        responseResult.second.validate()
        return responseResult.third.get()
    }
}

data class ItemProperties(
    @SerializedName("uri")
    val uri: String,
    @SerializedName("properties")
    val properties: Map<String, List<String>>
)