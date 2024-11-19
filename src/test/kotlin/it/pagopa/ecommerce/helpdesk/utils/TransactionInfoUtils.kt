package it.pagopa.ecommerce.helpdesk.utils

import it.pagopa.ecommerce.commons.documents.v2.TransactionAuthorizationRequestData
import it.pagopa.ecommerce.commons.documents.v2.TransactionEvent
import it.pagopa.ecommerce.commons.documents.v2.activation.NpgTransactionGatewayActivationData
import it.pagopa.ecommerce.commons.documents.v2.refund.NpgGatewayRefundData
import it.pagopa.ecommerce.commons.generated.npg.v1.dto.OperationResultDto
import it.pagopa.ecommerce.commons.generated.npg.v1.dto.OrderResponseDto
import it.pagopa.ecommerce.commons.v2.TransactionTestUtils
import reactor.core.publisher.Mono

class TransactionInfoUtils {
    companion object {
        val orderId = "orderId"
        val refundOperationId = "refundOperationId"

        fun buildOrderResponseDtoNullOperation(): Mono<OrderResponseDto> {
            val orderResponseDto = OrderResponseDto()
            return Mono.just(orderResponseDto)
        }
        fun buildEventsList(
            correlationId: String,
            gateway: TransactionAuthorizationRequestData.PaymentGateway
        ): List<TransactionEvent<Any>> {
            val transactionActivatedEvent =
                TransactionTestUtils.transactionActivateEvent(
                    NpgTransactionGatewayActivationData(orderId, correlationId)
                )
            val transactionAuthorizationRequestedEvent =
                TransactionTestUtils.transactionAuthorizationRequestedEvent(gateway)

            TransactionTestUtils.transactionAuthorizationRequestedEvent(gateway)

            val transactionExpiredEvent =
                TransactionTestUtils.transactionExpiredEvent(
                    TransactionTestUtils.reduceEvents(
                        transactionActivatedEvent,
                        transactionAuthorizationRequestedEvent
                    )
                )
            val transactionRefundRequestedEvent =
                TransactionTestUtils.transactionRefundRequestedEvent(
                    TransactionTestUtils.reduceEvents(
                        transactionActivatedEvent,
                        transactionAuthorizationRequestedEvent,
                        transactionExpiredEvent
                    ),
                    null
                )
            val transactionRefundErrorEvent =
                TransactionTestUtils.transactionRefundErrorEvent(
                    TransactionTestUtils.reduceEvents(
                        transactionActivatedEvent,
                        transactionAuthorizationRequestedEvent,
                        transactionExpiredEvent,
                        transactionRefundRequestedEvent
                    )
                )
            val transactionRefundRetryEvent =
                TransactionTestUtils.transactionRefundRetriedEvent(
                    1,
                    TransactionTestUtils.npgTransactionGatewayAuthorizationData(
                        OperationResultDto.EXECUTED
                    )
                )
            val transactionRefundedEvent =
                TransactionTestUtils.transactionRefundedEvent(
                    TransactionTestUtils.reduceEvents(
                        transactionActivatedEvent,
                        transactionAuthorizationRequestedEvent,
                        transactionExpiredEvent,
                        transactionRefundRequestedEvent,
                        transactionRefundErrorEvent,
                        transactionRefundRetryEvent
                    ),
                    NpgGatewayRefundData(refundOperationId)
                )

            return (listOf(
                transactionActivatedEvent,
                transactionAuthorizationRequestedEvent,
                transactionExpiredEvent,
                transactionRefundRequestedEvent,
                transactionRefundErrorEvent,
                transactionRefundRetryEvent,
                transactionRefundedEvent
            )
                as List<TransactionEvent<Any>>)
        }
    }
}
