package it.pagopa.ecommerce.helpdesk.documents

enum class UserStatus(val code: Int, val description: String) {
    UNREGISTERED(0, "Utente non registrato"),
    REGISTERED_NO_SPID(1, "Utente registrato non SPID"),
    PENDING_OTP_VERIFICATION(2, "Utente in attesa di verifica OTP"),
    PASSWORD_TO_SET(3, "Password da impostare"),
    PASSWORD_RESET(4, "Password da impostare - cambio password"),
    USER_DELETED(5, "Utente cancellato"),
    REGISTERED_SPID(11, "Utente registrato SPID"),
    REGISTERED_WITH_CIE(12, "Utente registrato su IO con CIE");

    companion object {
        fun fromCode(code: Int): String {
            return UserStatus.values().find { it.code == code }?.name ?: code.toString()
        }
    }
}

enum class PaymentStatus(val code: Int, val description: String) {
    TO_AUTHORIZE(0, "Da autorizzare"),
    PENDING(1, "In attesa"),
    PENDING_MOD1(2, "In attesa mod1"),
    CONFIRMED(3, "Confermato"),
    REJECTED(4, "Rifiutato"),
    PENDING_XPAY(6, "In attesa di XPAY"),
    ERROR(7, "In errore"),
    CONFIRMED_MOD1(8, "Confermato mod1"),
    CONFIRMED_MOD2(9, "Confermato mod2"),
    REJECTED_AGAIN(10, "Rifiutato"),
    MISSING_CALLBACK_PSP(11, "Missing callback from PSP"),
    PAYMENT_TAKEN(12, "Pagamento preso in carico"),
    EXPIRED_3DS(13, "3DS Scaduto"),
    AUTHORIZED_NODE_TIMEOUT(14, "Authorized with nodo timeout"),
    AWAITING_3DS2_METHOD(15, "In attesa del metodo 3ds2"),
    AWAITING_3DS2_CHALLENGE(16, "In attesa della challenge 3ds2"),
    RETURNING_3DS2_METHOD(17, "Ritornando dal metodo 3ds2"),
    RETURNING_3DS2_CHALLENGE(18, "Ritornando dalla challenge 3ds2"),
    XPAY_PPAL_BPAY_TO_BE_REVERSED(19, "Transazione XPAY / PPAL / BPAY da stornare"),
    XPAY_REVERSED_BATCH(20, "Transazione XPAY stornata da batch"),
    PAYMENT_AUTHORIZED_BY_GATEWAY(21, "Pagamento Autorizzato dal Payment Gateway");

    companion object {
        fun fromCode(code: Int): String {
            return values().find { it.code == code }?.name ?: code.toString()
        }
    }
}

enum class AccountingStatus(val code: Int, val description: String) {
    NOT_MANAGED(0, "Non gestito"),
    ACCOUNTED(1, "Contabilizzato"),
    ACCOUNTING_ERROR(2, "Errore di contabilizzazione"),
    REVERSED(3, "Stornato"),
    REVERSAL_ERROR(4, "Errore Storno"),
    RECEIPT_CREATED(5, "Ricevuta creata");

    companion object {
        fun fromCode(code: Int): String {
            return AccountingStatus.values().find { it.code == code }?.name ?: code.toString()
        }
    }
}
