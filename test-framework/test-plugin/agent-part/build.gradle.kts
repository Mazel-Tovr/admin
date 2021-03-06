plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(drill("drill-agent-part-jvm", drillApiVersion))
    implementation(drill("common-jvm", drillApiVersion))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationVersion")
}

tasks {
    val jar by existing(Jar::class){
        archiveFileName.set("agent-part.jar")
    }
}
