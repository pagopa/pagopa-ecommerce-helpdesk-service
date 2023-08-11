package it.pagopa.ecommerce.helpdesk.utils

import it.pagopa.generated.ecommerce.helpdesk.model.TransactionResultDto
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class ResponseBuilderKtTest {

    companion object {
        @JvmStatic
        fun paginationData(): Stream<Arguments> =
            Stream.of(
                // total count, page size, expected pages
                Arguments.of(10, 10, 1),
                Arguments.of(10, 5, 2),
                Arguments.of(10, 4, 3),
                Arguments.of(2, 4, 1)
            )
    }

    @ParameterizedTest
    @MethodSource("paginationData")
    fun `should calculate total pages correctly`(
        totalCount: Int,
        pageSize: Int,
        expectedPages: Int
    ) {
        val paginatedResponse =
            buildTransactionSearchResponse(
                pageSize = pageSize,
                results = listOf(TransactionResultDto()),
                totalCount = totalCount,
                currentPage = 0
            )
        assertEquals(expectedPages, paginatedResponse.page.total)
    }
}
