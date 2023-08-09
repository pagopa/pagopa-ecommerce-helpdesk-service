package it.pagopa.ecommerce.helpdesk.exceptionhandler

import it.pagopa.generated.ecommerce.helpdesk.model.ProblemJsonDto
import org.springframework.http.HttpStatus

object HelpdeskTestUtils {

    fun buildProblemJson(
        httpStatus: HttpStatus,
        title: String,
        description: String
    ): ProblemJsonDto =
        ProblemJsonDto()
            .status(httpStatus.value())
            .detail(description)
            .title(title)
}