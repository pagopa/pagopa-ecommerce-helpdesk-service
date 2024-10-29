package it.pagopa.ecommerce.helpdesk.documents

import java.time.Instant
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
    val userFiscalCode: String,
    val notificationEmail: String,
    val authenticationType: Int
)

data class TransactionInfo(
    val creationDate: String,
    val status: Int,
    val statusDetails: Int,
    val amount: Int,
    val fee: Int,
    val grandTotal: Int,
    val rrn: String,
    val authorizationCode: String,
    val paymentMethodName: String
)

data class PaymentInfo(val origin: String, val details: List<PaymentDetailInfo>)

data class PaymentDetailInfo(
    val subject: String,
    val iuv: String,
    val idTransaction: String,
    val creditorInstitution: String,
    val paFiscalCode: String,
    val amount: Int
)

data class PspInfo(val pspId: String, val businessName: String, val idChannel: String)
