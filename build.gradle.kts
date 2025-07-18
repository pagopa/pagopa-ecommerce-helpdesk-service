import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "it.pagopa.ecommerce.helpdesk"

version = "2.1.1"

description = "pagopa-ecommerce-helpdesk-service"

plugins {
  id("java")
  id("org.springframework.boot") version "3.4.5"
  id("io.spring.dependency-management") version "1.1.0"
  id("com.diffplug.spotless") version "6.25.0"
  id("org.openapi.generator") version "7.13.0"
  id("org.sonarqube") version "6.0.1.5171"
  id("com.dipien.semantic-version") version "2.0.0" apply false
  kotlin("plugin.spring") version "2.2.0"
  kotlin("jvm") version "2.2.0"
  jacoco
  application
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

dependencyManagement {
  imports { mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.5") }
  // Kotlin BOM
  imports { mavenBom("org.jetbrains.kotlin:kotlin-bom:2.2.0") }
  imports { mavenBom("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.7.3") }
}

// dependencies versions
val mockWebServerVersion = "4.11.0"
val ecsLoggingVersion = "1.5.0"
val httpclientVersion = "4.5.13"
val mockitoInlineVersion = "5.2.0"

dependencies {
  implementation("io.projectreactor:reactor-core")
  implementation("io.projectreactor.netty:reactor-netty")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-web-services")
  implementation("org.glassfish.jaxb:jaxb-runtime")
  implementation("jakarta.xml.bind:jakarta.xml.bind-api")
  implementation("io.swagger.core.v3:swagger-annotations:2.2.8")
  implementation("org.apache.httpcomponents:httpclient:$httpclientVersion")
  implementation("com.google.code.findbugs:jsr305:3.0.2")
  implementation("org.projectlombok:lombok")
  implementation("org.openapitools:openapi-generator-gradle-plugin:6.5.0")
  implementation("org.openapitools:jackson-databind-nullable:0.2.6")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
  implementation("org.springframework.boot:spring-boot-starter-aop")
  implementation("io.netty:netty-resolver-dns-native-macos:4.1.90.Final")
  implementation("com.diffplug.spotless:spotless-plugin-gradle:6.25.0")
  implementation("javax.annotation:javax.annotation-api:1.3.2")
  // Kotlin dependencies
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
  implementation("it.pagopa:pagopa-ecommerce-commons:$ecommerceCommonsVersion")

  // oracle
  implementation("com.oracle.database.jdbc:ojdbc11:23.2.0.0")
  implementation("com.oracle.database.r2dbc:oracle-r2dbc:1.1.1")

  // ECS logback encoder
  implementation("co.elastic.logging:logback-ecs-encoder:$ecsLoggingVersion")

  runtimeOnly("org.springframework.boot:spring-boot-devtools")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.mockito:mockito-inline:$mockitoInlineVersion")
  testImplementation("io.projectreactor:reactor-test")
  // Kotlin dependencies
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
  testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
  testImplementation("com.squareup.okhttp3:mockwebserver:$mockWebServerVersion")
  testImplementation("com.squareup.okhttp3:okhttp:$mockWebServerVersion")
  testImplementation("it.pagopa:pagopa-ecommerce-commons:$ecommerceCommonsVersion:tests")
  testImplementation("com.h2database:h2:2.2.220")
  testImplementation("io.r2dbc:r2dbc-h2:1.0.0.RELEASE")
}

configurations {
  implementation.configure {
    exclude(module = "spring-boot-starter-web")
    exclude("org.apache.tomcat")
    exclude(group = "org.slf4j", module = "slf4j-simple")
  }
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

springBoot {
  mainClass.set("it.pagopa.ecommerce.helpdesk.PagopaEcommerceHelpdeskServiceApplicationKt")
  buildInfo { properties { additional.set(mapOf("description" to project.description)) } }
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
  generateApiTests.set(false)
  generateApiDocumentation.set(false)
  generateApiTests.set(false)
  generateModelTests.set(false)
  library.set("spring-boot")
  modelNameSuffix.set("Dto")
  configOptions.set(
    mapOf(
      "swaggerAnnotations" to "false",
      "openApiNullable" to "true",
      "interfaceOnly" to "true",
      "hideGenerationTimestamp" to "true",
      "skipDefaultInterface" to "true",
      "useSwaggerUI" to "false",
      "reactive" to "true",
      "useSpringBoot3" to "true",
      "oas3" to "true",
      "generateSupportingFiles" to "true",
      "enumPropertyNaming" to "MACRO_CASE"
    )
  )
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("helpdesk-v2") {
  generatorName.set("spring")
  inputSpec.set("$rootDir/api-spec/v2/openapi.yaml")
  outputDir.set(layout.buildDirectory.get().dir("generated").asFile.toString())
  apiPackage.set("it.pagopa.generated.ecommerce.helpdesk.v2.api")
  modelPackage.set("it.pagopa.generated.ecommerce.helpdesk.v2.model")
  generateApiTests.set(false)
  generateApiDocumentation.set(false)
  generateApiTests.set(false)
  generateModelTests.set(false)
  library.set("spring-boot")
  modelNameSuffix.set("Dto")
  configOptions.set(
    mapOf(
      "swaggerAnnotations" to "false",
      "openApiNullable" to "true",
      "interfaceOnly" to "true",
      "hideGenerationTimestamp" to "true",
      "skipDefaultInterface" to "true",
      "useSwaggerUI" to "false",
      "reactive" to "true",
      "useSpringBoot3" to "true",
      "oas3" to "true",
      "generateSupportingFiles" to "true",
      "enumPropertyNaming" to "MACRO_CASE",
      "useTags" to "true"
    )
  )
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("nodo") {
  generatorName.set("java")
  remoteInputSpec.set(
    "https://raw.githubusercontent.com/pagopa/pagopa-infra/v1.465.0/src/core/api/nodopagamenti_api/nodoPerPM/v2/_openapi.json.tpl"
  )
  outputDir.set(layout.buildDirectory.get().dir("generated").asFile.toString())
  apiPackage.set("it.pagopa.generated.ecommerce.nodo.v2.api")
  modelPackage.set("it.pagopa.generated.ecommerce.nodo.v2.model")
  generateApiTests.set(false)
  generateApiDocumentation.set(false)
  generateApiTests.set(false)
  generateModelTests.set(false)
  library.set("webclient")
  modelNameSuffix.set("Dto")
  configOptions.set(
    mapOf(
      "swaggerAnnotations" to "false",
      "openApiNullable" to "true",
      "interfaceOnly" to "true",
      "hideGenerationTimestamp" to "true",
      "skipDefaultInterface" to "true",
      "useSwaggerUI" to "false",
      "reactive" to "true",
      "useSpringBoot3" to "true",
      "oas3" to "true",
      "generateSupportingFiles" to "true",
      "enumPropertyNaming" to "MACRO_CASE"
    )
  )
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
