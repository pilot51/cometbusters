plugins {
	kotlin("js")
}

dependencies {
	implementation(project(":common"))
}

kotlin {
	js(IR) {
		browser()
		binaries.executable()
	}
}

tasks.register<Copy>("copyResources") {
	from("../common/src/commonMain/resources")
	into("build/distributions")
}

tasks.named("processResources") {
	finalizedBy("copyResources")
}
