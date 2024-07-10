package it.pagopa.ecommerce.helpdesk.dataproviders.v2

import it.pagopa.generated.ecommerce.helpdesk.v2.model.PmSearchPaymentMethodRequestDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchPaymentMethodResponseDto
import reactor.core.publisher.Mono

/**
 * Payment method data provider interface. This interface models common function for payment method
 * data provider in order to search for payment method given a specific
 * PmSearchPaymentMethodRequestDto criteria
 *
 * @see PmSearchPaymentMethodRequestDto
 */
interface PaymentMethodDataProvider {

    /**
     * Perform paginated query for retrieve all payment method information for the given search
     * criteria
     */
    fun findResult(
        searchParams: PmSearchPaymentMethodRequestDto
    ): Mono<SearchPaymentMethodResponseDto>
}
