package it.pagopa.ecommerce.helpdesk.exceptionhandler

import it.pagopa.ecommerce.helpdesk.exceptions.ApiError
import it.pagopa.ecommerce.helpdesk.exceptions.RestApiException
import it.pagopa.generated.ecommerce.helpdesk.model.ProblemJsonDto
import jakarta.xml.bind.ValidationException
import java.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ServerWebInputException

/**
 * Exception handler used to output a custom message in case an incoming request is invalid or an
 * api encounter an error and throw an RestApiException
 */
@RestControllerAdvice
class ExceptionHandler {

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    /** RestApiException exception handler */
    @ExceptionHandler(RestApiException::class)
    fun handleException(e: RestApiException): ResponseEntity<ProblemJsonDto> {
        logger.error("Exception processing request", e)
        return ResponseEntity.status(e.httpStatus)
            .body(
                ProblemJsonDto().status(e.httpStatus.value()).title(e.title).detail(e.description)
            )
    }

    /** ApiError exception handler */
    @ExceptionHandler(ApiError::class)
    fun handleException(e: ApiError): ResponseEntity<ProblemJsonDto> {
        return handleException(e.toRestException())
    }

    /** Validation request exception handler */
    @ExceptionHandler(
        MethodArgumentNotValidException::class,
        MethodArgumentTypeMismatchException::class,
        ServerWebInputException::class,
        ValidationException::class,
        HttpMessageNotReadableException::class,
        WebExchangeBindException::class
    )
    fun handleRequestValidationException(e: Exception): ResponseEntity<ProblemJsonDto> {

        logger.error("Input request is not valid", e)
        return ResponseEntity.badRequest()
            .body(
                ProblemJsonDto()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .title("Bad request")
                    .detail(e.localizedMessage)
            )
    }

    /** Handler for generic exception */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(e: Exception): ResponseEntity<ProblemJsonDto> {
        logger.error("Exception processing the request", e)
        return ResponseEntity.internalServerError()
            .body(
                ProblemJsonDto()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .title("Error processing the request")
                    .detail("Generic error occurred")
            )
    }
}
