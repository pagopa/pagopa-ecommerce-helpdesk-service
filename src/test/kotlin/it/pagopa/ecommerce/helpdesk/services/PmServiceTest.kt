package it.pagopa.ecommerce.helpdesk.services

import it.pagopa.ecommerce.helpdesk.HelpdeskTestUtils
import it.pagopa.ecommerce.helpdesk.dataproviders.oracle.PMTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.generated.ecommerce.helpdesk.model.PageInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionResponseDto
import java.time.OffsetDateTime
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class PmServiceTest {

    private val pmTransactionDataProvider: PMTransactionDataProvider = mock()

    private val pmService = PmService(pmTransactionDataProvider)

    @Test
    fun `should return found transaction successfully`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByUserMail()
        val pageSize = 10
        val pageNumber = 0
        val totalCount = 100
        val transactions =
            listOf(HelpdeskTestUtils.buildTransactionResultDtoPM(OffsetDateTime.now()))
        given(pmTransactionDataProvider.totalRecordCount(searchCriteria))
            .willReturn(Mono.just(totalCount))
        given(
                pmTransactionDataProvider.findResult(
                    searchParams = searchCriteria,
                    pageSize = pageSize,
                    pageNumber = pageNumber
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
                pmService.searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    pmSearchTransactionRequestDto = searchCriteria
                )
            )
            .expectNext(expectedResponse)
            .verifyComplete()

        verify(pmTransactionDataProvider, times(1)).totalRecordCount(any())
        verify(pmTransactionDataProvider, times(1)).findResult(any(), any(), any())
    }

    @Test
    fun `should return error for no transaction found performing only count query`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByUserMail()
        val pageSize = 10
        val pageNumber = 0
        val totalCount = 0
        val transactions =
            listOf(HelpdeskTestUtils.buildTransactionResultDtoPM(OffsetDateTime.now()))
        given(pmTransactionDataProvider.totalRecordCount(searchCriteria))
            .willReturn(Mono.just(totalCount))

        val expectedResponse =
            SearchTransactionResponseDto()
                .transactions(transactions)
                .page(
                    PageInfoDto().results(transactions.size).total(totalCount).current(pageNumber)
                )
        StepVerifier.create(
                pmService.searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    pmSearchTransactionRequestDto = searchCriteria
                )
            )
            .expectError(NoResultFoundException::class.java)
            .verify()

        verify(pmTransactionDataProvider, times(1)).totalRecordCount(any())
        verify(pmTransactionDataProvider, times(0)).findResult(any(), any(), any())
    }
}
