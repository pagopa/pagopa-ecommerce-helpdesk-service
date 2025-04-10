package it.pagopa.ecommerce.helpdesk.utils

import it.pagopa.ecommerce.commons.domain.FiscalCode
import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager

/**
 * Class used for handling fiscal code encryption and decryption.
 *
 * @param fiscalCodeConfidentialDataManager class for executing pdv call
 * @param encryptedToClearMap Map containing the opaque fiscal code token as the key and the clear
 *   fiscal code as the value.
 */
class ConfidentialFiscalCodeUtils(
    private val fiscalCodeConfidentialDataManager: ConfidentialDataManager,
    private val encryptedToClearMap: MutableMap<String, FiscalCode> = mutableMapOf(),
) :
    ConfidentialDataCacheUtils<FiscalCode>(
        confidentialFromClearData = ::FiscalCode,
        confidentialDataManager = fiscalCodeConfidentialDataManager,
        encryptedToClearMap = encryptedToClearMap
    )
