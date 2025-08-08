package it.pagopa.ecommerce.helpdesk.controllers.v1

import it.pagopa.ecommerce.helpdesk.HelpdeskTestUtils
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.services.v1.EcommerceService
import it.pagopa.generated.ecommerce.helpdesk.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import jakarta.inject.Inject
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import reactor.core.publisher.Mono

@OptIn(ExperimentalCoroutinesApi::class)
@WebFluxTest(EcommerceController::class)
class EcommerceControllerTest {

    @Inject lateinit var webClient: WebTestClient

    @MockBean lateinit var ecommerceService: EcommerceService

    @Test
    fun `post search transaction succeeded searching by payment token`() = runTest {
        val pageNumber = 1
        val pageSize = 15
        val request = HelpdeskTestUtils.buildSearchRequestByPaymentToken()
        given(
                ecommerceService.searchTransaction(
                    pageNumber = eq(pageNumber),
                    pageSize = eq(pageSize),
                    ecommerceSearchTransactionRequestDto =
                        argThat {
                            this is SearchTransactionRequestPaymentTokenDto &&
                                this.paymentToken == request.paymentToken
                        }
                )
            )
            .willReturn(Mono.just(SearchTransactionResponseDto()))
        webClient
            .post()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/ecommerce/searchTransaction")
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
    fun `post search transaction succeeded searching by payment token with secondary key`() =
        runTest {
            val pageNumber = 1
            val pageSize = 15
            val request = HelpdeskTestUtils.buildSearchRequestByPaymentToken()
            given(
                    ecommerceService.searchTransaction(
                        pageNumber = eq(pageNumber),
                        pageSize = eq(pageSize),
                        ecommerceSearchTransactionRequestDto =
                            argThat {
                                this is SearchTransactionRequestPaymentTokenDto &&
                                    this.paymentToken == request.paymentToken
                            }
                    )
                )
                .willReturn(Mono.just(SearchTransactionResponseDto()))
            webClient
                .post()
                .uri { uriBuilder ->
                    uriBuilder
                        .path("/ecommerce/searchTransaction")
                        .queryParam("pageNumber", "{pageNumber}")
                        .queryParam("pageSize", "{pageSize}")
                        .build(pageNumber, pageSize)
                }
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-api-key", "secondary-key")
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isOk
        }

    @Test
    fun `post search transaction succeeded searching by rpt id`() = runTest {
        val pageNumber = 1
        val pageSize = 15
        val request = HelpdeskTestUtils.buildSearchRequestByRptId()
        given(
                ecommerceService.searchTransaction(
                    pageNumber = eq(pageNumber),
                    pageSize = eq(pageSize),
                    ecommerceSearchTransactionRequestDto =
                        argThat {
                            this is SearchTransactionRequestRptIdDto && this.rptId == request.rptId
                        }
                )
            )
            .willReturn(Mono.just(SearchTransactionResponseDto()))
        webClient
            .post()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/ecommerce/searchTransaction")
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
    fun `post search transaction succeeded searching by transaction id`() = runTest {
        val pageNumber = 1
        val pageSize = 15
        val request = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        given(
                ecommerceService.searchTransaction(
                    pageNumber = eq(pageNumber),
                    pageSize = eq(pageSize),
                    ecommerceSearchTransactionRequestDto =
                        argThat {
                            this is SearchTransactionRequestTransactionIdDto &&
                                this.transactionId == request.transactionId
                        }
                )
            )
            .willReturn(Mono.just(SearchTransactionResponseDto()))
        webClient
            .post()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/ecommerce/searchTransaction")
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
        val request = HelpdeskTestUtils.buildSearchRequestByTransactionId()
        val expected =
            HelpdeskTestUtils.buildProblemJson(
                httpStatus = HttpStatus.NOT_FOUND,
                title = "No result found",
                description = "No result can be found searching for criteria ${request.type}"
            )
        given(
                ecommerceService.searchTransaction(
                    pageNumber = eq(pageNumber),
                    pageSize = eq(pageSize),
                    ecommerceSearchTransactionRequestDto =
                        argThat {
                            this is SearchTransactionRequestTransactionIdDto &&
                                this.transactionId == request.transactionId
                        }
                )
            )
            .willReturn(Mono.error(NoResultFoundException(request.type)))
        webClient
            .post()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/ecommerce/searchTransaction")
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
    fun `post search transaction should return 400 for invalid request body`() = runTest {
        val pageNumber = 1
        val pageSize = 1
        val request = HelpdeskTestUtils.buildSearchRequestByTransactionId().transactionId("")
        val expectedProblemJson =
            HelpdeskTestUtils.buildProblemJson(
                httpStatus = HttpStatus.BAD_REQUEST,
                title = "Bad request",
                description = "Input request is invalid. Invalid fields: transactionId"
            )
        webClient
            .post()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/ecommerce/searchTransaction")
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
                        "Input request is invalid. Invalid fields: ecommerceSearchTransaction.pageSize"
                )
            webClient
                .post()
                .uri { uriBuilder ->
                    uriBuilder
                        .path("/ecommerce/searchTransaction")
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
            val request = HelpdeskTestUtils.buildSearchRequestByTransactionId()
            val expected =
                HelpdeskTestUtils.buildProblemJson(
                    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                    title = "Error processing the request",
                    description = "Generic error occurred"
                )
            given(
                    ecommerceService.searchTransaction(
                        pageNumber = eq(pageNumber),
                        pageSize = eq(pageSize),
                        ecommerceSearchTransactionRequestDto =
                            argThat {
                                this is SearchTransactionRequestTransactionIdDto &&
                                    this.transactionId == request.transactionId
                            }
                    )
                )
                .willReturn(Mono.error(RuntimeException("Unhandled error")))
            webClient
                .post()
                .uri { uriBuilder ->
                    uriBuilder
                        .path("/ecommerce/searchTransaction")
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
    fun `Post search dead letter should return records successfully searching for all dead letter events without time range`() =
        runTest {
            val pageNumber = 1
            val pageSize = 15
            val request =
                EcommerceSearchDeadLetterEventsRequestDto()
                    .source(DeadLetterSearchEventSourceDto.ALL)
            val expected = SearchDeadLetterEventResponseDto()

            given(
                    ecommerceService.searchDeadLetterEvents(
                        pageNumber = pageNumber,
                        pageSize = pageSize,
                        searchRequest = request
                    )
                )
                .willReturn(Mono.just(SearchDeadLetterEventResponseDto()))
            webClient
                .post()
                .uri { uriBuilder ->
                    uriBuilder
                        .path("/ecommerce/searchDeadLetterEvents")
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
                .expectBody<SearchDeadLetterEventResponseDto>()
                .isEqualTo(expected)
        }

    @Test
    fun `post search NPG operations succeeded`() = runTest {
        val transactionId = "3fa85f6457174562b3fc2c963f66afa6"
        val request = SearchNpgOperationsRequestDto().idTransaction(transactionId)
        val response = SearchNpgOperationsResponseDto()

        given(ecommerceService.searchNpgOperations(transactionId = eq(transactionId)))
            .willReturn(Mono.just(response))

        webClient
            .post()
            .uri { uriBuilder -> uriBuilder.path("/ecommerce/searchNpgOperations").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-api-key", "primary-key")
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<SearchNpgOperationsResponseDto>()
            .isEqualTo(response)
    }

    @Test
    fun `post search NPG operations should return 404 for no operations found`() = runTest {
        val transactionId = "3fa85f6457174562b3fc2c963f66afa7"
        val request = SearchNpgOperationsRequestDto().idTransaction(transactionId)
        val expectedProblemJson =
            HelpdeskTestUtils.buildProblemJson(
                httpStatus = HttpStatus.NOT_FOUND,
                title = "No result found",
                description = "No result can be found searching for criteria $transactionId"
            )

        given(ecommerceService.searchNpgOperations(transactionId = eq(transactionId)))
            .willReturn(Mono.error(NoResultFoundException(transactionId)))

        webClient
            .post()
            .uri { uriBuilder -> uriBuilder.path("/ecommerce/searchNpgOperations").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-api-key", "primary-key")
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isNotFound
            .expectBody<ProblemJsonDto>()
            .isEqualTo(expectedProblemJson)
    }

    @Test
    fun `post search NPG operations should return 400 for invalid request body`() = runTest {
        val request = SearchNpgOperationsRequestDto().idTransaction("")
        val expectedProblemJson =
            HelpdeskTestUtils.buildProblemJson(
                httpStatus = HttpStatus.BAD_REQUEST,
                title = "Bad request",
                description = "Input request is invalid. Invalid fields: idTransaction"
            )

        webClient
            .post()
            .uri { uriBuilder -> uriBuilder.path("/ecommerce/searchNpgOperations").build() }
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
    fun `should return unauthorized if request has not api key header`() = runTest {
        val pageNumber = 1
        val pageSize = 15
        val request = HelpdeskTestUtils.buildSearchRequestByPaymentToken()
        webClient
            .post()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/ecommerce/searchTransaction")
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
        val request = HelpdeskTestUtils.buildSearchRequestByPaymentToken()
        webClient
            .post()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/ecommerce/searchTransaction")
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
