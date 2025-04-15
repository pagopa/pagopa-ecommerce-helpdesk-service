package it.pagopa.ecommerce.helpdesk.controllers.v2

import it.pagopa.ecommerce.helpdesk.HelpdeskTestUtils
import it.pagopa.ecommerce.helpdesk.HelpdeskTestUtilsV2
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.services.v2.EcommerceService
import it.pagopa.generated.ecommerce.helpdesk.v2.model.*
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
@WebFluxTest(EcommerceController::class)
class EcommerceControllerTest {

    @Autowired lateinit var webClient: WebTestClient

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
                    .path("/v2/ecommerce/searchTransaction")
                    .queryParam("pageNumber", "{pageNumber}")
                    .queryParam("pageSize", "{pageSize}")
                    .build(pageNumber, pageSize)
            }
            .contentType(MediaType.APPLICATION_JSON)
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
                    .path("/v2/ecommerce/searchTransaction")
                    .queryParam("pageNumber", "{pageNumber}")
                    .queryParam("pageSize", "{pageSize}")
                    .build(pageNumber, pageSize)
            }
            .contentType(MediaType.APPLICATION_JSON)
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
                    .path("/v2/ecommerce/searchTransaction")
                    .queryParam("pageNumber", "{pageNumber}")
                    .queryParam("pageSize", "{pageSize}")
                    .build(pageNumber, pageSize)
            }
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    fun `post search transaction should return 404 for no transaction found`() = runTest {
        val pageNumber = 1
        val pageSize = 15
        val request = HelpdeskTestUtilsV2.buildSearchRequestByTransactionId()
        val expected =
            HelpdeskTestUtilsV2.buildProblemJson(
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
                    .path("/v2/ecommerce/searchTransaction")
                    .queryParam("pageNumber", "{pageNumber}")
                    .queryParam("pageSize", "{pageSize}")
                    .build(pageNumber, pageSize)
            }
            .contentType(MediaType.APPLICATION_JSON)
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
        val request = HelpdeskTestUtilsV2.buildSearchRequestByTransactionId().transactionId("")
        val expectedProblemJson =
            HelpdeskTestUtilsV2.buildProblemJson(
                httpStatus = HttpStatus.BAD_REQUEST,
                title = "Bad request",
                description = "Input request is invalid. Invalid fields: transactionId"
            )
        webClient
            .post()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/v2/ecommerce/searchTransaction")
                    .queryParam("pageNumber", "{pageNumber}")
                    .queryParam("pageSize", "{pageSize}")
                    .build(pageNumber, pageSize)
            }
            .contentType(MediaType.APPLICATION_JSON)
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
            val request = HelpdeskTestUtilsV2.buildSearchRequestByTransactionId()
            val expectedProblemJson =
                HelpdeskTestUtilsV2.buildProblemJson(
                    httpStatus = HttpStatus.BAD_REQUEST,
                    title = "Bad request",
                    description =
                        "Input request is invalid. Invalid fields: ecommerceSearchTransaction.pageSize"
                )
            webClient
                .post()
                .uri { uriBuilder ->
                    uriBuilder
                        .path("/v2/ecommerce/searchTransaction")
                        .queryParam("pageNumber", "{pageNumber}")
                        .queryParam("pageSize", "{pageSize}")
                        .build(pageNumber, pageSize)
                }
                .contentType(MediaType.APPLICATION_JSON)
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
            val request = HelpdeskTestUtilsV2.buildSearchRequestByTransactionId()
            val expected =
                HelpdeskTestUtilsV2.buildProblemJson(
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
                        .path("/v2/ecommerce/searchTransaction")
                        .queryParam("pageNumber", "{pageNumber}")
                        .queryParam("pageSize", "{pageSize}")
                        .build(pageNumber, pageSize)
                }
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody<ProblemJsonDto>()
                .isEqualTo(expected)
        }

    @Test
    fun `ecommerceSearchMetrics should return 200 OK with metrics`() = runTest {
        val request = HelpdeskTestUtilsV2.buildSearchMetrics()
        given(ecommerceService.searchMetrics(searchMetricsRequestDto = request))
            .willReturn(
                Mono.just(
                    TransactionMetricsResponseDto()
                        .ACTIVATED(1)
                        .AUTHORIZATION_REQUESTED(2)
                        .AUTHORIZATION_COMPLETED(3)
                        .CLOSURE_REQUESTED(4)
                        .CLOSED(5)
                        .CLOSURE_ERROR(6)
                        .NOTIFIED_OK(7)
                        .NOTIFIED_KO(8)
                        .NOTIFICATION_ERROR(9)
                        .NOTIFICATION_REQUESTED(10)
                        .EXPIRED(11)
                        .REFUNDED(12)
                        .CANCELED(13)
                        .EXPIRED_NOT_AUTHORIZED(14)
                        .UNAUTHORIZED(15)
                        .REFUND_ERROR(16)
                        .REFUND_REQUESTED(17)
                        .CANCELLATION_REQUESTED(18)
                        .CANCELLATION_EXPIRED(19)
                )
            )
        webClient
            .post()
            .uri { uriBuilder -> uriBuilder.path("/v2/ecommerce/searchMetrics").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    fun `post ecommerceSearchMetrics return 400 for invalid  parameters`() = runTest {
        val expectedProblemJson =
            HelpdeskTestUtilsV2.buildProblemJson(
                httpStatus = HttpStatus.BAD_REQUEST,
                title = "Bad request",
                description =
                    "Input request is invalid. Invalid fields: paymentTypeCode,timeRange,clientId,pspId"
            )

        webClient
            .post()
            .uri { uriBuilder -> uriBuilder.path("/v2/ecommerce/searchMetrics").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(SearchMetricsRequestDto())
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody<ProblemJsonDto>()
            .isEqualTo(expectedProblemJson)
    }
    @Test
    fun `post ecommerceSearchMetrics return 500 for invalid  parameters`() = runTest {
        val request = HelpdeskTestUtilsV2.buildSearchMetrics()
        val expectedProblemJson =
            HelpdeskTestUtilsV2.buildProblemJson(
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                title = "Error processing the request",
                description = "Generic error occurred"
            )
        given(ecommerceService.searchMetrics(searchMetricsRequestDto = request))
            .willReturn(Mono.error(RuntimeException("Unhandled error")))

        webClient
            .post()
            .uri { uriBuilder -> uriBuilder.path("/v2/ecommerce/searchMetrics").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .is5xxServerError
            .expectBody<ProblemJsonDto>()
            .isEqualTo(expectedProblemJson)
    }
}
