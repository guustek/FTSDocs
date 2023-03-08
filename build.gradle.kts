plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.0.13"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))

    }
}

group = "ftsdocs"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")

    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.19.0")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.19.0")
    implementation("org.apache.logging.log4j:log4j-api:2.19.0")
    implementation("org.apache.logging.log4j:log4j-core:2.19.0")

    implementation("org.apache.solr:solr-solrj:9.0.0")
    implementation("org.apache.solr:solr-core:9.0.0")

    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")

    implementation("com.google.code.gson:gson:2.10")
    implementation("commons-io:commons-io:2.11.0")

    implementation("org.springframework:spring-context:6.0.3")
    implementation("org.apache.tika:tika-core:2.6.0")
    implementation("org.apache.tika:tika-parsers-standard-package:2.6.0")
    implementation("org.jfxtras:jmetro:11.6.14")
    implementation("org.fxmisc.richtext:richtextfx:0.11.0")
    implementation("org.controlsfx:controlsfx:11.1.2")
    implementation("io.methvin:directory-watcher:0.17.1")
}

javafx {
    version = "17"
    modules("javafx.controls", "javafx.fxml")
}

application {
    mainClass.set("ftsdocs.Launcher")
    applicationDefaultJvmArgs = listOf(
        "--add-opens", "javafx.graphics/javafx.scene.paint=ALL-UNNAMED",
        "--add-opens", "javafx.base/com.sun.javafx.event=ALL-UNNAMED")
}

tasks.clean {
    delete("bin")
}

tasks.register<Copy>("copyJar") {
    delete("bin")
    from("build/libs/FTSDocs.jar")
    into("bin")
    include("*.jar")
    dependsOn(tasks.jar)
}

tasks.test {
    useJUnitPlatform()
}