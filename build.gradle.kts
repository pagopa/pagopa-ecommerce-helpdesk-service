import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "it.pagopa.ecommerce.helpdesk"

version = "2.1.2"

description = "pagopa-ecommerce-helpdesk-service"

plugins {
  id("io.quarkus") version "3.25.0"
  kotlin("plugin.allopen") version "1.9.0"
  id("com.diffplug.spotless") version "6.25.0"
  id("org.openapi.generator") version "7.14.0"
  id("org.sonarqube") version "6.0.1.5171"
  id("com.dipien.semantic-version") version "2.0.0" apply false
  kotlin("jvm") version "2.2.0"
  jacoco
}

// eCommerce commons library version
val ecommerceCommonsVersion = "3.0.0"

// eCommerce commons library git ref (by default tag)
val ecommerceCommonsGitRef = ecommerceCommonsVersion

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

repositories {
  mavenCentral()
  mavenLocal()
}

// dependencies versions
val mockWebServerVersion = "4.11.0"
val ecsLoggingVersion = "1.5.0"
val httpclientVersion = "4.5.13"
val mockitoInlineVersion = "5.2.0"

dependencies {
  implementation(enforcedPlatform("io.quarkus:quarkus-bom:3.25.0"))
  implementation(enforcedPlatform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.10.2"))
  implementation("io.quarkus:quarkus-smallrye-openapi")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
  implementation("org.openapitools:openapi-generator-gradle-plugin:7.14.0")
  implementation("org.openapitools:jackson-databind-nullable:0.2.6")
  implementation("io.quarkus:quarkus-rest-kotlin")
  implementation("io.quarkus:quarkus-rest-client")
  implementation("io.quarkus:quarkus-rest-client-jackson")
  implementation("io.quarkus:quarkus-kotlin")
  implementation("io.quarkus:quarkus-smallrye-openapi")
  implementation("io.quarkus:quarkus-smallrye-health")
  implementation("io.quarkus:quarkus-mongodb-client:3.25.0")
  implementation("io.smallrye.reactive:mutiny:2.9.4")
  implementation("io.quarkiverse.openapi.generator:quarkus-openapi-generator-server:2.11.0")
  implementation("org.mongodb:mongodb-driver-sync:4.11.0")
  runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.10.2")
  runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.10.2")
  implementation("jakarta.json.bind:jakarta.json.bind-api:2.0.0")
  implementation("jakarta.annotation:jakarta.annotation-api:2.1.0")
  implementation("io.quarkiverse.cxf:quarkus-cxf:3.23.1")
  implementation("org.glassfish.jaxb:jaxb-runtime")
  implementation("jakarta.xml.bind:jakarta.xml.bind-api")
  implementation("org.apache.httpcomponents:httpclient:$httpclientVersion")
 implementation("co.elastic.logging:logback-ecs-encoder:$ecsLoggingVersion")
  implementation("it.pagopa:pagopa-ecommerce-commons:$ecommerceCommonsVersion")

  // Oracle DB
  implementation("com.oracle.database.jdbc:ojdbc11:23.2.0")
  implementation("com.oracle.database.r2dbc:oracle-r2dbc:1.1.1")

  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.0")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")


  implementation("io.quarkus:quarkus-rest-client")
  implementation("io.quarkus:quarkus-rest-client-jackson")
  implementation("io.quarkus:quarkus-smallrye-openapi")
  implementation("jakarta.validation:jakarta.validation-api")

  implementation("io.quarkus:quarkus-hibernate-validator")
  implementation("io.quarkus:quarkus-micrometer")
  implementation("io.quarkus:quarkus-vertx:3.15.1") // core Vert.x supportato da Quarkus
  implementation("io.quarkus:quarkus-kotlin:3.15.1")
  implementation("io.quarkus:quarkus-vertx-kotlin:3.15.1") // supporto Kotlin
  implementation("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")

  testImplementation("io.quarkus:quarkus-junit5")
  testImplementation("org.mockito:mockito-inline:$mockitoInlineVersion")
  testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
  testImplementation("com.squareup.okhttp3:mockwebserver:$mockWebServerVersion")
  testImplementation("com.squareup.okhttp3:okhttp:$mockWebServerVersion")
  testImplementation("it.pagopa:pagopa-ecommerce-commons:$ecommerceCommonsVersion:tests")
  testImplementation("com.h2database:h2:2.2.220")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")

  quarkusDev("io.quarkus:quarkus-arc-dev:3.25.0")
  quarkusDev("io.quarkus:quarkus-smallrye-openapi-dev:3.25.0")
  quarkusDev("io.quarkus:quarkus-smallrye-health-dev:3.25.0")
  quarkusDev("io.quarkus:quarkus-devui:3.25.0")
  quarkusDev("io.quarkus:quarkus-assistant-dev:3.25.0")
}

