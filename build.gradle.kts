plugins {
    java
    id("io.quarkus")
    id("com.diffplug.spotless") version "8.4.0"
    checkstyle
    pmd
}

repositories {
    mavenCentral()
    mavenLocal()
    gradlePluginPortal()
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project
val avroVersion = "1.11.3"

dependencies {
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-grpc")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-container-image-jib")
    implementation("org.neo4j:neo4j-ogm-quarkus:4.2.3")

    implementation("io.quarkus:quarkus-messaging-kafka")
    implementation("io.confluent:kafka-avro-serializer:7.9.0")

    // Confluent Avro Serializer
    implementation("io.quarkus:quarkus-confluent-registry-avro")
    implementation("org.apache.avro:avro:$avroVersion")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("io.smallrye.reactive:smallrye-reactive-messaging-in-memory")
}

group = "net.explorviz"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

sourceSets["main"].java {
    srcDir("build/classes/java/quarkus-generated-sources/grpc")
    srcDir("build/classes/java/quarkus-generated-sources/avdl")
}

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
    jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

fun registerGitHook(taskName: String, hookFile: String, targetHook: String) =
    tasks.register<Copy>(taskName) {
        from("code-analysis/$hookFile")
        into(".git/hooks")
        rename { "$targetHook" }
    }

registerGitHook("registerPreCommitHook", "pre-commit.sh", "pre-commit")
registerGitHook("registerPreMergeCommitHook", "pre-commit.sh", "pre-merge-commit")

tasks.named("quarkusGenerateCode") {
    dependsOn("registerPreCommitHook")
    dependsOn("registerPreMergeCommitHook")
}

pmd {
    // Empty ruleset is necessary for exclude-pattern
    // https://stackoverflow.com/questions/32247190/pmd-exclude-pattern-with-gradle
    ruleSets = listOf()
    ruleSetFiles = files("code-analysis/pmd.xml")
    isIgnoreFailures = false
    toolVersion = "6.53.0"
}

checkstyle {
    configDirectory.set(file("code-analysis"))
    configFile = file("code-analysis/checkstyle.xml")
    maxWarnings = 0
    isIgnoreFailures = false
    toolVersion = "10.12.5"
}

spotless {
    java {
        target("src/*/java/**/*.java")
        googleJavaFormat().reflowLongStrings()
        formatAnnotations()
    }

    flexmark {
        target("**/*.md")
        flexmark()
    }

    format("misc") {
        // define the files to apply `misc` to
        target("*.gradle", "*.gradle.kts", ".gitattributes", ".gitignore")

        // define the steps to apply to those files
        trimTrailingWhitespace()
        leadingTabsToSpaces()
        endWithNewline()
    }
}
