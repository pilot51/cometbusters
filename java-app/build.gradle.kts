plugins {
	id("java")
	kotlin("jvm")
}

dependencies {
	implementation(project(":common"))
}

tasks.register<Copy>("copyResources") {
	from("../common/src/commonMain/resources")
	into("build/resources/main")
}

tasks.named("processResources") {
	finalizedBy("copyResources")
}
