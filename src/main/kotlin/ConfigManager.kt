class ConfigManager {
    fun get(config: Config): String? {
        return System.getenv(config.name)
    }

    fun getOrDefault(config: Config, default: String): String {
        return get(config) ?: default
    }

    fun getOrThrow(
        config: Config,
        throwable: Throwable = IllegalArgumentException("Failed to find valid configuration for property '${config.name}'")
    ): String {
        return get(config) ?: throw throwable
    }
}

enum class Config {
    ARTIFACTORY_BASEURL,
    ARTIFACTORY_PORT,
    ARTIFACTORY_USERNAME,
    ARTIFACTORY_PASSWORD,
    ARTIFACTORY_VERSION,
    ARTIFACTORY_LICENSE_PATH
}