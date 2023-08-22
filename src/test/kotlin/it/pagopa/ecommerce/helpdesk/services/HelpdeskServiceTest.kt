package it.pagopa.ecommerce.helpdesk.services

import it.pagopa.ecommerce.helpdesk.HelpdeskTestUtils
import it.pagopa.ecommerce.helpdesk.dataproviders.mongo.EcommerceTransactionDataProvider
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

    @Test
    fun `Should recover records from eCommerce DB and PM`() {
        val totalEcommerceCount = 5
        val totalPmCount = 5
        val pageSize = 4
        val pageNumber = 1
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByUserMail("test@test.it")
        val ecommerceResults =
            listOf(
                HelpdeskTestUtils.buildTransactionResultDto(
                    OffsetDateTime.now(),
                    ProductDto.ECOMMERCE
                )
            )
        val pmResults =
            listOf(HelpdeskTestUtils.buildTransactionResultDto(OffsetDateTime.now(), ProductDto.PM))
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
            .willReturn(Mono.just(ecommerceResults))
        given(
                pmTransactionDataProvider.findResult(
                    searchParams = eq(searchCriteria),
                    limit = any(),
                    skip = any()
                )
            )
            .willReturn(Mono.just(pmResults))

        StepVerifier.create(
                helpdeskService.searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    searchTransactionRequestDto = searchCriteria
                )
            )
            .expectNext(
                SearchTransactionResponseDto()
                    .transactions(ecommerceResults + pmResults)
                    .page(PageInfoDto().current(1).total(3).results(2))
            )
            .verifyComplete()
        verify(pmTransactionDataProvider, times(1)).totalRecordCount(searchCriteria)
        verify(ecommerceTransactionDataProvider, times(1)).totalRecordCount(searchCriteria)
        verify(ecommerceTransactionDataProvider, times(1))
            .findResult(skip = 4, limit = 1, searchParams = searchCriteria)
        verify(pmTransactionDataProvider, times(1))
            .findResult(skip = 0, limit = 3, searchParams = searchCriteria)
    }

    @Test
    fun `Should recover records from PM DB only`() {
        val totalEcommerceCount = 5
        val totalPmCount = 5
        val pageSize = 4
        val pageNumber = 2
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByUserMail("test@test.it")
        val pmResults =
            listOf(HelpdeskTestUtils.buildTransactionResultDto(OffsetDateTime.now(), ProductDto.PM))
        given(ecommerceTransactionDataProvider.totalRecordCount(searchCriteria))
            .willReturn(Mono.just(totalEcommerceCount))
        given(pmTransactionDataProvider.totalRecordCount(searchCriteria))
            .willReturn(Mono.just(totalPmCount))

        given(
                pmTransactionDataProvider.findResult(
                    searchParams = eq(searchCriteria),
                    limit = any(),
                    skip = any()
                )
            )
            .willReturn(Mono.just(pmResults))

        StepVerifier.create(
                helpdeskService.searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    searchTransactionRequestDto = searchCriteria
                )
            )
            .expectNext(
                SearchTransactionResponseDto()
                    .transactions(pmResults)
                    .page(PageInfoDto().current(2).total(3).results(1))
            )
            .verifyComplete()
        verify(pmTransactionDataProvider, times(1)).totalRecordCount(searchCriteria)
        verify(ecommerceTransactionDataProvider, times(1)).totalRecordCount(searchCriteria)
        verify(ecommerceTransactionDataProvider, times(0))
            .findResult(skip = any(), limit = any(), searchParams = any())
        verify(pmTransactionDataProvider, times(1))
            .findResult(skip = 3, limit = 4, searchParams = searchCriteria)
    }

    @Test
    fun `Should return empty list for page after last one`() {
        val totalEcommerceCount = 5
        val totalPmCount = 5
        val pageSize = 4
        val pageNumber = 3
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByUserMail("test@test.it")
        given(ecommerceTransactionDataProvider.totalRecordCount(searchCriteria))
            .willReturn(Mono.just(totalEcommerceCount))
        given(pmTransactionDataProvider.totalRecordCount(searchCriteria))
            .willReturn(Mono.just(totalPmCount))

        given(
                pmTransactionDataProvider.findResult(
                    searchParams = eq(searchCriteria),
                    limit = any(),
                    skip = any()
                )
            )
            .willReturn(Mono.just(emptyList()))

        StepVerifier.create(
                helpdeskService.searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    searchTransactionRequestDto = searchCriteria
                )
            )
            .expectNext(
                SearchTransactionResponseDto()
                    .transactions(emptyList())
                    .page(PageInfoDto().current(3).total(3).results(0))
            )
            .verifyComplete()
        verify(pmTransactionDataProvider, times(1)).totalRecordCount(searchCriteria)
        verify(ecommerceTransactionDataProvider, times(1)).totalRecordCount(searchCriteria)
        verify(ecommerceTransactionDataProvider, times(0))
            .findResult(skip = any(), limit = any(), searchParams = any())
        verify(pmTransactionDataProvider, times(1))
            .findResult(skip = 7, limit = 4, searchParams = searchCriteria)
    }

    @Test
    fun `Should return error for no record found for criteria`() {
        val totalEcommerceCount = 0
        val totalPmCount = 0
        val pageSize = 4
        val pageNumber = 0
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByUserMail("test@test.it")
        given(ecommerceTransactionDataProvider.totalRecordCount(searchCriteria))
            .willReturn(Mono.just(totalEcommerceCount))
        given(pmTransactionDataProvider.totalRecordCount(searchCriteria))
            .willReturn(Mono.just(totalPmCount))

        StepVerifier.create(
                helpdeskService.searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    searchTransactionRequestDto = searchCriteria
                )
            )
            .expectError(NoResultFoundException::class.java)
            .verify()
        verify(pmTransactionDataProvider, times(1)).totalRecordCount(searchCriteria)
        verify(ecommerceTransactionDataProvider, times(1)).totalRecordCount(searchCriteria)
        verify(ecommerceTransactionDataProvider, times(0))
            .findResult(skip = any(), limit = any(), searchParams = any())
        verify(pmTransactionDataProvider, times(0))
            .findResult(skip = any(), limit = any(), searchParams = any())
    }
}
