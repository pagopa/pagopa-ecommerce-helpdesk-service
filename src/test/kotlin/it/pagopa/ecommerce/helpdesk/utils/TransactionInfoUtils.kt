package it.pagopa.ecommerce.helpdesk.utils

import it.pagopa.ecommerce.commons.documents.v2.TransactionAuthorizationRequestData
import it.pagopa.ecommerce.commons.documents.v2.TransactionAuthorizationRequestedEvent
import it.pagopa.ecommerce.commons.documents.v2.TransactionEvent
import it.pagopa.ecommerce.commons.documents.v2.activation.NpgTransactionGatewayActivationData
import it.pagopa.ecommerce.commons.generated.npg.v1.dto.OrderResponseDto
import it.pagopa.ecommerce.commons.v2.TransactionTestUtils
import reactor.core.publisher.Mono

class TransactionInfoUtils {
    companion object {
        val orderId = "orderId"

        fun buildOrderResponseDtoNullOperation(): Mono<OrderResponseDto> {
            val orderResponseDto = OrderResponseDto()
            return Mono.just(orderResponseDto)
        }

        fun buildSimpleEventsList(
            correlationId: String?,
            gateway: TransactionAuthorizationRequestData.PaymentGateway
        ): List<TransactionEvent<Any>> {
            val transactionActivatedEvent =
                TransactionTestUtils.transactionActivateEvent(
                    NpgTransactionGatewayActivationData(orderId, correlationId)
                )
            val transactionAuthorizationRequestedEvent =
                TransactionTestUtils.transactionAuthorizationRequestedEvent(gateway)

            return (listOf(transactionActivatedEvent, transactionAuthorizationRequestedEvent)
                as List<TransactionEvent<Any>>)
        }

        fun buildEventsList(
            correlationId: String,
            transactionAuthorizationRequestedEvent: TransactionAuthorizationRequestedEvent
        ): List<TransactionEvent<Any>> {
            val transactionActivatedEvent =
                TransactionTestUtils.transactionActivateEvent(
                    NpgTransactionGatewayActivationData(orderId, correlationId)
                )

            return (listOf(transactionActivatedEvent, transactionAuthorizationRequestedEvent)
                as List<TransactionEvent<Any>>)
        }
    }
}
