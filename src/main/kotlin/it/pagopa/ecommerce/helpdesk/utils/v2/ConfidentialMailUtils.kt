package it.pagopa.ecommerce.helpdesk.utils.v2

import it.pagopa.ecommerce.commons.domain.v2.Email
import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import it.pagopa.ecommerce.helpdesk.utils.ConfidentialDataCacheUtils

/**
 * Class used for handling email address encryption and decryption.
 *
 * @param emailDataManager class for executing pdv call
 * @param encryptedToClearMap Map containing the opaque email address token as the key and the clear
 *   email address as the value.
 */
class ConfidentialMailUtils(
    private val emailDataManager: ConfidentialDataManager,
    private val encryptedToClearMap: MutableMap<String, ConfidentialDataManager.ConfidentialData> =
        mutableMapOf(),
) :
    ConfidentialDataCacheUtils<ConfidentialDataManager.ConfidentialData>(
        confidentialFromClearData = ::Email,
        confidentialDataManager = emailDataManager,
        encryptedToClearMap = encryptedToClearMap
    )
