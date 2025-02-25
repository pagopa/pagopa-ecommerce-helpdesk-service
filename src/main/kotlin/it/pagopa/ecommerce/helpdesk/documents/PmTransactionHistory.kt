package it.pagopa.ecommerce.helpdesk.documents

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "pm-transactions-view")
data class PmTransactionHistory(
    @Id val id: String,
    val userInfo: UserInfo,
    val transactionInfo: TransactionInfo,
    val paymentInfo: PaymentInfo,
    val pspInfo: PspInfo,
    val product: String
)

data class UserInfo(
    val userFiscalCode: String? = null,
    val notificationEmail: String,
    val authenticationType: Int
)

data class TransactionInfo(
    val creationDate: String,
    val status: Int,
    val statusDetails: Int? = null,
    val amount: Int? = null,
    val fee: Int,
    val grandTotal: Int,
    val rrn: String? = null,
    val authorizationCode: String? = null,
    val paymentMethodName: String? = null
)

data class PaymentInfo(val origin: String? = null, val details: List<PaymentDetailInfo>)

data class PaymentDetailInfo(
    val subject: String? = null,
    val iuv: String? = null,
    val idTransaction: String? = null,
    val creditorInstitution: String? = null,
    val paFiscalCode: String? = null,
    val amount: Int? = null
)

data class PspInfo(val pspId: String? = null, val businessName: String? = null, val idChannel: String? = null)