configurations {
  implementation.configure { exclude(group = "org.slf4j", module = "slf4j-simple") }
}
// Dependency locking - lock all dependencies
dependencyLocking { lockAllConfigurations() }

sourceSets {
  main {
    java { srcDirs("${layout.buildDirectory.get().asFile.path}/generated/src/main/java") }
    kotlin {
      srcDirs(
        "src/main/kotlin",
        "${layout.buildDirectory.get().asFile.path}/generated/src/main/kotlin"
      )
    }
    resources { srcDirs("src/resources") }
  }
}



tasks
  .register("applySemanticVersionPlugin") { dependsOn("prepareKotlinBuildScriptModel") }
  .apply { apply(plugin = "com.dipien.semantic-version") }

tasks.withType(JavaCompile::class.java).configureEach { options.encoding = "UTF-8" }

tasks.withType(Javadoc::class.java).configureEach { options.encoding = "UTF-8" }

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
  kotlin {
    toggleOffOn()
    targetExclude("build/**/*")
    ktfmt().kotlinlangStyle()
  }
  kotlinGradle {
    toggleOffOn()
    targetExclude("build/**/*.kts")
    ktfmt().googleStyle()
  }
  java {
    target("**/*.java")
    targetExclude("build/**/*")
    eclipse().configFile("eclipse-style.xml")
    toggleOffOn()
    removeUnusedImports()
    trimTrailingWhitespace()
    endWithNewline()
  }
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("helpdesk-v1") {
  generatorName.set("spring")
  inputSpec.set("$rootDir/api-spec/v1/openapi.yaml")
  outputDir.set(layout.buildDirectory.get().dir("generated").asFile.toString())
  apiPackage.set("it.pagopa.generated.ecommerce.helpdesk.api")
  modelPackage.set("it.pagopa.generated.ecommerce.helpdesk.model")
  generateApiDocumentation.set(false)
  generateApiTests.set(false)
  generateModelTests.set(false)
  library.set("spring-boot")
  modelNameSuffix.set("Dto")
  globalProperties.set(
    mapOf(
      "models" to "",               // Solo i model
      "modelDocs" to "false",       // Niente documentazione per i model
      "modelTests" to "false",      // Niente test per i model
      "apis" to "false",            // Disabilita API
      "apiDocs" to "false",         // Disabilita doc API
      "apiTests" to "false",        // Disabilita test API
      "supportingFiles" to "false"  // (opzionale) disabilita file di supporto
    )
  )
  configOptions.set(
    mapOf(
      "sourceFolder" to "src/main/java",
      "swaggerAnnotations" to "false",
      "openApiNullable" to "true",
      "interfaceOnly" to "true",
      "hideGenerationTimestamp" to "true",
      "skipDefaultInterface" to "true",
      "useSwaggerUI" to "false",
      "reactive" to "true",
      "oas3" to "true",
      "useJakartaEe" to "true",
      "generateSupportingFiles" to "true",
      "enumPropertyNaming" to "MACRO_CASE",
      "useMutiny" to "true",
      "returnResponse" to "false",
      "dateLibrary" to "java8",
      "serializationLibrary" to "jackson",
      "useGenericResponse" to "false",
      "additionalModelTypeAnnotations" to "@io.quarkus.runtime.annotations.RegisterForReflection"
    )
  )
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("helpdesk-v2") {
  generatorName.set("spring")
  inputSpec.set("$rootDir/api-spec/v2/openapi.yaml")
  outputDir.set(layout.buildDirectory.get().dir("generated").asFile.toString())
  apiPackage.set("it.pagopa.generated.ecommerce.helpdesk.v2.api")
  modelPackage.set("it.pagopa.generated.ecommerce.helpdesk.v2.model")
  generateApiDocumentation.set(false)
  generateApiTests.set(false)
  generateModelTests.set(false)
  library.set("spring-boot")
  modelNameSuffix.set("Dto")
  globalProperties.set(
    mapOf(
      "models" to "",
      "modelDocs" to "false",
      "modelTests" to "false",
      "apis" to "false",
      "apiDocs" to "false",
      "apiTests" to "false",
      "supportingFiles" to "false"
    )
  )
  configOptions.set(
    mapOf(
      "sourceFolder" to "src/main/java",
      "swaggerAnnotations" to "false",
      "openApiNullable" to "true",
      "interfaceOnly" to "true",
      "hideGenerationTimestamp" to "true",
      "skipDefaultInterface" to "true",
      "useSwaggerUI" to "false",
      "reactive" to "true",
      "oas3" to "true",
      "useJakartaEe" to "true",
      "generateSupportingFiles" to "true",
      "enumPropertyNaming" to "MACRO_CASE",
      "useTags" to "true",
      "useMutiny" to "true",
      "returnResponse" to "false",
      "dateLibrary" to "java8"
    )
  )
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("nodo") {
  generatorName.set("spring")
  remoteInputSpec.set(
    "https://raw.githubusercontent.com/pagopa/pagopa-infra/v1.465.0/src/core/api/nodopagamenti_api/nodoPerPM/v2/_openapi.json.tpl"
  )
  outputDir.set(layout.buildDirectory.get().dir("generated").asFile.toString())
  apiPackage.set("it.pagopa.generated.ecommerce.nodo.v2.api")
  modelPackage.set("it.pagopa.generated.ecommerce.nodo.v2.model")
  generateApiDocumentation.set(false)
  generateApiTests.set(false)
  generateModelTests.set(false)
  library.set("spring-boot")
  modelNameSuffix.set("Dto")
  globalProperties.set(
    mapOf(
      "models" to "",
      "modelDocs" to "false",
      "modelTests" to "false",
      "apis" to "false",
      "apiDocs" to "false",
      "apiTests" to "false",
      "supportingFiles" to "false"
    )
  )
  configOptions.set(
    mapOf(
      "sourceFolder" to "src/main/java",
      "swaggerAnnotations" to "false",
      "openApiNullable" to "true",
      "interfaceOnly" to "true",
      "hideGenerationTimestamp" to "true",
      "skipDefaultInterface" to "true",
      "useSwaggerUI" to "false",
      "reactive" to "true",
      "oas3" to "true",
      "useJakartaEe" to "true",
      "generateSupportingFiles" to "true",
      "enumPropertyNaming" to "MACRO_CASE",
      "useMutiny" to "true",
      "returnResponse" to "false",
      "dateLibrary" to "java8"
    )
  )
}

tasks.register("generateHelpdeskDTOs") {
  group = "openapi"
  description = "Generates DTOs for Helpdesk (Hybrid Approach - no interface generation)"

  dependsOn("helpdesk-v1", "helpdesk-v2", "nodo")
}


tasks.register<Exec>("install-commons") {
  val buildCommons = providers.gradleProperty("buildCommons")
  onlyIf("To build commons library run gradle build -PbuildCommons") { buildCommons.isPresent }
  commandLine("sh", "./pagopa-ecommerce-commons-maven-install.sh", ecommerceCommonsGitRef)
}

tasks.withType<KotlinCompile> {
  dependsOn("helpdesk-v1", "helpdesk-v2", "nodo")
  compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21) }
}

kotlin { jvmToolchain(21) }

tasks.named<Jar>("jar") { enabled = false }

tasks.test {
  useJUnitPlatform()
  finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}

tasks.jacocoTestReport {
  dependsOn(tasks.test) // tests are required to run before generating the report

  classDirectories.setFrom(
    files(
      classDirectories.files.map {
        fileTree(it).matching {
          exclude("it/pagopa/ecommerce/helpdesk/PagopaEcommerceHelpdeskServiceApplicationKt.class")
        }
      }
    )
  )

  reports { xml.required.set(true) }
}

/**
 * Task used to expand application properties with build specific properties such as artifact name
 * and version
 */
tasks.processResources {
  val projectName = project.name
  val projectVersion = project.version
  val projectDescription = project.description

  filesMatching("application.properties") {
    expand(
      mapOf("name" to projectName, "version" to projectVersion, "description" to projectDescription)
    )
  }
}



