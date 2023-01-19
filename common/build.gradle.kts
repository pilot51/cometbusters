plugins {
	kotlin("multiplatform")
}

kotlin {
	jvm()
	js(IR) {
		browser()
	}

	sourceSets {
		val commonMain by getting {
			dependencies {
				implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
			}
		}
		val jvmMain by getting
		val jsMain by getting {
			dependencies {
				implementation("org.jetbrains.kotlin-wrappers:kotlin-js:1.0.0-pre.477")
			}
		}
	}
}
