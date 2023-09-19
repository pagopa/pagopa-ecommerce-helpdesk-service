import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "it.pagopa.ecommerce.helpdesk"

version = "0.1.0"

description = "pagopa-ecommerce-helpdesk-service"

plugins {
  id("java")
  id("org.springframework.boot") version "3.0.5"
  id("io.spring.dependency-management") version "1.1.0"
  id("com.diffplug.spotless") version "6.18.0"
  id("org.openapi.generator") version "6.3.0"
  id("org.sonarqube") version "4.2.0.3129"
  id("com.dipien.semantic-version") version "2.0.0" apply false
  kotlin("plugin.spring") version "1.8.10"
  kotlin("jvm") version "1.8.10"
  jacoco
  application
}
// eCommerce commons library version
val ecommerceCommonsVersion = "0.24.1"

// eCommerce commons library git ref (by default tag)
val ecommerceCommonsGitRef = ecommerceCommonsVersion

java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
  mavenCentral()
  mavenLocal()
}

dependencyManagement {
  imports { mavenBom("org.springframework.boot:spring-boot-dependencies:3.0.5") }
  // Kotlin BOM
  imports { mavenBom("org.jetbrains.kotlin:kotlin-bom:1.7.22") }
  imports { mavenBom("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.6.4") }
}

val mockWebServerVersion = "4.11.0"

val ecsLoggingVersion = "1.5.0"

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
  implementation("org.apache.httpcomponents:httpclient")
  implementation("com.google.code.findbugs:jsr305:3.0.2")
  implementation("org.projectlombok:lombok")
  implementation("org.openapitools:openapi-generator-gradle-plugin:6.5.0")
  implementation("org.openapitools:jackson-databind-nullable:0.2.6")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
  implementation("org.springframework.boot:spring-boot-starter-aop")
  implementation("io.netty:netty-resolver-dns-native-macos:4.1.90.Final")
  implementation("com.diffplug.spotless:spotless-plugin-gradle:6.18.0")
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
  testImplementation("org.mockito:mockito-inline")
  testImplementation("io.projectreactor:reactor-test")
  // Kotlin dependencies
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
  testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
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
    java { srcDirs("$buildDir/generated/src/main/java") }
    kotlin { srcDirs("src/main/kotlin", "$buildDir/generated/src/main/kotlin") }
    resources { srcDirs("src/resources") }
  }
}

springBoot {
  mainClass.set("it.pagopa.ecommerce.helpdesk.PagopaEcommerceHelpdeskServiceApplicationKt")
  buildInfo { properties { additional.set(mapOf("description" to project.description)) } }
}

tasks.create("applySemanticVersionPlugin") {
  dependsOn("prepareKotlinBuildScriptModel")
  apply(plugin = "com.dipien.semantic-version")
}

tasks.withType(JavaCompile::class.java).configureEach {
  options.encoding = "UTF-8"
  options.compilerArgs.add("--enable-preview")
}

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

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("helpdesk") {
  generatorName.set("spring")
  inputSpec.set("$rootDir/api-spec/openapi.yaml")
  outputDir.set("$buildDir/generated")
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
      "enumPropertyNaming" to "UPPERCASE"
    )
  )
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("nodo") {
  generatorName.set("java")
  remoteInputSpec.set(
    "https://raw.githubusercontent.com/pagopa/pagopa-infra/eff0d7e961328c9888d73dc17d96695301df1861/src/core/api/nodopagamenti_api/nodoPerPM/v2/_swagger.json.tpl"
  )
  outputDir.set("$buildDir/generated")
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
      "enumPropertyNaming" to "UPPERCASE"
    )
  )
}

tasks.register<Exec>("install-commons") {
  val buildCommons = providers.gradleProperty("buildCommons")
  onlyIf("To build commons library run gradle build -PbuildCommons") { buildCommons.isPresent }
  commandLine("sh", "./pagopa-ecommerce-commons-maven-install.sh", ecommerceCommonsGitRef)
}

tasks.withType<KotlinCompile> {
  dependsOn("helpdesk", "nodo", "install-commons")
  kotlinOptions.jvmTarget = "17"
}

tasks.named<Jar>("jar") { enabled = false }

tasks.test {
  useJUnitPlatform()
  finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
  jvmArgs(listOf("--enable-preview"))
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
tasks.processResources { filesMatching("application.properties") { expand(project.properties) } }
