package it.pagopa.ecommerce.helpdesk.services

import it.pagopa.ecommerce.helpdesk.HelpdeskTestUtils
import it.pagopa.ecommerce.helpdesk.dataproviders.oracle.PMPaymentMethodsDataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.oracle.PMTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.generated.ecommerce.helpdesk.model.PageInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.ProductDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionResponseDto
import java.time.OffsetDateTime
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class PmServiceTest {

    private val pmTransactionDataProvider: PMTransactionDataProvider = mock()
    private val pmPaymentMethodsDataProvider: PMPaymentMethodsDataProvider = mock()

    private val pmService = PmService(pmTransactionDataProvider, pmPaymentMethodsDataProvider)

    @Test
    fun `should return found transaction successfully`() {
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByUserMail("test@test.it")
        val pageSize = 10
        val pageNumber = 0
        val totalCount = 100
        val transactions =
            listOf(HelpdeskTestUtils.buildTransactionResultDto(OffsetDateTime.now(), ProductDto.PM))
        given(pmTransactionDataProvider.totalRecordCount(searchCriteria))
            .willReturn(Mono.just(totalCount))
        given(
                pmTransactionDataProvider.findResult(
                    searchParams = searchCriteria,
                    skip = pageSize * pageNumber,
                    limit = pageSize
                )
            )
            .willReturn(Mono.just(transactions))
        val expectedResponse =
            SearchTransactionResponseDto()
                .transactions(transactions)
                .page(PageInfoDto().results(transactions.size).total(10).current(pageNumber))
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
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByUserMail("unknown@test.it")
        val pageSize = 10
        val pageNumber = 0
        val totalCount = 0
        given(pmTransactionDataProvider.totalRecordCount(searchCriteria))
            .willReturn(Mono.just(totalCount))
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

    @Test
    fun `should return found payment methods successfully using fiscal code`() {
        val searchCriteria =
            HelpdeskTestUtils.buildPaymentMethodSearchRequestByUserFiscalCode("GOGFT675GGEY98IT")
        val response = HelpdeskTestUtils.buildSearchPaymentMethodResponseDto()
        given(pmPaymentMethodsDataProvider.findResult(searchParams = searchCriteria))
            .willReturn(Mono.just(response))
        StepVerifier.create(
                pmService.searchPaymentMethods(pmSearchPaymentMethodsRequestDto = searchCriteria)
            )
            .expectNext(response)
            .verifyComplete()

        verify(pmTransactionDataProvider, times(0)).totalRecordCount(any())
        verify(pmTransactionDataProvider, times(0)).findResult(any(), any(), any())
        verify(pmPaymentMethodsDataProvider, times(1)).findResult(any())
    }

    @Test
    fun `should return found payment methods successfully using email`() {
        val searchCriteria =
            HelpdeskTestUtils.buildPaymentMethodSearchRequestByUserEmail("test@test.it")
        val response = HelpdeskTestUtils.buildSearchPaymentMethodResponseDto()
        given(pmPaymentMethodsDataProvider.findResult(searchParams = searchCriteria))
            .willReturn(Mono.just(response))
        StepVerifier.create(
                pmService.searchPaymentMethods(pmSearchPaymentMethodsRequestDto = searchCriteria)
            )
            .expectNext(response)
            .verifyComplete()

        verify(pmTransactionDataProvider, times(0)).totalRecordCount(any())
        verify(pmTransactionDataProvider, times(0)).findResult(any(), any(), any())
        verify(pmPaymentMethodsDataProvider, times(1)).findResult(any())
    }
}
