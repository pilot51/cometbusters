plugins {
	id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
	jvm()
	js {
		browser()
	}

	sourceSets {
		val commonMain by getting {
			dependencies {
				implementation(kotlin("stdlib-common:1.6.0"))
				implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2-native-mt")
			}
		}
		val jvmMain by getting {
			dependencies {
				implementation(kotlin("stdlib-jdk8:1.6.0"))
			}
		}
		val jsMain by getting {
			dependencies {
				implementation(kotlin("stdlib-js:1.6.0"))
				implementation("org.jetbrains.kotlin-wrappers:kotlin-extensions:1.0.1-pre.270-kotlin-1.6.0")
			}
		}
	}
}
