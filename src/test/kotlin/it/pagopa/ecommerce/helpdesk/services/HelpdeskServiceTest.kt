package it.pagopa.ecommerce.helpdesk.services

import it.pagopa.ecommerce.helpdesk.HelpdeskTestUtils
import it.pagopa.ecommerce.helpdesk.dataproviders.mongo.EcommerceTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.oracle.PMTransactionDataProvider
import it.pagopa.generated.ecommerce.helpdesk.model.PageInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.ProductDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionResponseDto
import java.time.OffsetDateTime
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class HelpdeskServiceTest {

    private val pmTransactionDataProvider: PMTransactionDataProvider = mock()

    private val ecommerceTransactionDataProvider: EcommerceTransactionDataProvider = mock()

    private val helpdeskService =
        HelpdeskService(
            pmTransactionDataProvider = pmTransactionDataProvider,
            ecommerceTransactionDataProvider = ecommerceTransactionDataProvider
        )

    @Test
    fun `Should recover records from eCommerce DB only`() {
        val totalEcommerceCount = 5
        val totalPmCount = 5
        val pageSize = 4
        val pageNumber = 0
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByUserMail("test@test.it")
        val results =
            listOf(
                HelpdeskTestUtils.buildTransactionResultDto(
                    OffsetDateTime.now(),
                    ProductDto.ECOMMERCE
                )
            )
        given(ecommerceTransactionDataProvider.totalRecordCount(searchCriteria))
            .willReturn(Mono.just(totalEcommerceCount))
        given(pmTransactionDataProvider.totalRecordCount(searchCriteria))
            .willReturn(Mono.just(totalPmCount))
        given(
                ecommerceTransactionDataProvider.findResult(
                    searchParams = eq(searchCriteria),
                    limit = any(),
                    skip = any()
                )
            )
            .willReturn(Mono.just(results))

        StepVerifier.create(
                helpdeskService.searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    searchTransactionRequestDto = searchCriteria
                )
            )
            .expectNext(
                SearchTransactionResponseDto()
                    .transactions(results)
                    .page(PageInfoDto().current(0).total(3).results(1))
            )
            .verifyComplete()
        verify(pmTransactionDataProvider, times(1)).totalRecordCount(searchCriteria)
        verify(ecommerceTransactionDataProvider, times(1)).totalRecordCount(searchCriteria)
        verify(ecommerceTransactionDataProvider, times(1))
            .findResult(skip = 0, limit = 4, searchParams = searchCriteria)
        verify(pmTransactionDataProvider, times(0))
            .findResult(skip = any(), limit = any(), searchParams = any())
    }
}
