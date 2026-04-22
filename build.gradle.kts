plugins {
    id("java")
}

group = "com.litovskiy"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.12.1")
    compileOnly("org.projectlombok:lombok:1.18.44")
    annotationProcessor("org.projectlombok:lombok:1.18.44")
    implementation("org.telegram:telegrambots-client:9.5.0")
    implementation("org.telegram:telegrambots-longpolling:9.5.0")
}

tasks.test {
    useJUnitPlatform()
}