plugins {
	id("java")
	id("org.jetbrains.kotlin.jvm")
}

dependencies {
	implementation(project(":common"))
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}
