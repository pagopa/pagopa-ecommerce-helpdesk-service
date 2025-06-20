package it.pagopa.ecommerce.helpdesk.controllers.v2

import it.pagopa.ecommerce.helpdesk.HelpdeskTestUtilsV2
import it.pagopa.ecommerce.helpdesk.services.v2.HelpdeskService
import it.pagopa.generated.ecommerce.helpdesk.v2.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@OptIn(ExperimentalCoroutinesApi::class)
@WebFluxTest(HelpdeskController::class)
class HelpdeskControllerTest {
    @Autowired lateinit var webClient: WebTestClient

    @MockBean lateinit var helpdeskService: HelpdeskService

    @Test
    fun `post search transaction should return 400 for invalid page parameters`() = runTest {
        val pageNumber = -1 // Invalid value
        val pageSize = 25 // Invalid value (greater than max 20)
        val request = HelpdeskTestUtilsV2.buildSearchRequestByPaymentToken()

        webClient
            .post()
            .uri {
                it.path("/v2//helpdesk/searchTransaction")
                    .queryParam("pageNumber", pageNumber)
                    .queryParam("pageSize", pageSize)
                    .build()
            }
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-api-key", "primary-key")
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isBadRequest
    }
}
