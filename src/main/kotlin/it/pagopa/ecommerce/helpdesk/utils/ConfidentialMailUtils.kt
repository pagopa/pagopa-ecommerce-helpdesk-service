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
 * @param encryptedToClearMap Map containing the opaque mail token as the key and the clear mail as
 *   the value.
 */
class ConfidentialMailUtils(
    private val emailConfidentialDataManager: ConfidentialDataManager,
    private val encryptedToClearMap: MutableMap<String, Email> = mutableMapOf()
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
        return if (encryptedToClearMap.contains(encrypted.opaqueData)) {
            logger.info("email decrypt cache hit")
            mono { encryptedToClearMap[encrypted.opaqueData] }
        } else {
            emailConfidentialDataManager
                .decrypt(encrypted, ::Email)
                .doOnError { e -> logger.error("Exception decrypting confidential data", e) }
                .doOnNext { decryptedEmail ->
                    encryptedToClearMap[encrypted.opaqueData] = decryptedEmail
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
        return mono {
                encryptedToClearMap.entries
                    .stream()
                    .filter { it.value == clearText }
                    .map { it.key }
                    .findFirst()
                    .map { it }
            }
            .flatMap {
                if (it.isEmpty) {
                    emailConfidentialDataManager
                        .encrypt(clearText)
                        .doOnNext { encryptedToClearMap[it.opaqueData] = clearText }
                        .doOnError { e ->
                            logger.error("Exception encrypting confidential data", e)
                        }
                } else {
                    Mono.just(Confidential(it.get()))
                }
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
