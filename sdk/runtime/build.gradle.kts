plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "dev.acpsdk.runtime"
    compileSdk = 35

    defaultConfig {
        minSdk = 28
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    api(project(":sdk:annotations"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.mockk)
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
    configure(com.vanniktech.maven.publish.AndroidSingleVariantLibrary(variant = "release"))
    coordinates(property("GROUP").toString(), "runtime", property("VERSION_NAME").toString())
    pom {
        name.set("ACP Runtime")
        description.set("Android runtime library for the Android Capability Protocol SDK")
        url.set(property("POM_URL").toString())
        licenses {
            license {
                name.set(property("POM_LICENCE_NAME").toString())
                url.set(property("POM_LICENCE_URL").toString())
            }
        }
        developers {
            developer {
                id.set(property("POM_DEVELOPER_ID").toString())
                name.set(property("POM_DEVELOPER_NAME").toString())
                url.set(property("POM_DEVELOPER_URL").toString())
            }
        }
        scm {
            url.set(property("POM_SCM_URL").toString())
            connection.set(property("POM_SCM_CONNECTION").toString())
            developerConnection.set(property("POM_SCM_DEV_CONNECTION").toString())
        }
    }
}
