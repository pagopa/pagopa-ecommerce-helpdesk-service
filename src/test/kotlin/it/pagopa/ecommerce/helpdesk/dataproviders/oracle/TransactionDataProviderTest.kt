package it.pagopa.ecommerce.helpdesk.dataproviders.oracle

import it.pagopa.ecommerce.helpdesk.dataproviders.TransactionDataProvider
import it.pagopa.generated.ecommerce.helpdesk.model.*
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class TransactionDataProviderTest {

    companion object {
        @JvmStatic
        private fun searchCriteriaMappingTestSource() =
            Stream.of(
                Arguments.of(SearchTransactionRequestPaymentTokenDto::class.java, "PAYMENT_TOKEN"),
                Arguments.of(SearchTransactionRequestRptIdDto::class.java, "RPT_ID"),
                Arguments.of(
                    SearchTransactionRequestTransactionIdDto::class.java,
                    "TRANSACTION_ID"
                ),
                Arguments.of(SearchTransactionRequestEmailDto::class.java, "USER_EMAIL"),
                Arguments.of(SearchTransactionRequestFiscalCodeDto::class.java, "USER_FISCAL_CODE"),
            )
    }

    @ParameterizedTest
    @MethodSource("searchCriteriaMappingTestSource")
    fun shouldMapSearchCriteriaType(clazz: Class<*>, searchType: String) {
        assertEquals(searchType, TransactionDataProvider.SearchTypeMapping.getSearchType(clazz))
    }
}
