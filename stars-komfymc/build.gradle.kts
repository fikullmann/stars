plugins { id("tools.aqua.stars.library-conventions") }

mavenMetadata {
    name.set("STARS Kotlin CMFTBL")
    description.set(
        "STARS - Scenario-Based Testing of Autonomous Robotic Systems - Library for Kotlin model checker of CMFTBL")
}

dependencies {
    implementation(project(":stars-core"))
    testImplementation(project(mapOf("path" to ":stars-data-av")))
    testImplementation(project(":stars-data-av", "test"))
}