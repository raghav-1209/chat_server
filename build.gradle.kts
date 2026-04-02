plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)


}

group = "com"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.h2)
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.resources)
    implementation(libs.ktor.server.request.validation)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    implementation(libs.postgresql)
    implementation(libs.ktor.server.websockets)
val kotlinVersion ="3.3.3"



    implementation("io.ktor:ktor-client-core:${kotlinVersion}")
    implementation("io.ktor:ktor-client-cio:${kotlinVersion}")
    implementation("io.ktor:ktor-client-content-negotiation:${kotlinVersion}")
    implementation("io.ktor:ktor-serialization-gson:${kotlinVersion}")

    implementation("com.google.firebase:firebase-admin:9.2.0")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

}
