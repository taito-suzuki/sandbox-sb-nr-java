plugins {
    java
    id("org.springframework.boot") version "3.0.0"
    id("io.spring.dependency-management") version "1.1.0"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_19

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-web")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // NewRelicのjava agent
    implementation(files("${project.rootDir}/newrelic/newrelic-api.jar"))
    // https://mvnrepository.com/artifact/com.newrelic.logging/jul
    implementation("com.newrelic.logging:jul:2.6.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<org.springframework.boot.gradle.tasks.run.BootRun> {
    jvmArgs = listOf(
        "-Dspring.profiles.active=local,default",
        "--add-opens=java.base/java.net=ALL-UNNAMED",
        "-javaagent:./newrelic/newrelic.jar"
    )
}