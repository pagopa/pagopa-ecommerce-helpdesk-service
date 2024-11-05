package it.pagopa.ecommerce.helpdesk.utils

/** Enum class representing the type provider to be used for transaction searches. */
enum class PmProviderType {
    /** Legacy PM transaction provider */
    PM_LEGACY,

    /** Provider for searching PM transactions in eCommerce history */
    ECOMMERCE_HISTORY
}
