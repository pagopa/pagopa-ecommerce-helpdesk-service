package it.pagopa.ecommerce.helpdesk.controllers.v1

import it.pagopa.ecommerce.helpdesk.HelpdeskTestUtils
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.services.v1.PmService
import it.pagopa.generated.ecommerce.helpdesk.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import reactor.core.publisher.Mono

@OptIn(ExperimentalCoroutinesApi::class)
@WebFluxTest(PmController::class)
class PmControllerTest {
    @Autowired lateinit var webClient: WebTestClient

    @MockBean lateinit var pmService: PmService

    @Test
    fun `post search transaction succeeded searching by user email`() = runTest {
        val pageNumber = 1
        val pageSize = 15
        val request = HelpdeskTestUtils.buildSearchRequestByUserMail("test@test.it")
        given(
                pmService.searchTransaction(
                    pageNumber = eq(pageNumber),
                    pageSize = eq(pageSize),
                    pmSearchTransactionRequestDto =
                        argThat {
                            this is SearchTransactionRequestEmailDto &&
                                this.userEmail == request.userEmail
                        }
                )
            )
            .willReturn(Mono.just(SearchTransactionResponseDto()))
        webClient
            .post()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/pm/searchTransaction")
                    .queryParam("pageNumber", "{pageNumber}")
                    .queryParam("pageSize", "{pageSize}")
                    .build(pageNumber, pageSize)
            }
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-api-key", "primary-key")
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    fun `post search transaction succeeded searching by user fiscal code`() = runTest {
        val pageNumber = 1
        val pageSize = 15
        val request = HelpdeskTestUtils.buildSearchRequestByUserFiscalCode("AAABBB91E22A123A")
        given(
                pmService.searchTransaction(
                    pageNumber = eq(pageNumber),
                    pageSize = eq(pageSize),
                    pmSearchTransactionRequestDto =
                        argThat {
                            this is SearchTransactionRequestFiscalCodeDto &&
                                this.userFiscalCode == request.userFiscalCode
                        }
                )
            )
            .willReturn(Mono.just(SearchTransactionResponseDto()))
        webClient
            .post()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/pm/searchTransaction")
                    .queryParam("pageNumber", "{pageNumber}")
                    .queryParam("pageSize", "{pageSize}")
                    .build(pageNumber, pageSize)
            }
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-api-key", "primary-key")
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    fun `post search transaction should return 404 for no transaction found`() = runTest {
        val pageNumber = 1
        val pageSize = 15
        val request = HelpdeskTestUtils.buildSearchRequestByUserMail("test@test.it")
        val expected =
            HelpdeskTestUtils.buildProblemJson(
                httpStatus = HttpStatus.NOT_FOUND,
                title = "No result found",
                description = "No result can be found searching for criteria ${request.type}"
            )
        given(
                pmService.searchTransaction(
                    pageNumber = eq(pageNumber),
                    pageSize = eq(pageSize),
                    pmSearchTransactionRequestDto =
                        argThat {
                            this is SearchTransactionRequestEmailDto &&
                                this.userEmail == request.userEmail
                        }
                )
            )
            .willReturn(Mono.error(NoResultFoundException(request.type)))
        webClient
            .post()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/pm/searchTransaction")
                    .queryParam("pageNumber", "{pageNumber}")
                    .queryParam("pageSize", "{pageSize}")
                    .build(pageNumber, pageSize)
            }
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-api-key", "primary-key")
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isNotFound
            .expectBody<ProblemJsonDto>()
            .isEqualTo(expected)
    }

    @Test
    fun `post search transaction should return 400 for bad request`() = runTest {
        val pageNumber = 1
        val pageSize = 1
        val request = HelpdeskTestUtils.buildSearchRequestByUserMail("")
        val expectedProblemJson =
            HelpdeskTestUtils.buildProblemJson(
                httpStatus = HttpStatus.BAD_REQUEST,
                title = "Bad request",
                description = "Input request is invalid. Invalid fields: userEmail"
            )
        webClient
            .post()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/pm/searchTransaction")
                    .queryParam("pageNumber", "{pageNumber}")
                    .queryParam("pageSize", "{pageSize}")
                    .build(pageNumber, pageSize)
            }
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-api-key", "primary-key")
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody(ProblemJsonDto::class.java)
            .isEqualTo(expectedProblemJson)
    }

    @Test
    fun `post search transaction should return 400 for invalid query page query parameters`() =
        runTest {
            val pageNumber = 0
            val pageSize = Int.MAX_VALUE
            val request = HelpdeskTestUtils.buildSearchRequestByTransactionId()
            val expectedProblemJson =
                HelpdeskTestUtils.buildProblemJson(
                    httpStatus = HttpStatus.BAD_REQUEST,
                    title = "Bad request",
                    description =
                        "Input request is invalid. Invalid fields: pmSearchTransaction.pageSize"
                )
            webClient
                .post()
                .uri { uriBuilder ->
                    uriBuilder
                        .path("/pm/searchTransaction")
                        .queryParam("pageNumber", "{pageNumber}")
                        .queryParam("pageSize", "{pageSize}")
                        .build(pageNumber, pageSize)
                }
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-api-key", "primary-key")
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest
                .expectBody(ProblemJsonDto::class.java)
                .isEqualTo(expectedProblemJson)
        }

    @Test
    fun `post search transaction should return 500 for unhandled error processing request`() =
        runTest {
            val pageNumber = 1
            val pageSize = 15
            val request = HelpdeskTestUtils.buildSearchRequestByUserMail("test@test.it")
            val expected =
                HelpdeskTestUtils.buildProblemJson(
                    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                    title = "Error processing the request",
                    description = "Generic error occurred"
                )
            given(
                    pmService.searchTransaction(
                        pageNumber = eq(pageNumber),
                        pageSize = eq(pageSize),
                        pmSearchTransactionRequestDto =
                            argThat {
                                this is SearchTransactionRequestEmailDto &&
                                    this.userEmail == request.userEmail
                            }
                    )
                )
                .willReturn(Mono.error(RuntimeException("Unhandled error")))
            webClient
                .post()
                .uri { uriBuilder ->
                    uriBuilder
                        .path("/pm/searchTransaction")
                        .queryParam("pageNumber", "{pageNumber}")
                        .queryParam("pageSize", "{pageSize}")
                        .build(pageNumber, pageSize)
                }
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-api-key", "primary-key")
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody<ProblemJsonDto>()
                .isEqualTo(expected)
        }

    @Test
    fun `post search payment method succeeded searching by user fiscal code`() = runTest {
        val request =
            HelpdeskTestUtils.buildPaymentMethodSearchRequestByUserFiscalCode("RHFGDH98HG02DH7U")
        given(
                pmService.searchPaymentMethod(
                    pmSearchPaymentMethodRequestDto =
                        argThat {
                            this is SearchPaymentMethodRequestFiscalCodeDto &&
                                this.userFiscalCode == request.userFiscalCode
                        }
                )
            )
            .willReturn(Mono.just(SearchPaymentMethodResponseDto()))
        webClient
            .post()
            .uri { uriBuilder -> uriBuilder.path("/pm/searchPaymentMethod").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-api-key", "primary-key")
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    fun `post search payment method failed for invalid fiscal code`() = runTest {
        val request =
            HelpdeskTestUtils.buildPaymentMethodSearchRequestByUserFiscalCode("invalidFiscalCode")
        given(
                pmService.searchPaymentMethod(
                    pmSearchPaymentMethodRequestDto =
                        argThat {
                            this is SearchPaymentMethodRequestFiscalCodeDto &&
                                this.userFiscalCode == request.userFiscalCode
                        }
                )
            )
            .willReturn(Mono.just(SearchPaymentMethodResponseDto()))
        webClient
            .post()
            .uri { uriBuilder -> uriBuilder.path("/pm/searchPaymentMethod").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-api-key", "primary-key")
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `post search payment method succeeded searching by user email`() = runTest {
        val request =
            HelpdeskTestUtils.buildPaymentMethodSearchRequestByUserEmail("mail.test@email.com")
        given(
                pmService.searchPaymentMethod(
                    pmSearchPaymentMethodRequestDto =
                        argThat {
                            this is SearchPaymentMethodRequestEmailDto &&
                                this.userEmail == request.userEmail
                        }
                )
            )
            .willReturn(Mono.just(SearchPaymentMethodResponseDto()))
        webClient
            .post()
            .uri { uriBuilder -> uriBuilder.path("/pm/searchPaymentMethod").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-api-key", "primary-key")
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    fun `post search payment method failed for invalid email`() = runTest {
        val request = HelpdeskTestUtils.buildPaymentMethodSearchRequestByUserEmail("invalid_email")
        given(
                pmService.searchPaymentMethod(
                    pmSearchPaymentMethodRequestDto =
                        argThat {
                            this is SearchPaymentMethodRequestEmailDto &&
                                this.userEmail == request.userEmail
                        }
                )
            )
            .willReturn(Mono.just(SearchPaymentMethodResponseDto()))
        webClient
            .post()
            .uri { uriBuilder -> uriBuilder.path("/pm/searchPaymentMethod").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-api-key", "primary-key")
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `post search payment method should return 500 for unhandled error processing request`() =
        runTest {
            val request =
                HelpdeskTestUtils.buildPaymentMethodSearchRequestByUserEmail("mail.test@email.com")
            val expected =
                HelpdeskTestUtils.buildProblemJson(
                    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                    title = "Error processing the request",
                    description = "Generic error occurred"
                )
            given(
                    pmService.searchPaymentMethod(
                        pmSearchPaymentMethodRequestDto =
                            argThat {
                                this is SearchPaymentMethodRequestEmailDto &&
                                    this.userEmail == request.userEmail
                            }
                    )
                )
                .willReturn(Mono.error(RuntimeException("Unhandled error")))
            webClient
                .post()
                .uri { uriBuilder -> uriBuilder.path("/pm/searchPaymentMethod").build() }
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-api-key", "primary-key")
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody<ProblemJsonDto>()
                .isEqualTo(expected)
        }

    @Test
    fun `post search bulk transaction succeeded`() = runTest {
        val transactionIdRangeDto =
            SearchTransactionRequestTransactionIdRangeTransactionIdRangeDto()
                .startTransactionId("1")
                .endTransactionId("1000")
        val request =
            HelpdeskTestUtils.buildBulkSearchRequest("TRANSACTION_ID_RANGE", transactionIdRangeDto)
        val responseList = listOf(TransactionBulkResultDto(), TransactionBulkResultDto())
        given(
                pmService.searchBulkTransaction(
                    argThat {
                        this is SearchTransactionRequestTransactionIdRangeDto &&
                            this.transactionIdRange == request.transactionIdRange
                    }
                )
            )
            .willReturn(Mono.just(responseList))

        webClient
            .post()
            .uri { uriBuilder -> uriBuilder.path("/pm/searchBulkTransaction").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-api-key", "primary-key")
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
            .expectBodyList(TransactionBulkResultDto::class.java)
    }

    @Test
    fun `post search bulk transaction should return 400 for bad request`() = runTest {
        val transactionIdRangeDto =
            SearchTransactionRequestTransactionIdRangeTransactionIdRangeDto()
                .startTransactionId("1")
        val request =
            HelpdeskTestUtils.buildBulkSearchRequest("TRANSACTION_ID_RANGE", transactionIdRangeDto)

        val expectedProblemJson =
            HelpdeskTestUtils.buildProblemJson(
                httpStatus = HttpStatus.BAD_REQUEST,
                title = "Bad request",
                description =
                    "Input request is invalid. Invalid fields: transactionIdRange.endTransactionId"
            )
        webClient
            .post()
            .uri { uriBuilder -> uriBuilder.path("/pm/searchBulkTransaction").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-api-key", "primary-key")
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody(ProblemJsonDto::class.java)
            .isEqualTo(expectedProblemJson)
    }

    @Test
    fun `post search bulk transaction should return 500 for unhandled error processing request`() =
        runTest {
            val transactionIdRangeDto =
                SearchTransactionRequestTransactionIdRangeTransactionIdRangeDto()
                    .startTransactionId("1")
                    .endTransactionId("100")
            val request =
                HelpdeskTestUtils.buildBulkSearchRequest(
                    "TRANSACTION_ID_RANGE",
                    transactionIdRangeDto
                )
            val expected =
                HelpdeskTestUtils.buildProblemJson(
                    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                    title = "Error processing the request",
                    description = "Generic error occurred"
                )
            given(
                    pmService.searchBulkTransaction(
                        argThat {
                            this is SearchTransactionRequestTransactionIdRangeDto &&
                                this.transactionIdRange == request.transactionIdRange
                        }
                    )
                )
                .willReturn(Mono.error(RuntimeException("Unhandled error")))
            webClient
                .post()
                .uri { uriBuilder -> uriBuilder.path("/pm/searchBulkTransaction").build() }
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-api-key", "primary-key")
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody<ProblemJsonDto>()
                .isEqualTo(expected)
        }

    @Test
    fun `should return unauthorized if request has not api key header`() = runTest {
        val pageNumber = 1
        val pageSize = 15
        val request = HelpdeskTestUtils.buildSearchRequestByUserMail("test@test.it")
        given(
                pmService.searchTransaction(
                    pageNumber = eq(pageNumber),
                    pageSize = eq(pageSize),
                    pmSearchTransactionRequestDto =
                        argThat {
                            this is SearchTransactionRequestEmailDto &&
                                this.userEmail == request.userEmail
                        }
                )
            )
            .willReturn(Mono.just(SearchTransactionResponseDto()))
        webClient
            .post()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/pm/searchTransaction")
                    .queryParam("pageNumber", "{pageNumber}")
                    .queryParam("pageSize", "{pageSize}")
                    .build(pageNumber, pageSize)
            }
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `should return unauthorized if request has wrong api key header`() = runTest {
        val pageNumber = 1
        val pageSize = 15
        val request = HelpdeskTestUtils.buildSearchRequestByUserMail("test@test.it")
        given(
                pmService.searchTransaction(
                    pageNumber = eq(pageNumber),
                    pageSize = eq(pageSize),
                    pmSearchTransactionRequestDto =
                        argThat {
                            this is SearchTransactionRequestEmailDto &&
                                this.userEmail == request.userEmail
                        }
                )
            )
            .willReturn(Mono.just(SearchTransactionResponseDto()))
        webClient
            .post()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/pm/searchTransaction")
                    .queryParam("pageNumber", "{pageNumber}")
                    .queryParam("pageSize", "{pageSize}")
                    .build(pageNumber, pageSize)
            }
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-api-key", "super-wrong-api-key")
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.UNAUTHORIZED)
    }
}
