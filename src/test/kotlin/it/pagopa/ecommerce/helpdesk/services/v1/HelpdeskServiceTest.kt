package it.pagopa.ecommerce.helpdesk.services.v1

import io.vavr.control.Either
import it.pagopa.ecommerce.commons.client.NpgClient
import it.pagopa.ecommerce.commons.documents.BaseTransactionEvent
import it.pagopa.ecommerce.commons.documents.BaseTransactionView
import it.pagopa.ecommerce.commons.documents.v2.*
import it.pagopa.ecommerce.commons.domain.Confidential
import it.pagopa.ecommerce.commons.domain.Email
import it.pagopa.ecommerce.commons.domain.v2.TransactionActivated
import it.pagopa.ecommerce.commons.domain.v2.pojos.BaseTransactionWithRequestedAuthorization
import it.pagopa.ecommerce.commons.exceptions.NpgResponseException
import it.pagopa.ecommerce.commons.generated.npg.v1.dto.OperationDto
import it.pagopa.ecommerce.commons.generated.npg.v1.dto.OperationTypeDto
import it.pagopa.ecommerce.commons.generated.npg.v1.dto.OrderResponseDto
import it.pagopa.ecommerce.commons.generated.server.model.TransactionStatusDto
import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import it.pagopa.ecommerce.commons.utils.NpgApiKeyConfiguration
import it.pagopa.ecommerce.commons.v1.TransactionTestUtils
import it.pagopa.ecommerce.commons.v2.TransactionTestUtils.*
import it.pagopa.ecommerce.helpdesk.HelpdeskTestUtils
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce.TransactionsEventStoreRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce.TransactionsViewRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.mongo.EcommerceTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.oracle.PMTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.exceptions.NpgBadGatewayException
import it.pagopa.ecommerce.helpdesk.exceptions.NpgBadRequestException
import it.pagopa.ecommerce.helpdesk.utils.TransactionInfoUtils
import it.pagopa.ecommerce.helpdesk.utils.TransactionInfoUtils.Companion.buildOrderResponseDtoNullOperation
import it.pagopa.generated.ecommerce.helpdesk.model.*
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.http.HttpStatus
import reactor.core.publisher.Flux
import reactor.core.publisher.Hooks
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class HelpdeskServiceTest {

    private val pmTransactionDataProvider: PMTransactionDataProvider = mock()
    private val ecommerceTransactionDataProvider: EcommerceTransactionDataProvider = mock()
    private val confidentialDataManager: ConfidentialDataManager = mock()
    private val npgApiKeyConfiguration: NpgApiKeyConfiguration = mock()
    private val npgClient: NpgClient = mock()

    private val helpdeskService =
        HelpdeskService(
            pmTransactionDataProvider = pmTransactionDataProvider,
            ecommerceTransactionDataProvider = ecommerceTransactionDataProvider,
            confidentialDataManager = confidentialDataManager,
            npgClient = npgClient,
            npgApiKeyConfiguration = npgApiKeyConfiguration
        )

    private val testEmail = "test@test.it"
    private val encryptedEmail = TransactionTestUtils.EMAIL.opaqueData
    private val transactionsViewRepository: TransactionsViewRepository = mock()
    private val transactionsEventStoreRepository: TransactionsEventStoreRepository<Any> = mock()

    @Test
    fun `Should recover records from eCommerce DB only`() {
        val totalEcommerceCount = 5
        val totalPmCount = 5
        val pageSize = 4
        val pageNumber = 0
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByUserMail(testEmail)

        val results =
            listOf(
                HelpdeskTestUtils.buildTransactionResultDto(
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
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByUserMail(testEmail)
        val ecommerceResults =
            listOf(
                HelpdeskTestUtils.buildTransactionResultDto(
                    OffsetDateTime.now(),
                    ProductDto.ECOMMERCE
                )
            )
        val pmResults =
            listOf(HelpdeskTestUtils.buildTransactionResultDto(OffsetDateTime.now(), ProductDto.PM))
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
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByUserMail(testEmail)
        val ecommerceResults =
            listOf(
                HelpdeskTestUtils.buildTransactionResultDto(
                    OffsetDateTime.now(),
                    ProductDto.ECOMMERCE
                )
            )
        val pmResults =
            listOf(HelpdeskTestUtils.buildTransactionResultDto(OffsetDateTime.now(), ProductDto.PM))
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
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByUserMail(testEmail)
        val pmResults =
            listOf(HelpdeskTestUtils.buildTransactionResultDto(OffsetDateTime.now(), ProductDto.PM))
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
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByUserMail(testEmail)

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
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByUserMail(testEmail)

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
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByUserMail(testEmail)
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
                        transactionsEventStoreRepository = transactionsEventStoreRepository
                    ),
                confidentialDataManager = confidentialDataManager,
                npgClient = npgClient,
                npgApiKeyConfiguration = npgApiKeyConfiguration
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
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByUserMail(testEmail)
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
                        transactionsEventStoreRepository = transactionsEventStoreRepository
                    ),
                confidentialDataManager = confidentialDataManager,
                npgClient = npgClient,
                npgApiKeyConfiguration = npgApiKeyConfiguration
            )
        val pmResults =
            listOf(HelpdeskTestUtils.buildTransactionResultDto(OffsetDateTime.now(), ProductDto.PM))
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
        val searchCriteria = HelpdeskTestUtils.buildSearchRequestByUserMail(testEmail)
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
                        transactionsEventStoreRepository = transactionsEventStoreRepository
                    ),
                confidentialDataManager = confidentialDataManager,
                npgClient = npgClient,
                npgApiKeyConfiguration = npgApiKeyConfiguration
            )
        val pmResults =
            listOf(HelpdeskTestUtils.buildTransactionResultDto(OffsetDateTime.now(), ProductDto.PM))
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

    companion object {
        private val correlationId = UUID.randomUUID().toString()
        private const val OPERATION_ID = "operationId"
    }

    @Test
    fun `Should successfully retrieve NPG operations`() {
        val orderId = AUTHORIZATION_REQUEST_ID
        val pspId = PSP_ID
        val paymentMethod = NpgClient.PaymentMethod.CARDS
        val transactionId = TRANSACTION_ID
        val tuple4 =
            NTuple4(AUTHORIZATION_REQUEST_ID, PSP_ID, correlationId, NpgClient.PaymentMethod.CARDS)

        val transactionEvent = mock<TransactionActivatedEvent>()
        val transaction = mock<TransactionActivated>()
        val transactionData = mock<TransactionActivatedData>()

        val events =
            TransactionInfoUtils.buildSimpleEventsList(
                correlationId,
                TransactionAuthorizationRequestData.PaymentGateway.NPG
            )

        given(ecommerceTransactionDataProvider.retrieveTransactionDetails(transactionId))
            .willReturn(Mono.just(tuple4))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    TRANSACTION_ID
                )
            )
            .willReturn(Flux.fromIterable(events))

        given(transactionEvent.data).willReturn(transactionData)
        given(transaction.transactionActivatedData).willReturn(transactionData)

        val npgOperation =
            OperationDto().apply {
                operationId = OPERATION_ID
                operationType = OperationTypeDto.AUTHORIZATION
                operationResult =
                    it.pagopa.ecommerce.commons.generated.npg.v1.dto.OperationResultDto.EXECUTED
                additionalData = mapOf("authorizationCode" to "auth123", "rrn" to "rrn123")
            }
        val orderResponse = OrderResponseDto().apply { operations = listOf(npgOperation) }

        given(npgApiKeyConfiguration[paymentMethod, pspId]).willReturn(Either.right("apiKey"))
        given(npgClient.getOrder(any(), eq("apiKey"), eq(orderId)))
            .willReturn(Mono.just(orderResponse))

        StepVerifier.create(helpdeskService.searchNpgOperations(TRANSACTION_ID))
            .expectNextMatches { response ->
                response.operations?.size == 1 &&
                    response.operations?.first()?.operationResult == OperationResultDto.EXECUTED
            }
            .verifyComplete()
    }

    @Test
    fun `Should handle missing operation data`() {
        val transactionId = TRANSACTION_ID
        val tuple4 =
            NTuple4(AUTHORIZATION_REQUEST_ID, PSP_ID, correlationId, NpgClient.PaymentMethod.CARDS)
        val transactionEvent =
            mock<BaseTransactionEvent<BaseTransactionWithRequestedAuthorization>>()
        val baseTransaction = mock<BaseTransactionWithRequestedAuthorization>()
        val authRequestData = mock<TransactionAuthorizationRequestData>()

        val events =
            TransactionInfoUtils.buildSimpleEventsList(
                correlationId,
                TransactionAuthorizationRequestData.PaymentGateway.NPG
            )

        given(ecommerceTransactionDataProvider.retrieveTransactionDetails(transactionId))
            .willReturn(Mono.just(tuple4))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    TRANSACTION_ID
                )
            )
            .willReturn(Flux.fromIterable(events))

        given(transactionEvent.data).willReturn(baseTransaction)
        given(baseTransaction.transactionAuthorizationRequestData).willReturn(authRequestData)
        given(authRequestData.paymentMethodName).willReturn("CARDS")
        given(authRequestData.pspId).willReturn("pspId")
        given(npgApiKeyConfiguration[any<NpgClient.PaymentMethod>(), any()])
            .willReturn(Either.right("apiKey"))
        given(npgClient.getOrder(any(), any(), any()))
            .willReturn(buildOrderResponseDtoNullOperation())

        StepVerifier.create(helpdeskService.searchNpgOperations(TRANSACTION_ID))
            .expectNext(SearchNpgOperationsResponseDto().apply { operations = null })
            .verifyComplete()
    }

    @Test
    fun `Should handle NPG server error`() {
        val transactionId = TRANSACTION_ID
        val tuple4 =
            NTuple4(AUTHORIZATION_REQUEST_ID, PSP_ID, correlationId, NpgClient.PaymentMethod.CARDS)
        val events =
            TransactionInfoUtils.buildSimpleEventsList(
                correlationId,
                TransactionAuthorizationRequestData.PaymentGateway.NPG
            )

        given(ecommerceTransactionDataProvider.retrieveTransactionDetails(transactionId))
            .willReturn(Mono.just(tuple4))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    TRANSACTION_ID
                )
            )
            .willReturn(Flux.fromIterable(events))
        given(npgApiKeyConfiguration[any<NpgClient.PaymentMethod>(), any()])
            .willReturn(Either.right("apiKey"))
        given(npgClient.getOrder(any(), any(), any()))
            .willReturn(
                Mono.error(
                    NpgResponseException(
                        "error",
                        emptyList(),
                        Optional.of(HttpStatus.INTERNAL_SERVER_ERROR),
                        RuntimeException()
                    )
                )
            )

        StepVerifier.create(helpdeskService.searchNpgOperations(TRANSACTION_ID))
            .expectError(NpgBadGatewayException::class.java)
            .verify()
    }

    @Test
    fun `Should handle NPG client error`() {
        val transactionId = TRANSACTION_ID
        val tuple4 =
            NTuple4(AUTHORIZATION_REQUEST_ID, PSP_ID, correlationId, NpgClient.PaymentMethod.CARDS)
        val events =
            TransactionInfoUtils.buildSimpleEventsList(
                correlationId,
                TransactionAuthorizationRequestData.PaymentGateway.NPG
            )

        given(ecommerceTransactionDataProvider.retrieveTransactionDetails(transactionId))
            .willReturn(Mono.just(tuple4))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    TRANSACTION_ID
                )
            )
            .willReturn(Flux.fromIterable(events))
        given(npgApiKeyConfiguration[any<NpgClient.PaymentMethod>(), any()])
            .willReturn(Either.right("apiKey"))
        given(npgClient.getOrder(any(), any(), any()))
            .willReturn(
                Mono.error(
                    NpgResponseException(
                        "error",
                        emptyList(),
                        Optional.of(HttpStatus.BAD_REQUEST),
                        RuntimeException()
                    )
                )
            )

        StepVerifier.create(helpdeskService.searchNpgOperations(transactionId))
            .expectError(NpgBadRequestException::class.java)
            .verify()
    }

    @Test
    fun `Should handle NPG operations with no operations in order response`() {
        val transactionId = TRANSACTION_ID
        val orderId = AUTHORIZATION_REQUEST_ID
        val pspId = PSP_ID
        val paymentMethod = NpgClient.PaymentMethod.CARDS
        val tuple4 =
            NTuple4(AUTHORIZATION_REQUEST_ID, PSP_ID, correlationId, NpgClient.PaymentMethod.CARDS)
        val events =
            TransactionInfoUtils.buildSimpleEventsList(
                correlationId,
                TransactionAuthorizationRequestData.PaymentGateway.NPG
            )

        val orderResponse = OrderResponseDto().apply { operations = null }

        given(ecommerceTransactionDataProvider.retrieveTransactionDetails(transactionId))
            .willReturn(Mono.just(tuple4))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    TRANSACTION_ID
                )
            )
            .willReturn(Flux.fromIterable(events))
        given(npgApiKeyConfiguration[paymentMethod, pspId]).willReturn(Either.right("apiKey"))
        given(npgClient.getOrder(any(), eq("apiKey"), eq(orderId)))
            .willReturn(Mono.just(orderResponse))

        StepVerifier.create(helpdeskService.searchNpgOperations(TRANSACTION_ID))
            .expectNext(SearchNpgOperationsResponseDto().apply { operations = null })
            .verifyComplete()
    }

    @Test
    fun `Should handle NPG operations with no RRN in additional data`() {
        val orderId = AUTHORIZATION_REQUEST_ID
        val pspId = PSP_ID
        val paymentMethod = NpgClient.PaymentMethod.CARDS
        val transactionId = TRANSACTION_ID
        val tuple4 =
            NTuple4(AUTHORIZATION_REQUEST_ID, PSP_ID, correlationId, NpgClient.PaymentMethod.CARDS)
        val events =
            TransactionInfoUtils.buildSimpleEventsList(
                correlationId,
                TransactionAuthorizationRequestData.PaymentGateway.NPG
            )

        val npgOperation =
            OperationDto().apply {
                operationId = OPERATION_ID
                operationType = OperationTypeDto.AUTHORIZATION
                operationResult =
                    it.pagopa.ecommerce.commons.generated.npg.v1.dto.OperationResultDto.EXECUTED
                additionalData = mapOf("authorizationCode" to AUTHORIZATION_CODE)
            }

        val orderResponse = OrderResponseDto().apply { operations = listOf(npgOperation) }

        given(ecommerceTransactionDataProvider.retrieveTransactionDetails(transactionId))
            .willReturn(Mono.just(tuple4))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    TRANSACTION_ID
                )
            )
            .willReturn(Flux.fromIterable(events))
        given(npgApiKeyConfiguration[paymentMethod, pspId]).willReturn(Either.right("apiKey"))
        given(npgClient.getOrder(any(), eq("apiKey"), eq(orderId)))
            .willReturn(Mono.just(orderResponse))

        StepVerifier.create(helpdeskService.searchNpgOperations(TRANSACTION_ID))
            .expectNextMatches { response ->
                response.operations?.size == 1 &&
                    response.operations?.first()?.additionalData?.rrn == null
            }
            .verifyComplete()
    }

    @Test
    fun `Should handle NPG operations with no payment method`() {
        val orderId = AUTHORIZATION_REQUEST_ID
        val pspId = PSP_ID
        val paymentMethod = NpgClient.PaymentMethod.CARDS
        val transactionId = TRANSACTION_ID
        val tuple4 =
            NTuple4(AUTHORIZATION_REQUEST_ID, PSP_ID, correlationId, NpgClient.PaymentMethod.CARDS)

        val events =
            TransactionInfoUtils.buildSimpleEventsList(
                correlationId,
                TransactionAuthorizationRequestData.PaymentGateway.NPG
            )

        val npgOperation =
            OperationDto().apply {
                operationId = OPERATION_ID
                operationType = OperationTypeDto.AUTHORIZATION
                operationResult =
                    it.pagopa.ecommerce.commons.generated.npg.v1.dto.OperationResultDto.EXECUTED
                this.paymentMethod = null
            }

        val orderResponse = OrderResponseDto().apply { operations = listOf(npgOperation) }

        given(ecommerceTransactionDataProvider.retrieveTransactionDetails(transactionId))
            .willReturn(Mono.just(tuple4))
        given(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    TRANSACTION_ID
                )
            )
            .willReturn(Flux.fromIterable(events))
        given(npgApiKeyConfiguration[paymentMethod, pspId]).willReturn(Either.right("apiKey"))
        given(npgClient.getOrder(any(), eq("apiKey"), eq(orderId)))
            .willReturn(Mono.just(orderResponse))

        StepVerifier.create(helpdeskService.searchNpgOperations(TRANSACTION_ID))
            .expectNextMatches { response ->
                response.operations?.size == 1 &&
                    response.operations?.first()?.paymentMethod == null
            }
            .verifyComplete()
    }
}
