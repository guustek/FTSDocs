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
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")

    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.19.0")
    implementation("org.apache.logging.log4j:log4j-api:2.19.0")
    implementation("org.apache.logging.log4j:log4j-core:2.19.0")

    implementation("org.apache.solr:solr-solrj:9.0.0")
    implementation("org.apache.solr:solr-core:9.0.0")

    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")

    implementation ("com.google.code.gson:gson:2.10")
    implementation ("commons-io:commons-io:2.11.0")

    implementation("org.springframework:spring-context:6.0.0")

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