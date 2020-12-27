rootProject.name = "CometBusters"

pluginManagement {
	resolutionStrategy {
		eachPlugin {
			when (requested.id.id) {
				"kotlin-multiplatform" -> useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${requested.version}")
			}
		}
	}
}

include("common")
include("java-app")
include("js-app")
