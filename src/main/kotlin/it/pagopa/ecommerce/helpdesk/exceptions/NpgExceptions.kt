package it.pagopa.ecommerce.helpdesk.exceptions

import org.springframework.http.HttpStatus

class NoOperationDataFoundException(missingValueMessage: String?) :
    RuntimeException(missingValueMessage)

class NpgBadGatewayException(errorCodeReason: String?) :
    ApiError("Bad gateway : Received HTTP error code from NPG: $errorCodeReason") {
    override fun toRestException() =
        RestApiException(
            httpStatus = HttpStatus.BAD_GATEWAY,
            title = "Bad gateway",
            description = message ?: "Bad gateway from NPG"
        )
}

class NpgBadRequestException(transactionId: String?, orderId: String?, errorCodeReason: String?) :
    ApiError(
        transactionId?.let {
            "Transaction with id $it cannot be retrieved. Reason: Received HTTP error code from NPG: $errorCodeReason"
        }
            ?: "Npg state cannot be retrieved with orderId $orderId. Reason: Received HTTP error code from NPG: $errorCodeReason"
    ) {
    override fun toRestException() =
        RestApiException(
            httpStatus = HttpStatus.BAD_REQUEST,
            title = "Bad request",
            description = message ?: "Bad request from NPG"
        )
}
