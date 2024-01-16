package it.pagopa.ecommerce.helpdesk.utils

import it.pagopa.ecommerce.commons.domain.Confidential
import it.pagopa.ecommerce.commons.domain.Email
import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

/**
 * Class used for handle mail encryption end decryption.
 *
 * @param emailConfidentialDataManager class for execute pdv call
 * @param emailCachedMap Map containing the opaque mail token as the key and the clear mail as the
 *   value.
 */
class ConfidentialMailUtils(
    private val emailConfidentialDataManager: ConfidentialDataManager,
    private val emailCachedMap: MutableMap<String, Email> = mutableMapOf()
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * The method search clear mail into cache if not found make a call to pdv to decrypt the email
     * then cache it
     *
     * @param encrypted email encrypted
     * @return Mono<Email> return mono with clear email value object
     */
    fun toEmail(encrypted: Confidential<Email>): Mono<Email> {
        return if (emailCachedMap.contains(encrypted.opaqueData)) {
            mono { emailCachedMap[encrypted.opaqueData] }
        } else {
            emailConfidentialDataManager
                .decrypt(encrypted, ::Email)
                .doOnError { e -> logger.error("Exception decrypting confidential data", e) }
                .map { decryptedEmail ->
                    emailCachedMap[encrypted.opaqueData] = decryptedEmail
                    decryptedEmail
                }
        }
    }

    /**
     * The method used for encrypt mail using pdv
     *
     * @param clearText clear email
     * @return Mono<Confidential<Email>> return mono with encrypted email
     */
    private fun toConfidential(clearText: Email): Mono<Confidential<Email>> {
        return emailConfidentialDataManager.encrypt(clearText).doOnError { e ->
            logger.error("Exception encrypting confidential data", e)
        }
    }

    /**
     * The method used for encrypt mail using pdv
     *
     * @param clearText clear email
     * @return Mono<Confidential<Email>> return mono with encrypted email
     */
    fun toConfidential(email: String): Mono<Confidential<Email>> {
        return toConfidential(Email(email))
    }
}
