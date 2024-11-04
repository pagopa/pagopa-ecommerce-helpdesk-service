FROM openjdk:17-jdk@sha256:528707081fdb9562eb819128a9f85ae7fe000e2fbaeaf9f87662e7b3f38cb7d8 as build
WORKDIR /workspace/app

RUN microdnf install git

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.lockfile .
COPY gradle.properties .
COPY pagopa-ecommerce-commons-maven-install.sh .

COPY eclipse-style.xml eclipse-style.xml
COPY src src
COPY api-spec api-spec
RUN ./gradlew build -x test -PbuildCommons
RUN mkdir build/extracted && java -Djarmode=layertools -jar build/libs/*.jar extract --destination build/extracted

FROM amazoncorretto:17-alpine@sha256:d7ac7ae33ee93cd88703611c59596e3e30c87c78eae2b1f8f2f81ed5c89b2cf3

RUN addgroup --system user && adduser --ingroup user --system user
USER user:user

WORKDIR /app/

ARG EXTRACTED=/workspace/app/build/extracted

ADD --chown=user https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.25.1/opentelemetry-javaagent.jar .

COPY --from=build --chown=user ${EXTRACTED}/dependencies/ ./
RUN true
COPY --from=build --chown=user ${EXTRACTED}/spring-boot-loader/ ./
RUN true
COPY --from=build --chown=user ${EXTRACTED}/snapshot-dependencies/ ./
RUN true
COPY --from=build --chown=user ${EXTRACTED}/application/ ./
RUN true

ENTRYPOINT ["java","--enable-preview","-javaagent:opentelemetry-javaagent.jar","org.springframework.boot.loader.JarLauncher"]

