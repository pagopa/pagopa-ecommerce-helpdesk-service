package it.pagopa.ecommerce.helpdesk.utils

import it.pagopa.ecommerce.commons.domain.Email
import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager

/**
 * Class used for handling email address encryption and decryption.
 *
 * @param emailDataManager class for executing pdv call
 * @param encryptedToClearMap Map containing the opaque email address token as the key and the clear
 *   email address as the value.
 */
class ConfidentialMailUtils(
    private val emailDataManager: ConfidentialDataManager,
    private val encryptedToClearMap: MutableMap<String, Email> = mutableMapOf(),
) :
    ConfidentialDataCacheUtils<Email>(
        confidentialFromClearData = ::Email,
        confidentialDataManager = emailDataManager,
        encryptedToClearMap = encryptedToClearMap
    )
