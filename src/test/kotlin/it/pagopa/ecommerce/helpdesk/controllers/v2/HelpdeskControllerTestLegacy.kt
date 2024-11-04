package it.pagopa.ecommerce.helpdesk.controllers.v2

import it.pagopa.ecommerce.helpdesk.HelpdeskTestUtilsV2
import it.pagopa.ecommerce.helpdesk.services.v2.HelpdeskService
import it.pagopa.ecommerce.helpdesk.utils.PmProviderType
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchTransactionRequestPaymentTokenDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchTransactionResponseDto
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
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import reactor.core.publisher.Mono

@OptIn(ExperimentalCoroutinesApi::class)
@WebFluxTest(HelpdeskController::class)
@TestPropertySource(properties = ["search.pm.in.ecommerce.history.enabled=false"])
class HelpdeskControllerTestLegacy {

    @Autowired lateinit var webClient: WebTestClient

    @MockBean lateinit var helpdeskService: HelpdeskService

    @Test
    fun `post search transaction should use PM_LEGACY when searchPmInEcommerceHistory is false`() =
        runTest {
            val pageNumber = 0
            val pageSize = 20
            val request = HelpdeskTestUtilsV2.buildSearchRequestByPaymentToken()
            val response = SearchTransactionResponseDto()

            given(
                    helpdeskService.searchTransaction(
                        pageNumber = eq(pageNumber),
                        pageSize = eq(pageSize),
                        searchTransactionRequestDto =
                            argThat {
                                this is SearchTransactionRequestPaymentTokenDto &&
                                    this.paymentToken == request.paymentToken
                            },
                        pmProviderType = eq(PmProviderType.PM_LEGACY)
                    )
                )
                .willReturn(Mono.just(response))

            webClient
                .post()
                .uri {
                    it.path("/v2/helpdesk/searchTransaction")
                        .queryParam("pageNumber", pageNumber)
                        .queryParam("pageSize", pageSize)
                        .build()
                }
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody<SearchTransactionResponseDto>()
                .isEqualTo(response)
        }

    @Test
    fun `post search transaction should handle service error gracefully`() = runTest {
        val pageNumber = 0
        val pageSize = 15
        val request = HelpdeskTestUtilsV2.buildSearchRequestByPaymentToken()

        given(
                helpdeskService.searchTransaction(
                    pageNumber = eq(pageNumber),
                    pageSize = eq(pageSize),
                    searchTransactionRequestDto =
                        argThat {
                            this is SearchTransactionRequestPaymentTokenDto &&
                                this.paymentToken == request.paymentToken
                        },
                    pmProviderType = eq(PmProviderType.PM_LEGACY)
                )
            )
            .willReturn(Mono.error(RuntimeException("Unhandled error")))

        webClient
            .post()
            .uri {
                it.path("/v2//helpdesk/searchTransaction")
                    .queryParam("pageNumber", pageNumber)
                    .queryParam("pageSize", pageSize)
                    .build()
            }
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
