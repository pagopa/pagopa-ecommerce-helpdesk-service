package it.pagopa.ecommerce.helpdesk.utils.v2

import it.pagopa.ecommerce.commons.mdcutilities.LogTracingUtils
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchTransactionRequestAuthorizationRequestIdDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchTransactionRequestPaymentTokenDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchTransactionRequestRptIdDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchTransactionRequestTransactionIdDto

object LogUtils {
    /**
     * Extracts a specific tracing attribute key-value pair based on the concrete type of the
     * provided DTO. The extracted value is mapped to its corresponding
     * [LogTracingUtils.AttributeKeys].
     *
     * @param dto the generic request DTO to be evaluated.
     * @return a [Pair] containing the matched [LogTracingUtils.AttributeKeys] and its string value,
     *   or `null` if the DTO type does not carry a specific tracing identifier.
     */
    fun extractContextAttributeFromDto(dto: Any): Pair<LogTracingUtils.AttributeKeys, String>? {
        return when (dto) {
            is SearchTransactionRequestTransactionIdDto -> {
                LogTracingUtils.AttributeKeys.CTX_TRANSACTION_ID to dto.transactionId
            }
            is SearchTransactionRequestRptIdDto -> {
                LogTracingUtils.AttributeKeys.CTX_RPT_IDS to dto.rptId
            }
            is SearchTransactionRequestPaymentTokenDto -> {
                LogTracingUtils.AttributeKeys.CTX_PAYMENT_TOKENS to dto.paymentToken
            }
            is SearchTransactionRequestAuthorizationRequestIdDto -> {
                LogTracingUtils.AttributeKeys.CTX_AUTHORIZATION_REQUEST_ID to
                    dto.authorizationRequestId
            }
            else -> null
        }
    }
}
