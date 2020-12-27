plugins {
	id("org.jetbrains.kotlin.js")
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(project(":common"))
	implementation(kotlin("stdlib-js"))
}

kotlin {
	js {
		browser {
			testTask {
				enabled = false
			}
		}
		binaries.executable()
	}
}

tasks.register<Copy>("copyResources") {
	from("../common/src/commonMain/resources")
	into("build/distributions")
}
