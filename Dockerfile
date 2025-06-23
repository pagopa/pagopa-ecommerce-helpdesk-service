# build commons with java 17
FROM openjdk:17-jdk@sha256:528707081fdb9562eb819128a9f85ae7fe000e2fbaeaf9f87662e7b3f38cb7d8 AS commons-builder

WORKDIR /workspace/app
RUN microdnf install -y findutils git

COPY . .
RUN chmod +x ./gradlew
RUN chmod +x ./pagopa-ecommerce-commons-maven-install.sh
RUN ./gradlew install-commons -PbuildCommons

# build application with java 21
FROM amazoncorretto:21-alpine@sha256:937a7f5c5f7ec41315f1c7238fd9ec0347684d6d99e086db81201ca21d1f5778 AS build

WORKDIR /workspace/app
RUN apk add --no-cache git findutils

COPY . .
COPY --from=commons-builder /root/.m2 /root/.m2

RUN chmod +x ./gradlew
RUN ./gradlew build -x test -Dorg.gradle.dependency.verification=off

RUN mkdir build/extracted && java -Djarmode=layertools -jar build/libs/*.jar extract --destination build/extracted

# runtime with java 21
FROM amazoncorretto:21-alpine

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

ENTRYPOINT ["java","-javaagent:opentelemetry-javaagent.jar","org.springframework.boot.loader.launch.JarLauncher"]