# pagoPA Help desk service

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=pagopa_pagopa-ecommerce-helpdesk-service&metric=alert_status)](https://sonarcloud.io/dashboard?id=pagopa_pagopa-ecommerce-helpdesk-service)

This microservice is responsible for ...

- [pagoPA Help Desk Service](#pagopa-helpdesk-service)
    * [Api Documentation 📖](#api-documentation-)
    * [Technology Stack](#technology-stack)
    * [Start Project Locally 🚀](#start-project-locally-)
        + [Prerequisites](#prerequisites)
        + [Run docker container](#run-docker-container)
    * [Develop Locally 💻](#develop-locally-)
        + [Prerequisites](#prerequisites-1)
        + [GitHub Token Setup](#github-token-setup)
        + [Run the project](#run-the-project)
        + [Testing 🧪](#testing-)
            - [Unit testing](#unit-testing)
            - [Integration testing](#integration-testing)
            - [Performance testing](#performance-testing)
    * [Dependency management 🔧](#dependency-management-)
        + [Dependency lock](#dependency-lock)
        + [Dependency verification](#dependency-verification)
    * [Contributors 👥](#contributors-)
        + [Maintainers](#maintainers)

<small><i><a href='http://ecotrust-canada.github.io/markdown-toc/'>Table of contents generated with
markdown-toc</a></i></small>

---

## Api Documentation 📖

See
the [OpenAPI 3 here.](https://editor.swagger.io/?url=https://raw.githubusercontent.com/pagopa/pagopa-ecommerce-helpdesk-service/main/api-spec/openapi.yaml)

---

## Technology Stack

- Kotlin
- Spring Boot

---

## Working with Windows

If you are developing on Windows, it is recommended the use of WSL2 combined with IntelliJ IDEA.

The IDE should be installed on Windows, with the repository cloned into a folder in WSL2. All the necessary tools will
be installed in the Linux distro of your choice.

You can find more info on how to set up the environment following the link below.

https://www.jetbrains.com/help/idea/how-to-use-wsl-development-environment-in-product.html

After setting up the WSL environment, you can test the application by building it through either Docker or Spring Boot (
useful for local development).

## Start Project Locally 🚀

### Prerequisites

- docker

### Populate the environment

The microservice needs a valid `.env` file in order to be run.

If you want to start the application without too much hassle, you can just copy `.env.example` with

```shell
$ cp .env.example .env
```

to get a good default configuration.

If you want to customize the application environment, reference this table:

| Variable name                           | Description                                                                                                                                                | type               | default |
|-----------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------|---------|
| DEFAULT_LOGGING_LEVEL                   | Default root application logging level                                                                                                                     | string             | INFO    |
| APP_LOGGING_LEVEL                       | Application logging level                                                                                                                                  | string             | INFO    |
| WEB_LOGGING_LEVEL                       | Web logging level                                                                                                                                          | string             | INFO    |
| MONGO_HOST                              | MongoDB ecommerce hostname instance                                                                                                                        | hostname (string)  |         |
| MONGO_PORT                              | Port where MongoDB is bound to in MongoDB host                                                                                                             | number             |         |
| MONGO_USERNAME                          | MongoDB username used to connect to the database                                                                                                           | string             |         |
| MONGO_PASSWORD                          | MongoDB password used to connect to the database                                                                                                           | string             |         |
| MONGO_SSL_ENABLED                       | Whether SSL is enabled while connecting to MongoDB                                                                                                         | string             |         |
| MONGO_PORT                              | Port used for connecting to MongoDB instance                                                                                                               | string             |         |
| MONGO_MIN_POOL_SIZE                     | Min amount of connections to be retained into connection pool. See docs *                                                                                  | string             |         |
| MONGO_MAX_POOL_SIZE                     | Max amount of connections to be retained into connection pool.See docs *                                                                                   | string             |         |
| MONGO_MAX_IDLE_TIMEOUT_MS               | Max timeout after which an idle connection is killed in milliseconds. See docs *                                                                           | string             |         |
| MONGO_CONNECTION_TIMEOUT_MS             | Max time to wait for a connection to be opened. See docs *                                                                                                 | string             |         |
| MONGO_SOCKET_TIMEOUT_MS                 | Max time to wait for a command send or receive before timing out. See docs *                                                                               | string             |         |
| MONGO_SERVER_SELECTION_TIMEOUT_MS       | Max time to wait for a server to be selected while performing a communication with Mongo in milliseconds. See docs *                                       | string             |         |
| MONGO_WAITING_QUEUE_MS                  | Max time a thread has to wait for a connection to be available in milliseconds. See docs *                                                                 | string             |         |
| MONGO_HEARTBEAT_FREQUENCY_MS            | Hearth beat frequency in milliseconds. This is an hello command that is sent periodically on each active connection to perform an health check. See docs * | string             |         |
| NPG_URI                                 | NPG service URI                                                                                                                                            | string             |         |
| NPG_READ_TIMEOUT                        | NPG service HTTP read timeout                                                                                                                              | integer            |         |
| NPG_CONNECTION_TIMEOUT                  | NPG service HTTP connection timeout                                                                                                                        | integer            |         |
| NPG_API_KEY                             | NPG service api-key                                                                                                                                        | string             |         |
| NPG_CARDS_PSP_KEYS                      | Secret structure that holds psp - api keys association for authorization request                                                                           | string             |         |
| NPG_CARDS_PSP_LIST                      | List of all psp ids that are expected to be found into the NPG_CARDS_PSP_KEYS configuration (used for configuration cross validation)                      | string             |         |
| NPG_PAYPAL_PSP_KEYS                     | Secret structure that holds psp - api keys association for authorization request used for APM PAYPAL payment method                                        | string             |         |
| NPG_PAYPAL_PSP_LIST                     | List of all psp ids that are expected to be found into the NPG_PAYPAL_PSP_KEYS configuration (used for configuration cross validation)                     | string             |         |
| NPG_BANCOMATPAY_PSP_KEYS                | Secret structure that holds psp - api keys association for authorization request used for APM Bancomat pay payment method                                  | string             |         |
| NPG_BANCOMATPAY_PSP_LIST                | List of all psp ids that are expected to be found into the NPG_BANCOMATPAY_PSP_KEYS configuration (used for configuration cross validation)                | string             |         |
| NPG_MYBANK_PSP_KEYS                     | Secret structure that holds psp - api keys association for authorization request used for APM My bank payment method                                       | string             |         |
| NPG_MYBANK_PSP_LIST                     | List of all psp ids that are expected to be found into the NPG_MYBANK_PSP_KEYS configuration (used for configuration cross validation)                     | string             |         |
| NPG_SATISPAY_PSP_KEYS                   | Secret structure that holds psp - api keys association for authorization request used for APM Satispay payment method                                      | string             |         |
| NPG_SATISPAY_PSP_LIST                   | List of all psp ids that are expected to be found into the NPG_SATISPAY_PSP_KEYS configuration (used for configuration cross validation)                   | string             |         |
| NPG_APPLEPAY_PSP_KEYS                   | Secret structure that holds psp - api keys association for authorization request used for APM Apple pay payment method                                     | string             |         |
| NPG_APPLEPAY_PSP_LIST                   | List of all psp ids that are expected to be found into the NPG_APPLEPAY_PSP_KEYS configuration (used for configuration cross validation)                   | string             |         |
| PERSONAL_DATA_VAULT_API_KEY_EMAIL       | API Key for Personal Data Vault (PDV is used to safely encrypt PIIs, e.g. the user's email address)                                                        | string             |         |         
| PERSONAL_DATA_VAULT_API_KEY_FISCAL_CODE | A different API Key for Personal Data Vault (PDV is used to safely encrypt PIIs, e.g. the user's email address)                                            | string             |         |
| PERSONAL_DATA_VAULT_API_BASE_PATH       | API base path for Personal Data Vault                                                                                                                      | string             |         |         
| PM_ORACLE_HOST                          | PM Oracle DB hostname instance                                                                                                                             | hostname (string)  |         |
| PM_ORACLE_PORT                          | PM Oracle DB port                                                                                                                                          | int                |         |
| PM_ORACLE_DATABASE_NAME                 | PM Oracle DB database name                                                                                                                                 | string             |         |
| PM_ORACLE_USERNAME                      | PM Oracle DB username                                                                                                                                      | string             |         |
| PM_ORACLE_PASSWORD                      | PM Oracle DB password                                                                                                                                      | string             |         |
| SEARCH_DEAD_LETTER_QUEUE_MAPPING        | Dead letter search criteria to queue name mapping                                                                                                          | map(string,string) |         |
| SEARCH_PM_IN_ECOMMERCE_HISTORY_ENABLED  | Whether the search by fiscal code is made through the history (false) or ecommerce (true) database                                                         | string             | false   |
| NPG_GOOGLE_PAY_PSP_KEYS                 | Secret structure that holds psp - api keys association for authorization request used for APM Google pay payment method                                    | string             |         |
| NPG_GOOGLE_PAY_PSP_LIST                 | List of all psp ids that are expected to be found into the NPG_GOOGLE_PAY_PSP_KEYS configuration (used for configuration cross validation)                 | string             |         |
| SECURITY_API_KEY_SECURED_PATHS          | Secured paths for API Key                                                                                                                                  | string             |         |
| SECURITY_API_KEY_PRIMARY                | Primary API Key used to secure helpdesk-service service's APIs                                                                                             | string             |         |
| SECURITY_API_KEY_SECONDARY              | Secondary API Key used to secure helpdesk-service service's APIs                                                                                           | string             |         |
| GITHUB_TOKEN                            | GitHub Personal Access Token with packages:read permission for accessing pagopa-ecommerce-commons from GitHub Packages                                     | string             |         |
(*): for Mongo connection string options
see [docs](https://www.mongodb.com/docs/drivers/java/sync/v4.3/fundamentals/connection/connection-options/#connection-options)

### Run docker container

```shell
$ export GITHUB_TOKEN=your_github_token_with_packages_read_permission
$ docker compose up --build
```

---

## Develop Locally 💻

### Prerequisites

- git
- gradle
- jdk-21
- GitHub personal access token with `packages:read` permission

### GitHub Token Setup

To access the `pagopa-ecommerce-commons` library from GitHub Packages, you need to set up authentication:

1. Create a GitHub personal access token with `packages:read` permission
2. Set the token as an environment variable:

```shell
export GITHUB_TOKEN=your_github_token_with_packages_read_permission
```

### Run the project

```shell
$ ./gradlew bootRun
```

### eCommerce Commons Library

The service uses the `ecommerce-commons` library which is now distributed via GitHub Packages. The library version is configured in `build.gradle.kts`.

This two properties maps `ecommerce-commons` version and git ref:

````
val ecommerceCommonsVersion = "x.y.z" -> valued with ecommerce commons wanted pom version
val ecommerceCommonsGitRef = ecommerceCommonsVersion -> the branch/tag to be checkout.
````

`ecommerceCommonsGitRef` has by default the same value as `ecommerceCommonsVersion`, so that version tagged
with `"x.y.z"` will be checked out and installed locally.

This value was left as a separate property because, during developing phases can be changed to a feature branch
making the local build use a ref branch other than a tag for developing purpose.

The library is automatically downloaded from GitHub Packages during the build process using the configured GitHub token.

### Testing 🧪

#### Unit testing

To run the **Junit** tests:

```shell
$ ./gradlew test
```

#### Integration testing

TODO

#### Performance testing

install [k6](https://k6.io/) and then from `./performance-test/src`

1. `k6 run --env VARS=local.environment.json --env TEST_TYPE=./test-types/load.json main_scenario.js`

### Dependency management 🔧

For support reproducible build this project has the following gradle feature enabled:

- [dependency lock](https://docs.gradle.org/8.1/userguide/dependency_locking.html)
- [dependency verification](https://docs.gradle.org/8.1/userguide/dependency_verification.html)

#### Dependency lock

This feature use the content of `gradle.lockfile` to check the declared dependencies against the locked one.

If a transitive dependencies have been upgraded the build will fail because of the locked version mismatch.

The following command can be used to upgrade dependency lockfile:

```shell
./gradlew dependencies --write-locks 
```

Running the above command will cause the `gradle.lockfile` to be updated against the current project dependency
configuration

#### Dependency verification

This feature is enabled by adding the gradle `./gradle/verification-metadata.xml` configuration file.

Perform checksum comparison against dependency artifact (jar files, zip, ...) and metadata (pom.xml, gradle module
metadata, ...) used during build
and the ones stored into `verification-metadata.xml` file raising error during build in case of mismatch.

The following command can be used to recalculate dependency checksum:

```shell
./gradlew --write-verification-metadata sha256 clean spotlessApply build 
```

In the above command the `clean`, `spotlessApply` `build` tasks where chosen to be run
in order to discover all transitive dependencies used during build and also the ones used during
spotless apply task used to format source code.

The above command will upgrade the `verification-metadata.xml` adding all the newly discovered dependencies' checksum.
Those checksum should be checked against a trusted source to check for corrispondence with the library author published
checksum.

`/gradlew --write-verification-metadata sha256` command appends all new dependencies to the verification files but does
not remove
entries for unused dependencies.

This can make this file grow every time a dependency is upgraded.

To detect and remove old dependencies make the following steps:

1. Delete, if present, the `gradle/verification-metadata.dryrun.xml`
2. Run the gradle write-verification-metadata in dry-mode (this will generate a verification-metadata-dryrun.xml file
   leaving untouched the original verification file)
3. Compare the verification-metadata file and the verification-metadata.dryrun one checking for differences and removing
   old unused dependencies

The 1-2 steps can be performed with the following commands

```Shell
rm -f ./gradle/verification-metadata.dryrun.xml 
./gradlew --write-verification-metadata sha256 clean spotlessApply build --dry-run
```

The resulting `verification-metadata.xml` modifications must be reviewed carefully checking the generated
dependencies checksum against official websites or other secure sources.

If a dependency is not discovered during the above command execution it will lead to build errors.

You can add those dependencies manually by modifying the `verification-metadata.xml`
file adding the following component:

```xml

<verification-metadata>
    <!-- other configurations... -->
    <components>
        <!-- other components -->
        <component group="GROUP_ID" name="ARTIFACT_ID" version="VERSION">
            <artifact name="artifact-full-name.jar">
                <sha256 value="sha value"
                        origin="Description of the source of the checksum value"/>
            </artifact>
            <artifact name="artifact-pom-file.pom">
                <sha256 value="sha value"
                        origin="Description of the source of the checksum value"/>
            </artifact>
        </component>
    </components>
</verification-metadata>
```

Add those components at the end of the components list and then run the

```shell
./gradlew --write-verification-metadata sha256 clean spotlessApply build 
```

that will reorder the file with the added dependencies checksum in the expected order.

Finally, you can add new dependencies both to gradle.lockfile writing verification metadata running

```shell
 ./gradlew dependencies --write-locks --write-verification-metadata sha256
```

For more information read the
following [article](https://docs.gradle.org/8.1/userguide/dependency_verification.html#sec:checksum-verification)

## Contributors 👥

Made with ❤️ by PagoPA S.p.A.

### Maintainers

See `CODEOWNERS` file
