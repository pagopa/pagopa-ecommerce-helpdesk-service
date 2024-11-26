package it.pagopa.ecommerce.helpdesk.exceptions

import org.springframework.http.HttpStatus

class UnsupportedTransactionVersionException(reason: String) : ApiError(reason) {

    override fun toRestException() =
        RestApiException(
            httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
            description = this.message!!,
            title = "Unsupported version"
        )
}
