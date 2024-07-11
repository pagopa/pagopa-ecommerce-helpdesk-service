package it.pagopa.ecommerce.helpdesk.services.v2

import it.pagopa.ecommerce.commons.documents.BaseTransactionEvent
import it.pagopa.ecommerce.commons.documents.BaseTransactionView
import it.pagopa.ecommerce.commons.domain.Confidential
import it.pagopa.ecommerce.commons.domain.Email
import it.pagopa.ecommerce.commons.generated.server.model.TransactionStatusDto
import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import it.pagopa.ecommerce.commons.v1.TransactionTestUtils
import it.pagopa.ecommerce.helpdesk.HelpdeskTestUtilsV2
import it.pagopa.ecommerce.helpdesk.dataproviders.TransactionsEventStoreRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.TransactionsViewRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.v2.mongo.EcommerceTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.generated.ecommerce.helpdesk.v2.model.PageInfoDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.ProductDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchTransactionRequestEmailDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchTransactionRequestFiscalCodeDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchTransactionResponseDto
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Hooks
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class HelpdeskServiceTest {

    private val ecommerceTransactionDataProvider: EcommerceTransactionDataProvider = mock()

    private val confidentialDataManager: ConfidentialDataManager = mock()

    private val helpdeskService =
        HelpdeskService(
            ecommerceTransactionDataProvider = ecommerceTransactionDataProvider,
            confidentialDataManager = confidentialDataManager
        )

    private val testEmail = "test@test.it"
    private val testFiscalCode = "default-fiscalcode"

    private val encryptedEmail = TransactionTestUtils.EMAIL.opaqueData
    private val transactionsViewRepository: TransactionsViewRepository = mock()
    private val transactionsEventStoreRepository: TransactionsEventStoreRepository<Any> = mock()

    @Test
    fun `Should recover records from eCommerce DB`() {
        val totalEcommerceCount = 5
        val pageSize = 4
        val pageNumber = 0
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByUserMail(testEmail)

        val results =
            listOf(
                HelpdeskTestUtilsV2.buildTransactionResultDto(
                    OffsetDateTime.now(),
                    ProductDto.ECOMMERCE
                )
            )
        given(
                ecommerceTransactionDataProvider.totalRecordCount(
                    argThat {
                        (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                            testEmail
                    }
                )
            )
            .willReturn(Mono.just(totalEcommerceCount))
        given(
                ecommerceTransactionDataProvider.findResult(
                    searchParams =
                        argThat {
                            (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                                testEmail
                        },
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
                    .page(PageInfoDto().current(0).total(2).results(1))
            )
            .verifyComplete()
        verify(ecommerceTransactionDataProvider, times(1))
            .totalRecordCount(
                argThat {
                    (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                        testEmail
                }
            )
        verify(ecommerceTransactionDataProvider, times(1))
            .findResult(
                searchParams =
                    argThat {
                        (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                            testEmail
                    },
                skip = eq(0),
                limit = eq(4)
            )
    }

    @Test
    fun `Should recover records from eCommerce DB last page without remainder`() {
        val totalEcommerceCount = 8
        val pageSize = 4
        val pageNumber = 1
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByUserMail(testEmail)
        val ecommerceResults =
            listOf(
                HelpdeskTestUtilsV2.buildTransactionResultDto(
                    OffsetDateTime.now(),
                    ProductDto.ECOMMERCE
                )
            )
        given(
                ecommerceTransactionDataProvider.totalRecordCount(
                    argThat {
                        (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                            testEmail
                    }
                )
            )
            .willReturn(Mono.just(totalEcommerceCount))
        given(
                ecommerceTransactionDataProvider.findResult(
                    searchParams =
                        argThat {
                            (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                                testEmail
                        },
                    limit = any(),
                    skip = any()
                )
            )
            .willReturn(Mono.just(ecommerceResults))

        StepVerifier.create(
                helpdeskService.searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    searchTransactionRequestDto = searchCriteria
                )
            )
            .expectNext(
                SearchTransactionResponseDto()
                    .transactions(ecommerceResults)
                    .page(PageInfoDto().current(1).total(2).results(ecommerceResults.size))
            )
            .verifyComplete()
        verify(ecommerceTransactionDataProvider, times(1))
            .totalRecordCount(
                argThat {
                    (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                        testEmail
                }
            )
        verify(ecommerceTransactionDataProvider, times(1))
            .findResult(
                searchParams =
                    argThat {
                        (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                            testEmail
                    },
                skip = eq(4),
                limit = eq(4)
            )
    }

    @Test
    fun `Should return empty list for page after last one`() {
        val totalEcommerceCount = 5
        val pageSize = 4
        val pageNumber = 3
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByUserMail(testEmail)

        given(
                ecommerceTransactionDataProvider.totalRecordCount(
                    argThat {
                        (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                            testEmail
                    }
                )
            )
            .willReturn(Mono.just(totalEcommerceCount))

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
                    .page(PageInfoDto().current(3).total(2).results(0))
            )
            .verifyComplete()
        verify(ecommerceTransactionDataProvider, times(1))
            .totalRecordCount(
                argThat {
                    (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                        testEmail
                }
            )
        verify(ecommerceTransactionDataProvider, times(0))
            .findResult(skip = any(), limit = any(), searchParams = any())
    }

    @Test
    fun `Should return error for no record found for criteria`() {
        val totalEcommerceCount = 0
        val pageSize = 4
        val pageNumber = 0
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByUserMail(testEmail)

        given(
                ecommerceTransactionDataProvider.totalRecordCount(
                    argThat {
                        (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                            testEmail
                    }
                )
            )
            .willReturn(Mono.just(totalEcommerceCount))

        StepVerifier.create(
                helpdeskService.searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    searchTransactionRequestDto = searchCriteria
                )
            )
            .expectError(NoResultFoundException::class.java)
            .verify()
        verify(ecommerceTransactionDataProvider, times(1))
            .totalRecordCount(
                argThat {
                    (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                        testEmail
                }
            )
        verify(ecommerceTransactionDataProvider, times(0))
            .findResult(skip = any(), limit = any(), searchParams = any())
    }

    @Test
    fun `Should invoke PDV only once recovering records from eCommerce DB only`() {
        val totalEcommerceCount = 5
        val pageSize = 4
        val pageNumber = 0
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByUserMail(testEmail)
        val transactionDocument =
            TransactionTestUtils.transactionDocument(
                TransactionStatusDto.ACTIVATED,
                ZonedDateTime.now()
            ) as BaseTransactionView
        val helpDeskServiceLocalMock =
            HelpdeskService(
                ecommerceTransactionDataProvider =
                    EcommerceTransactionDataProvider(
                        transactionsViewRepository = transactionsViewRepository,
                        transactionsEventStoreRepository = transactionsEventStoreRepository
                    ),
                confidentialDataManager = confidentialDataManager
            )
        given(confidentialDataManager.encrypt(Email(testEmail)))
            .willReturn(Mono.just(Confidential(encryptedEmail)))
        given(transactionsViewRepository.countTransactionsWithEmail(encryptedEmail))
            .willReturn(Mono.just(totalEcommerceCount.toLong()))
        given(
                transactionsViewRepository
                    .findTransactionsWithEmailPaginatedOrderByCreationDateDesc(
                        encryptedEmail = any(),
                        skip = any(),
                        limit = any()
                    )
            )
            .willReturn(Flux.just(transactionDocument))
        given(transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(any()))
            .willReturn(
                Flux.just(
                    TransactionTestUtils.transactionActivateEvent() as BaseTransactionEvent<Any>
                )
            )
        Hooks.onOperatorDebug()
        StepVerifier.create(
                helpDeskServiceLocalMock.searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    searchTransactionRequestDto = searchCriteria
                )
            )
            .assertNext { assertEquals(it.page, PageInfoDto().current(0).total(2).results(1)) }
            .verifyComplete()
        verify(confidentialDataManager, times(1)).encrypt(any())
        verify(confidentialDataManager, times(0)).decrypt(any<Confidential<Email>>(), any())
    }

    @Test
    fun `Should invoke PDV only once recovering records from eCommerce DB and PM`() {
        val totalEcommerceCount = 5
        val pageSize = 4
        val pageNumber = 1
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByUserMail(testEmail)
        val transactionDocument =
            TransactionTestUtils.transactionDocument(
                TransactionStatusDto.ACTIVATED,
                ZonedDateTime.now()
            ) as BaseTransactionView
        val helpDeskServiceLocalMock =
            HelpdeskService(
                ecommerceTransactionDataProvider =
                    EcommerceTransactionDataProvider(
                        transactionsViewRepository = transactionsViewRepository,
                        transactionsEventStoreRepository = transactionsEventStoreRepository
                    ),
                confidentialDataManager = confidentialDataManager
            )
        given(confidentialDataManager.encrypt(Email(testEmail)))
            .willReturn(Mono.just(Confidential(encryptedEmail)))
        given(transactionsViewRepository.countTransactionsWithEmail(encryptedEmail))
            .willReturn(Mono.just(totalEcommerceCount.toLong()))
        given(
                transactionsViewRepository
                    .findTransactionsWithEmailPaginatedOrderByCreationDateDesc(
                        encryptedEmail = any(),
                        skip = any(),
                        limit = any()
                    )
            )
            .willReturn(Flux.just(transactionDocument))
        given(transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(any()))
            .willReturn(
                Flux.just(
                    TransactionTestUtils.transactionActivateEvent() as BaseTransactionEvent<Any>
                )
            )
        Hooks.onOperatorDebug()
        StepVerifier.create(
                helpDeskServiceLocalMock.searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    searchTransactionRequestDto = searchCriteria
                )
            )
            .assertNext { assertEquals(it.page, PageInfoDto().current(1).total(2).results(1)) }
            .verifyComplete()
        verify(confidentialDataManager, times(1)).encrypt(any())
        verify(confidentialDataManager, times(0)).decrypt(any<Confidential<Email>>(), any())
    }

    @Test
    fun `Should invoke PDV only once recovering records from eCommerce DB last page without remainder`() {
        val totalEcommerceCount = 8
        val pageSize = 4
        val pageNumber = 1
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByUserMail(testEmail)
        val transactionDocument =
            TransactionTestUtils.transactionDocument(
                TransactionStatusDto.ACTIVATED,
                ZonedDateTime.now()
            ) as BaseTransactionView
        val helpDeskServiceLocalMock =
            HelpdeskService(
                ecommerceTransactionDataProvider =
                    EcommerceTransactionDataProvider(
                        transactionsViewRepository = transactionsViewRepository,
                        transactionsEventStoreRepository = transactionsEventStoreRepository
                    ),
                confidentialDataManager = confidentialDataManager
            )
        given(confidentialDataManager.encrypt(Email(testEmail)))
            .willReturn(Mono.just(Confidential(encryptedEmail)))
        given(transactionsViewRepository.countTransactionsWithEmail(encryptedEmail))
            .willReturn(Mono.just(totalEcommerceCount.toLong()))
        given(
                transactionsViewRepository
                    .findTransactionsWithEmailPaginatedOrderByCreationDateDesc(
                        encryptedEmail = any(),
                        skip = any(),
                        limit = any()
                    )
            )
            .willReturn(Flux.just(transactionDocument))
        given(transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(any()))
            .willReturn(
                Flux.just(
                    TransactionTestUtils.transactionActivateEvent() as BaseTransactionEvent<Any>
                )
            )
        Hooks.onOperatorDebug()
        StepVerifier.create(
                helpDeskServiceLocalMock.searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    searchTransactionRequestDto = searchCriteria
                )
            )
            .assertNext { assertEquals(it.page, PageInfoDto().current(1).total(2).results(1)) }
            .verifyComplete()
        verify(confidentialDataManager, times(1)).encrypt(any())
        verify(confidentialDataManager, times(0)).decrypt(any<Confidential<Email>>(), any())
    }

    @Test
    fun `Should return error for wrong search type`() {
        val pageSize = 1
        val pageNumber = 0
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByUserFiscalCode(testFiscalCode)

        given(
                ecommerceTransactionDataProvider.totalRecordCount(
                    argThat {
                        (this.searchParameter as SearchTransactionRequestFiscalCodeDto)
                            .userFiscalCode == testFiscalCode
                    }
                )
            )
            .willReturn(Mono.error(InvalidSearchCriteriaException("USER_FISCAL_CODE")))

        StepVerifier.create(
                helpdeskService.searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    searchTransactionRequestDto = searchCriteria
                )
            )
            .expectError(NoResultFoundException::class.java)
            .verify()

        verify(ecommerceTransactionDataProvider, times(1))
            .totalRecordCount(
                argThat {
                    (this.searchParameter as SearchTransactionRequestFiscalCodeDto)
                        .userFiscalCode == testFiscalCode
                }
            )
        verify(ecommerceTransactionDataProvider, times(0))
            .findResult(skip = any(), limit = any(), searchParams = any())
    }

    @Test
    fun `Should return empty list for findResult error`() {
        val totalEcommerceCount = 5
        val pageSize = 1
        val pageNumber = 0
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByUserFiscalCode(testFiscalCode)

        given(
                ecommerceTransactionDataProvider.totalRecordCount(
                    argThat {
                        (this.searchParameter as SearchTransactionRequestFiscalCodeDto)
                            .userFiscalCode == testFiscalCode
                    }
                )
            )
            .willReturn(Mono.just(totalEcommerceCount))

        given(
                ecommerceTransactionDataProvider.findResult(
                    searchParams =
                        argThat {
                            (this.searchParameter as SearchTransactionRequestFiscalCodeDto)
                                .userFiscalCode == testFiscalCode
                        },
                    limit = any(),
                    skip = any()
                )
            )
            .willReturn(Mono.error(InvalidSearchCriteriaException("USER_FISCAL_CODE")))

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
                    .page(PageInfoDto().current(0).total(5).results(0))
            )
            .verifyComplete()

        verify(ecommerceTransactionDataProvider, times(1))
            .totalRecordCount(
                argThat {
                    (this.searchParameter as SearchTransactionRequestFiscalCodeDto)
                        .userFiscalCode == testFiscalCode
                }
            )
        verify(ecommerceTransactionDataProvider, times(1))
            .findResult(
                searchParams =
                    argThat {
                        (this.searchParameter as SearchTransactionRequestFiscalCodeDto)
                            .userFiscalCode == testFiscalCode
                    },
                skip = eq(0),
                limit = eq(1)
            )
    }
}
