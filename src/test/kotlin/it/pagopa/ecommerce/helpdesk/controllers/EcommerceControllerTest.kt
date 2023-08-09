package it.pagopa.ecommerce.helpdesk.controllers

import it.pagopa.ecommerce.helpdesk.services.EcommerceService
import it.pagopa.generated.ecommerce.helpdesk.model.EcommerceSearchTransactionRequestPaymentTokenDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionResponseDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

@OptIn(ExperimentalCoroutinesApi::class)
@WebFluxTest(EcommerceController::class)
class EcommerceControllerTest {

    @Autowired
    lateinit var webClient: WebTestClient

    @MockBean
    lateinit var ecommerceService: EcommerceService

    @Test
    fun `post search transaction succeeded`() = runTest {
        val pageNumber = 0
        val pageSize = 0
        val request = EcommerceSearchTransactionRequestPaymentTokenDto()
            .paymentToken("paymentToken")
            .type("PAYMENT_TOKEN")
        given(
            ecommerceService.searchTransaction(
                pageNumber = pageNumber,
                pageSize = pageSize,
                ecommerceSearchTransactionRequestDto = Mono.just(request)
            )
        ).willReturn(Mono.just(SearchTransactionResponseDto()))
        webClient
            .post()
            .uri("/ecommerce/searchTransaction")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
    }

}