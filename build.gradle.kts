plugins {
    application
    id("org.openjfx.javafxplugin") version "0.0.13"
}

group = "ftsdocs"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")

    implementation("org.apache.solr:solr-solrj:9.0.0")
    implementation("org.apache.solr:solr-core:9.0.0")
}

javafx {
    version = "17"
    modules("javafx.controls", "javafx.fxml")
//    configuration = "compileOnly"
}

application {
    mainClass.set("ftsdocs.FTSDocsApplication")
}

tasks.test {
    useJUnitPlatform()
}