package it.pagopa.ecommerce.helpdesk.services

import it.pagopa.ecommerce.helpdesk.HelpdeskTestUtils
import it.pagopa.ecommerce.helpdesk.dataproviders.mongo.EcommerceTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.generated.ecommerce.helpdesk.model.PageInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionResponseDto
import java.time.OffsetDateTime
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class EcommerceServiceTest {

    private val ecommerceTransactionDataProvider: EcommerceTransactionDataProvider = mock()

    private val ecommerceService = EcommerceService(ecommerceTransactionDataProvider)

    @Test
    fun `should return found transaction successfully`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByRptId()
        val pageSize = 10
        val pageNumber = 0
        val totalCount = 100
        val transactions =
            listOf(HelpdeskTestUtils.buildTransactionResultDtoPM(OffsetDateTime.now()))
        given(ecommerceTransactionDataProvider.totalRecordCount(searchCriteria))
            .willReturn(Mono.just(totalCount))
        given(
                ecommerceTransactionDataProvider.findResult(
                    searchParams = searchCriteria,
                    skip = pageSize * pageNumber,
                    limit = pageSize
                )
            )
            .willReturn(Mono.just(transactions))
        val expectedResponse =
            SearchTransactionResponseDto()
                .transactions(transactions)
                .page(
                    PageInfoDto().results(transactions.size).total(totalCount).current(pageNumber)
                )
        StepVerifier.create(
                ecommerceService.searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    ecommerceSearchTransactionRequestDto = searchCriteria
                )
            )
            .expectNext(expectedResponse)
            .verifyComplete()

        verify(ecommerceTransactionDataProvider, times(1)).totalRecordCount(any())
        verify(ecommerceTransactionDataProvider, times(1)).findResult(any(), any(), any())
    }

    @Test
    fun `should return error for no transaction found performing only count query`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByRptId()
        val pageSize = 10
        val pageNumber = 0
        val totalCount = 0
        given(ecommerceTransactionDataProvider.totalRecordCount(searchCriteria))
            .willReturn(Mono.just(totalCount))
        StepVerifier.create(
                ecommerceService.searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    ecommerceSearchTransactionRequestDto = searchCriteria
                )
            )
            .expectError(NoResultFoundException::class.java)
            .verify()

        verify(ecommerceTransactionDataProvider, times(1)).totalRecordCount(any())
        verify(ecommerceTransactionDataProvider, times(0)).findResult(any(), any(), any())
    }
}
