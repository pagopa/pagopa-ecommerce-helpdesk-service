package it.pagopa.ecommerce.helpdesk.services.v2

import it.pagopa.ecommerce.commons.documents.BaseTransactionEvent
import it.pagopa.ecommerce.commons.documents.BaseTransactionView
import it.pagopa.ecommerce.commons.domain.Confidential
import it.pagopa.ecommerce.commons.domain.v2.Email
import it.pagopa.ecommerce.commons.generated.server.model.TransactionStatusDto
import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import it.pagopa.ecommerce.commons.v2.TransactionTestUtils
import it.pagopa.ecommerce.helpdesk.HelpdeskTestUtilsV2
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce.TransactionsEventStoreRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce.TransactionsViewRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.history.TransactionsEventStoreRepository as TransactionsEventStoreHistoryRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.history.TransactionsViewRepository as TransactionsViewHistoryRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.v2.mongo.EcommerceTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.v2.mongo.PmTransactionHistoryDataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.v2.oracle.PMTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.utils.PmProviderType
import it.pagopa.generated.ecommerce.helpdesk.v2.model.PageInfoDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.ProductDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchTransactionRequestEmailDto
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

    private val pmTransactionDataProvider: PMTransactionDataProvider = mock()

    private val ecommerceTransactionDataProvider: EcommerceTransactionDataProvider = mock()

    private val confidentialDataManager: ConfidentialDataManager = mock()

    private val pmEcommerceHistoryDataProvider: PmTransactionHistoryDataProvider = mock()

    private val helpdeskService =
        HelpdeskService(
            pmTransactionDataProvider = pmTransactionDataProvider,
            ecommerceTransactionDataProvider = ecommerceTransactionDataProvider,
            confidentialDataManagerEmail = confidentialDataManager,
            confidentialDataManagerFiscalCode = confidentialDataManager,
            pmEcommerceHistoryDataProvider = pmEcommerceHistoryDataProvider
        )

    private val testEmail = "test@test.it"

    private val encryptedEmail = TransactionTestUtils.EMAIL.opaqueData
    private val transactionsViewRepository: TransactionsViewRepository = mock()
    private val transactionsViewHistoryRepository: TransactionsViewHistoryRepository = mock()

    private val transactionsEventStoreRepository: TransactionsEventStoreRepository<Any> = mock()
    private val transactionsEventStoreHistoryRepository:
        TransactionsEventStoreHistoryRepository<Any> =
        mock()

    @Test
    fun `Should recover records from eCommerce DB only`() {
        val totalEcommerceCount = 5
        val totalPmCount = 5
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
                pmTransactionDataProvider.totalRecordCount(
                    argThat {
                        (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                            testEmail
                    }
                )
            )
            .willReturn(Mono.just(totalPmCount))
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
                    .page(PageInfoDto().current(0).total(3).results(1))
            )
            .verifyComplete()
        verify(pmTransactionDataProvider, times(1))
            .totalRecordCount(
                argThat {
                    (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                        testEmail
                }
            )
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
        verify(pmTransactionDataProvider, times(0))
            .findResult(skip = any(), limit = any(), searchParams = any())
    }

    @Test
    fun `Should recover records from eCommerce DB and PM`() {
        val totalEcommerceCount = 5
        val totalPmCount = 5
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
        val pmResults =
            listOf(
                HelpdeskTestUtilsV2.buildTransactionResultDto(OffsetDateTime.now(), ProductDto.PM)
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
                pmTransactionDataProvider.totalRecordCount(
                    argThat {
                        (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                            testEmail
                    }
                )
            )
            .willReturn(Mono.just(totalPmCount))
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
        given(
                pmTransactionDataProvider.findResult(
                    searchParams =
                        argThat {
                            (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                                testEmail
                        },
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
        verify(pmTransactionDataProvider, times(1))
            .totalRecordCount(
                argThat {
                    (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                        testEmail
                }
            )
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
                limit = eq(1)
            )
        verify(pmTransactionDataProvider, times(1))
            .findResult(
                searchParams =
                    argThat {
                        (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                            testEmail
                    },
                skip = eq(0),
                limit = eq(3)
            )
    }

    @Test
    fun `Should recover records from eCommerce DB last page without remainder`() {
        val totalEcommerceCount = 8
        val totalPmCount = 5
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
        val pmResults =
            listOf(
                HelpdeskTestUtilsV2.buildTransactionResultDto(OffsetDateTime.now(), ProductDto.PM)
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
                pmTransactionDataProvider.totalRecordCount(
                    argThat {
                        (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                            testEmail
                    }
                )
            )
            .willReturn(Mono.just(totalPmCount))
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
        given(
                pmTransactionDataProvider.findResult(
                    searchParams =
                        argThat {
                            (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                                testEmail
                        },
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
                    .transactions(ecommerceResults)
                    .page(PageInfoDto().current(1).total(4).results(ecommerceResults.size))
            )
            .verifyComplete()
        verify(pmTransactionDataProvider, times(1))
            .totalRecordCount(
                argThat {
                    (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                        testEmail
                }
            )
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
        verify(pmTransactionDataProvider, times(0))
            .findResult(skip = any(), limit = any(), searchParams = any())
    }

    @Test
    fun `Should recover records from PM DB only`() {
        val totalEcommerceCount = 5
        val totalPmCount = 5
        val pageSize = 4
        val pageNumber = 2
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByUserMail(testEmail)
        val pmResults =
            listOf(
                HelpdeskTestUtilsV2.buildTransactionResultDto(OffsetDateTime.now(), ProductDto.PM)
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
                pmTransactionDataProvider.totalRecordCount(
                    argThat {
                        (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                            testEmail
                    }
                )
            )
            .willReturn(Mono.just(totalPmCount))

        given(
                pmTransactionDataProvider.findResult(
                    searchParams =
                        argThat {
                            (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                                testEmail
                        },
                    limit = any(),
                    skip = any()
                )
            )
            .willReturn(Mono.just(pmResults))
        Hooks.onOperatorDebug()
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
        verify(pmTransactionDataProvider, times(1))
            .totalRecordCount(
                argThat {
                    (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                        testEmail
                }
            )
        verify(ecommerceTransactionDataProvider, times(1))
            .totalRecordCount(
                argThat {
                    (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                        testEmail
                }
            )
        verify(ecommerceTransactionDataProvider, times(0))
            .findResult(skip = any(), limit = any(), searchParams = any())
        verify(pmTransactionDataProvider, times(1))
            .findResult(
                searchParams =
                    argThat {
                        (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                            testEmail
                    },
                skip = eq(3),
                limit = eq(4)
            )
    }

    @Test
    fun `Should return empty list for page after last one`() {
        val totalEcommerceCount = 5
        val totalPmCount = 5
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
        given(
                pmTransactionDataProvider.totalRecordCount(
                    argThat {
                        (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                            testEmail
                    }
                )
            )
            .willReturn(Mono.just(totalPmCount))

        given(
                pmTransactionDataProvider.findResult(
                    searchParams =
                        argThat {
                            (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                                testEmail
                        },
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
        verify(pmTransactionDataProvider, times(1))
            .totalRecordCount(
                argThat {
                    (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                        testEmail
                }
            )
        verify(ecommerceTransactionDataProvider, times(1))
            .totalRecordCount(
                argThat {
                    (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                        testEmail
                }
            )
        verify(ecommerceTransactionDataProvider, times(0))
            .findResult(skip = any(), limit = any(), searchParams = any())
        verify(pmTransactionDataProvider, times(1))
            .findResult(
                searchParams =
                    argThat {
                        (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                            testEmail
                    },
                skip = eq(7),
                limit = eq(4),
            )
    }

    @Test
    fun `Should return error for no record found for criteria`() {
        val totalEcommerceCount = 0
        val totalPmCount = 0
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
        given(
                pmTransactionDataProvider.totalRecordCount(
                    argThat {
                        (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                            testEmail
                    }
                )
            )
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
        verify(pmTransactionDataProvider, times(1))
            .totalRecordCount(
                argThat {
                    (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                        testEmail
                }
            )
        verify(ecommerceTransactionDataProvider, times(1))
            .totalRecordCount(
                argThat {
                    (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                        testEmail
                }
            )
        verify(ecommerceTransactionDataProvider, times(0))
            .findResult(skip = any(), limit = any(), searchParams = any())
        verify(pmTransactionDataProvider, times(0))
            .findResult(skip = any(), limit = any(), searchParams = any())
    }

    @Test
    fun `Should invoke PDV only once recovering records from eCommerce DB only`() {
        val totalEcommerceCount = 5
        val totalPmCount = 5
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
                pmTransactionDataProvider = pmTransactionDataProvider,
                ecommerceTransactionDataProvider =
                    EcommerceTransactionDataProvider(
                        transactionsViewRepository = transactionsViewRepository,
                        transactionsViewHistoryRepository = transactionsViewHistoryRepository,
                        transactionsEventStoreRepository = transactionsEventStoreRepository,
                        transactionsEventStoreHistoryRepository =
                            transactionsEventStoreHistoryRepository,
                    ),
                confidentialDataManagerEmail = confidentialDataManager,
                confidentialDataManagerFiscalCode = confidentialDataManager,
                pmEcommerceHistoryDataProvider = pmEcommerceHistoryDataProvider
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
        given(
                pmTransactionDataProvider.totalRecordCount(
                    argThat {
                        (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                            testEmail
                    }
                )
            )
            .willReturn(Mono.just(totalPmCount))
        Hooks.onOperatorDebug()
        StepVerifier.create(
                helpDeskServiceLocalMock.searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    searchTransactionRequestDto = searchCriteria
                )
            )
            .assertNext { assertEquals(it.page, PageInfoDto().current(0).total(3).results(1)) }
            .verifyComplete()
        verify(confidentialDataManager, times(1)).encrypt(any())
        verify(confidentialDataManager, times(0)).decrypt(any<Confidential<Email>>(), any())
    }

    @Test
    fun `Should invoke PDV only once recovering records from eCommerce DB and PM`() {
        val totalEcommerceCount = 5
        val totalPmCount = 5
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
                pmTransactionDataProvider = pmTransactionDataProvider,
                ecommerceTransactionDataProvider =
                    EcommerceTransactionDataProvider(
                        transactionsViewRepository = transactionsViewRepository,
                        transactionsViewHistoryRepository = transactionsViewHistoryRepository,
                        transactionsEventStoreRepository = transactionsEventStoreRepository,
                        transactionsEventStoreHistoryRepository =
                            transactionsEventStoreHistoryRepository,
                    ),
                confidentialDataManagerEmail = confidentialDataManager,
                confidentialDataManagerFiscalCode = confidentialDataManager,
                pmEcommerceHistoryDataProvider = pmEcommerceHistoryDataProvider
            )
        val pmResults =
            listOf(
                HelpdeskTestUtilsV2.buildTransactionResultDto(OffsetDateTime.now(), ProductDto.PM)
            )
        given(
                pmTransactionDataProvider.totalRecordCount(
                    argThat {
                        (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                            testEmail
                    }
                )
            )
            .willReturn(Mono.just(totalPmCount))
        given(
                pmTransactionDataProvider.findResult(
                    searchParams =
                        argThat {
                            (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                                testEmail
                        },
                    limit = any(),
                    skip = any()
                )
            )
            .willReturn(Mono.just(pmResults))
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
        given(
                pmTransactionDataProvider.totalRecordCount(
                    argThat {
                        (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                            testEmail
                    }
                )
            )
            .willReturn(Mono.just(totalPmCount))
        Hooks.onOperatorDebug()
        StepVerifier.create(
                helpDeskServiceLocalMock.searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    searchTransactionRequestDto = searchCriteria
                )
            )
            .assertNext { assertEquals(it.page, PageInfoDto().current(1).total(3).results(2)) }
            .verifyComplete()
        verify(confidentialDataManager, times(1)).encrypt(any())
        verify(confidentialDataManager, times(0)).decrypt(any<Confidential<Email>>(), any())
    }

    @Test
    fun `Should invoke PDV only once recovering records from eCommerce DB last page without remainder`() {
        val totalEcommerceCount = 8
        val totalPmCount = 5
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
                pmTransactionDataProvider = pmTransactionDataProvider,
                ecommerceTransactionDataProvider =
                    EcommerceTransactionDataProvider(
                        transactionsViewRepository = transactionsViewRepository,
                        transactionsViewHistoryRepository = transactionsViewHistoryRepository,
                        transactionsEventStoreRepository = transactionsEventStoreRepository,
                        transactionsEventStoreHistoryRepository =
                            transactionsEventStoreHistoryRepository,
                    ),
                confidentialDataManagerEmail = confidentialDataManager,
                confidentialDataManagerFiscalCode = confidentialDataManager,
                pmEcommerceHistoryDataProvider = pmEcommerceHistoryDataProvider
            )
        val pmResults =
            listOf(
                HelpdeskTestUtilsV2.buildTransactionResultDto(OffsetDateTime.now(), ProductDto.PM)
            )
        given(
                pmTransactionDataProvider.totalRecordCount(
                    argThat {
                        (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                            testEmail
                    }
                )
            )
            .willReturn(Mono.just(totalPmCount))
        given(
                pmTransactionDataProvider.findResult(
                    searchParams =
                        argThat {
                            (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                                testEmail
                        },
                    limit = any(),
                    skip = any()
                )
            )
            .willReturn(Mono.just(pmResults))
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
        given(
                pmTransactionDataProvider.totalRecordCount(
                    argThat {
                        (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                            testEmail
                    }
                )
            )
            .willReturn(Mono.just(totalPmCount))
        Hooks.onOperatorDebug()
        StepVerifier.create(
                helpDeskServiceLocalMock.searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    searchTransactionRequestDto = searchCriteria
                )
            )
            .assertNext { assertEquals(it.page, PageInfoDto().current(1).total(4).results(1)) }
            .verifyComplete()
        verify(confidentialDataManager, times(1)).encrypt(any())
        verify(confidentialDataManager, times(0)).decrypt(any<Confidential<Email>>(), any())
    }

    @Test
    fun `Should recover records from eCommerce DB and ECOMMERCE_HISTORY`() {
        val totalEcommerceCount = 5
        val totalPmCount = 5
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
        val pmHistoryResults =
            listOf(
                HelpdeskTestUtilsV2.buildTransactionResultDto(OffsetDateTime.now(), ProductDto.PM)
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
                pmEcommerceHistoryDataProvider.totalRecordCount(
                    argThat {
                        (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                            testEmail
                    }
                )
            )
            .willReturn(Mono.just(totalPmCount))
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
        given(
                pmEcommerceHistoryDataProvider.findResult(
                    searchParams =
                        argThat {
                            (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                                testEmail
                        },
                    limit = any(),
                    skip = any()
                )
            )
            .willReturn(Mono.just(pmHistoryResults))

        StepVerifier.create(
                helpdeskService.searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    searchTransactionRequestDto = searchCriteria,
                    pmProviderType = PmProviderType.ECOMMERCE_HISTORY
                )
            )
            .expectNext(
                SearchTransactionResponseDto()
                    .transactions(ecommerceResults + pmHistoryResults)
                    .page(PageInfoDto().current(1).total(3).results(2))
            )
            .verifyComplete()
        verify(pmEcommerceHistoryDataProvider, times(1))
            .totalRecordCount(
                argThat {
                    (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                        testEmail
                }
            )
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
                limit = eq(1)
            )
        verify(pmEcommerceHistoryDataProvider, times(1))
            .findResult(
                searchParams =
                    argThat {
                        (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                            testEmail
                    },
                skip = eq(0),
                limit = eq(3)
            )
    }

    @Test
    fun `Should recover records from PM ECOMMERCE_HISTORY only`() {
        val totalEcommerceCount = 5
        val totalPmCount = 5
        val pageSize = 4
        val pageNumber = 2
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByUserMail(testEmail)
        val pmHistoryResults =
            listOf(
                HelpdeskTestUtilsV2.buildTransactionResultDto(OffsetDateTime.now(), ProductDto.PM)
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
                pmEcommerceHistoryDataProvider.totalRecordCount(
                    argThat {
                        (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                            testEmail
                    }
                )
            )
            .willReturn(Mono.just(totalPmCount))

        given(
                pmEcommerceHistoryDataProvider.findResult(
                    searchParams =
                        argThat {
                            (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                                testEmail
                        },
                    limit = any(),
                    skip = any()
                )
            )
            .willReturn(Mono.just(pmHistoryResults))

        StepVerifier.create(
                helpdeskService.searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    searchTransactionRequestDto = searchCriteria,
                    pmProviderType = PmProviderType.ECOMMERCE_HISTORY
                )
            )
            .expectNext(
                SearchTransactionResponseDto()
                    .transactions(pmHistoryResults)
                    .page(PageInfoDto().current(2).total(3).results(1))
            )
            .verifyComplete()
        verify(pmEcommerceHistoryDataProvider, times(1))
            .totalRecordCount(
                argThat {
                    (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                        testEmail
                }
            )
        verify(ecommerceTransactionDataProvider, times(1))
            .totalRecordCount(
                argThat {
                    (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                        testEmail
                }
            )
        verify(ecommerceTransactionDataProvider, times(0))
            .findResult(skip = any(), limit = any(), searchParams = any())
        verify(pmEcommerceHistoryDataProvider, times(1))
            .findResult(
                searchParams =
                    argThat {
                        (this.searchParameter as SearchTransactionRequestEmailDto).userEmail ==
                            testEmail
                    },
                skip = eq(3),
                limit = eq(4)
            )
    }
}
