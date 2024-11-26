package it.pagopa.ecommerce.helpdesk.exceptions

class NoOperationDataFoundException(missingValueMessage: String?) :
    RuntimeException(missingValueMessage)

class NpgBadGatewayException(errorCodeReason: String?) :
    RuntimeException("Bad gateway : Received HTTP error code from NPG: $errorCodeReason")

class NpgBadRequestException(transactionId: String?, errorCodeReason: String?) :
    RuntimeException(
        "Transaction with id $transactionId npg state cannot be retrieved. Reason: Received HTTP error code from NPG: $errorCodeReason"
    )
