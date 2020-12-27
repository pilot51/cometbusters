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
				implementation(kotlin("stdlib-common:1.4.21"))
				implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2-native-mt")
			}
		}
		val jvmMain by getting {
			dependencies {
				api(kotlin("stdlib-jdk8:1.4.21"))
			}
		}
		val jsMain by getting {
			dependencies {
				implementation(kotlin("stdlib-js:1.4.21"))
			}
		}
	}
}
