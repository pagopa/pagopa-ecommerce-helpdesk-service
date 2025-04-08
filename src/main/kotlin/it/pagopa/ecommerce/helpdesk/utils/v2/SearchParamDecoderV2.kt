package it.pagopa.ecommerce.helpdesk.utils.v2

import it.pagopa.ecommerce.helpdesk.utils.confidentialDataUtils
import it.pagopa.generated.ecommerce.helpdesk.v2.model.HelpDeskSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchTransactionRequestEmailDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchTransactionRequestFiscalCodeDto
import reactor.core.publisher.Mono

/** Data class that wraps search parameter with optional confidentialDataUtils class */
class SearchParamDecoderV2<out T>(
    val searchParameter: T,
    val confidentialDataUtils: ConfidentialDataUtils?
) where T : HelpDeskSearchTransactionRequestDto {

    /**
     * Decode search parameter to the one to be used for transaction searching.
     *
     * @return the decoded search parameter if a decoding operation is defined for input parameter
     *   or search parameter itself without any transformation
     */
    fun decode(): Mono<out HelpDeskSearchTransactionRequestDto> =
        when (searchParameter) {
            /*
             * Search parameter decoding is performed iff input search parameter is an email or fiscal code and the searching is performed for eCommerce transactions.
             * In fact confidentialDataUtils parameter is optional and valued only when searching for eCommerce transactions
             */
            is SearchTransactionRequestEmailDto ->
                if (confidentialDataUtils != null) {
                    confidentialDataUtils.toConfidential(searchParameter.userEmail).map {
                        SearchTransactionRequestEmailDto().userEmail(it.opaqueData).type("EMAIL")
                    }
                } else {
                    Mono.just(searchParameter)
                }
            is SearchTransactionRequestFiscalCodeDto ->
                if (confidentialDataUtils != null) {
                    confidentialDataUtils.toConfidential(searchParameter.userFiscalCode).map {
                        SearchTransactionRequestFiscalCodeDto()
                            .userFiscalCode(it.opaqueData)
                            .type("USER_FISCAL_CODE")
                    }
                } else {
                    Mono.just(searchParameter)
                }
            else -> Mono.just(searchParameter)
        }
}
