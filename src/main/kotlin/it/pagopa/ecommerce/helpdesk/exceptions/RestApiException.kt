package it.pagopa.ecommerce.helpdesk.exceptions

import org.springframework.http.HttpStatus

/** Rest api exception, used to return an error specific HttpStatus and reason code to the caller */
class RestApiException(val httpStatus: HttpStatus, val title: String, val description: String) :
    RuntimeException(title)
