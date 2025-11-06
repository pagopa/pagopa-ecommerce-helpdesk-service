package it.pagopa.ecommerce.helpdesk.services.v2

import it.pagopa.ecommerce.commons.documents.BaseTransactionEvent
import it.pagopa.ecommerce.commons.documents.BaseTransactionView
import it.pagopa.ecommerce.commons.domain.Confidential
import it.pagopa.ecommerce.commons.domain.v2.Email
import it.pagopa.ecommerce.commons.generated.server.model.TransactionStatusDto
import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import it.pagopa.ecommerce.commons.v1.TransactionTestUtils
import it.pagopa.ecommerce.helpdesk.HelpdeskTestUtilsV2
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce.TransactionsEventStoreRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce.TransactionsViewRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.history.TransactionsEventStoreRepository as TransactionsEventStoreHistoryRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.history.TransactionsViewRepository as TransactionsViewHistoryRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.v2.mongo.EcommerceTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.v2.mongo.StateMetricsDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.generated.ecommerce.helpdesk.v2.model.*
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class EcommerceServiceTest {

    private val ecommerceTransactionDataProvider: EcommerceTransactionDataProvider = mock()

    private val stateMetricsDataProvider: StateMetricsDataProvider = mock()

    private val confidentialDataManager: ConfidentialDataManager = mock()

    private val testEmail = "test@test.it"

    private val encryptedEmail = TransactionTestUtils.EMAIL.opaqueData
    private val transactionsViewRepository: TransactionsViewRepository = mock()
    private val transactionsViewHistoryRepository: TransactionsViewHistoryRepository = mock()

    private val transactionsEventStoreRepository: TransactionsEventStoreRepository<Any> = mock()
    private val transactionsEventStoreHistoryRepository:
        TransactionsEventStoreHistoryRepository<Any> =
        mock()

    private val ecommerceService =
        EcommerceService(
            ecommerceTransactionDataProvider,
            confidentialDataManager,
            confidentialDataManager,
            stateMetricsDataProvider
        )

    @Test
    fun `should return found transaction successfully`() {
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByRptId()
        val pageSize = 10
        val pageNumber = 0
        val totalCount = 100
        val transactions =
            listOf(
                HelpdeskTestUtilsV2.buildTransactionResultDto(
                    OffsetDateTime.now(),
                    ProductDto.ECOMMERCE
                )
            )
        given(
                ecommerceTransactionDataProvider.totalRecordCount(
                    argThat { this.searchParameter == searchCriteria }
                )
            )
            .willReturn(Mono.just(totalCount))
        given(
                ecommerceTransactionDataProvider.findResult(
                    searchParams = argThat { this.searchParameter == searchCriteria },
                    skip = eq(pageSize * pageNumber),
                    limit = eq(pageSize)
                )
            )
            .willReturn(Mono.just(transactions))
        val expectedResponse =
            SearchTransactionResponseDto()
                .transactions(transactions)
                .page(PageInfoDto().results(transactions.size).total(10).current(pageNumber))
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
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByRptId()
        val pageSize = 10
        val pageNumber = 0
        val totalCount = 0
        given(
                ecommerceTransactionDataProvider.totalRecordCount(
                    argThat { this.searchParameter == searchCriteria }
                )
            )
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

    @Test
    fun `should invoke PDV only once recovering found transaction successfully`() {
        val searchCriteria = HelpdeskTestUtilsV2.buildSearchRequestByUserMail(testEmail)
        val pageSize = 10
        val pageNumber = 0
        val totalCount = 100
        val transactionDocument =
            TransactionTestUtils.transactionDocument(
                TransactionStatusDto.ACTIVATED,
                ZonedDateTime.now()
            ) as BaseTransactionView
        val transactions =
            listOf(
                HelpdeskTestUtilsV2.buildTransactionResultDto(
                    OffsetDateTime.now(),
                    ProductDto.ECOMMERCE
                )
            )
        given(confidentialDataManager.encrypt(Email(testEmail)))
            .willReturn(Mono.just(Confidential(encryptedEmail)))
        given(transactionsViewRepository.countTransactionsWithEmail(encryptedEmail))
            .willReturn(Mono.just(totalCount.toLong()))
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
        val ecommerceServiceLocalMock =
            EcommerceService(
                confidentialDataManagerEmail = confidentialDataManager,
                confidentialDataManagerFiscalCode = confidentialDataManager,
                ecommerceTransactionDataProvider =
                    EcommerceTransactionDataProvider(
                        transactionsViewRepository = transactionsViewRepository,
                        transactionsViewHistoryRepository = transactionsViewHistoryRepository,
                        transactionsEventStoreRepository = transactionsEventStoreRepository,
                        transactionsEventStoreHistoryRepository =
                            transactionsEventStoreHistoryRepository,
                    ),
                stateMetricsDataProvider = stateMetricsDataProvider
            )

        StepVerifier.create(
                ecommerceServiceLocalMock.searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    ecommerceSearchTransactionRequestDto = searchCriteria
                )
            )
            .assertNext {
                Assertions.assertEquals(
                    it.page,
                    PageInfoDto().results(transactions.size).total(10).current(pageNumber)
                )
            }
            .verifyComplete()

        verify(confidentialDataManager, times(1)).encrypt(any())
        verify(confidentialDataManager, times(0)).decrypt(any<Confidential<Email>>(), any())
    }

    @Test
    fun `should return metrics successfully`() {
        val searchMetrics = HelpdeskTestUtilsV2.buildSearchMetrics()

        val expectedResponse =
            TransactionMetricsResponseDto()
                .ACTIVATED(1)
                .AUTHORIZATION_REQUESTED(2)
                .AUTHORIZATION_COMPLETED(3)
                .CLOSURE_REQUESTED(4)
                .CLOSED(5)
                .CLOSURE_ERROR(6)
                .NOTIFIED_OK(7)
                .NOTIFIED_KO(8)
                .NOTIFICATION_ERROR(9)
                .NOTIFICATION_REQUESTED(10)
                .EXPIRED(11)
                .REFUNDED(12)
                .CANCELED(13)
                .EXPIRED_NOT_AUTHORIZED(14)
                .UNAUTHORIZED(15)
                .REFUND_ERROR(16)
                .REFUND_REQUESTED(17)
                .CANCELLATION_REQUESTED(18)
                .CANCELLATION_EXPIRED(19)

        given(stateMetricsDataProvider.computeMetrics(searchMetrics))
            .willReturn(Mono.just(expectedResponse))

        StepVerifier.create(ecommerceService.searchMetrics(searchMetrics))
            .expectNext(expectedResponse)
            .verifyComplete()

        verify(stateMetricsDataProvider, times(1)).computeMetrics(searchMetrics)
    }
}
