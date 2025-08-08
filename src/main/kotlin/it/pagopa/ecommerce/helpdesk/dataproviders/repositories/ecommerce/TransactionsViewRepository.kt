package it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.reactivestreams.client.MongoClient
import it.pagopa.ecommerce.commons.documents.BaseTransactionView
import it.pagopa.ecommerce.commons.documents.DeadLetterEvent
import it.pagopa.ecommerce.helpdesk.configurations.MongoClientsProvider
import it.pagopa.ecommerce.helpdesk.documents.EcommerceStatusCount
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.bson.Document
import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import com.mongodb.client.model.Aggregates.*
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Accumulators.sum
import org.bson.types.ObjectId


/** eCommerce Mongo transaction view repository */
@ApplicationScoped
class TransactionsViewRepository {

    @Inject
    lateinit var mongoClient: MongoClient

    @Inject
    lateinit var mongoClientsProvider: MongoClientsProvider

    private val collectionName = "transactions-view"


    private fun getCollection() =
        mongoClient.getDatabase(mongoClientsProvider.getDefaultDatabaseName())
            .getCollection(collectionName, BaseTransactionView::class.java)

    /*@Query("{'paymentNotices.paymentToken': '?0'}", count = true)
    fun countTransactionsWithPaymentToken(paymentToken: String): Mono<Long>*/

    fun countTransactionsWithPaymentToken(paymentToken: String): Mono<Long> {

        val filter = Filters.eq("paymentNotices.paymentToken", paymentToken)

        return Mono.from(getCollection().countDocuments(filter))
    }

    /*@Aggregation(
        "{\$match: {'paymentNotices.paymentToken': '?0'}}",
        "{\$sort: {'creationDate': -1}}",
        "{\$skip: ?1}",
        "{\$limit: ?2}",
    )
    fun findTransactionsWithPaymentTokenPaginatedOrderByCreationDateDesc(
        paymentToken: String,
        skip: Int,
        limit: Int,
    ): Flux<BaseTransactionView>*/
    fun findTransactionsWithPaymentTokenPaginatedOrderByCreationDateDesc(
        paymentToken: String,
        skip: Int,
        limit: Int
    ): Flux<BaseTransactionView> {
        val pipeline = listOf(
            Aggregates.match(Filters.eq("paymentNotices.paymentToken", paymentToken)),
            Aggregates.sort(Document("creationDate", -1)),
            Aggregates.skip(skip),
            Aggregates.limit(limit)
        )

        return Flux.from(getCollection().aggregate(pipeline, BaseTransactionView::class.java))
    }


    /*@Query("{'paymentNotices.rptId': '?0'}", count = true)
    fun countTransactionsWithRptId(rpiId: String): Mono<Long>*/
    fun countTransactionsWithRptId(rptId: String): Mono<Long> {
        val filter = Filters.eq("paymentNotices.rptId", rptId)
        return Mono.from(
            getCollection().countDocuments(filter)
        )
    }

    /*@Aggregation(
        "{\$match: {'paymentNotices.rptId': '?0'}}",
        "{\$sort: {'creationDate': -1}}",
        "{\$skip: ?1}",
        "{\$limit: ?2}",
    )
    fun findTransactionsWithRptIdPaginatedOrderByCreationDateDesc(
        rptId: String,
        skip: Int,
        limit: Int
    ): Flux<BaseTransactionView>*/
    fun findTransactionsWithRptIdPaginatedOrderByCreationDateDesc(
        rptId: String,
        skip: Int,
        limit: Int
    ): Flux<BaseTransactionView> {
        val pipeline = listOf(
            Aggregates.match(Filters.eq("paymentNotices.rptId", rptId)),
            Aggregates.sort(Document("creationDate", -1)),
            Aggregates.skip(skip),
            Aggregates.limit(limit)
        )

        return Flux.from(
            getCollection().aggregate(pipeline, BaseTransactionView::class.java)
        )
    }


    /*@Query("{'email.data': '?0'}", count = true)
    fun countTransactionsWithEmail(encryptedEmail: String): Mono<Long>*/
    fun countTransactionsWithEmail(encryptedEmail: String): Mono<Long> {
        val filter = Filters.eq("email.data", encryptedEmail)
        return Mono.from(
            getCollection().countDocuments(filter)
        )
    }

    /*@Query("{'userId': '?0'}", count = true)
    fun countTransactionsWithFiscalCode(encryptedFiscalCode: String): Mono<Long>*/
    fun countTransactionsWithFiscalCode(encryptedFiscalCode: String): Mono<Long> {
        val filter = Filters.eq("userId", encryptedFiscalCode)
        return Mono.from(
            getCollection().countDocuments(filter)
        )
    }


    /*@Aggregation(
        "{\$match: {'email.data': '?0'}}",
        "{\$sort: {'creationDate': -1}}",
        "{\$skip: ?1}",
        "{\$limit: ?2}",
    )
    fun findTransactionsWithEmailPaginatedOrderByCreationDateDesc(
        encryptedEmail: String,
        skip: Int,
        limit: Int
    ): Flux<BaseTransactionView>*/
    fun findTransactionsWithEmailPaginatedOrderByCreationDateDesc(
        encryptedEmail: String,
        skip: Int,
        limit: Int
    ): Flux<BaseTransactionView> {
        val pipeline = listOf(
            match(eq("email.data", encryptedEmail)),
            sort(Document("creationDate", -1)),
            skip(skip),
            limit(limit)
        )

        return Flux.from(
            getCollection().aggregate(pipeline, BaseTransactionView::class.java)
        )
    }


    /*@Aggregation(
        "{\$match: {'userId': '?0'}}",
        "{\$sort: {'creationDate': -1}}",
        "{\$skip: ?1}",
        "{\$limit: ?2}",
    )
    fun findTransactionsWithFiscalCodePaginatedOrderByCreationDateDesc(
        encryptedFiscalCode: String,
        skip: Int,
        limit: Int
    ): Flux<BaseTransactionView>*/

    fun findTransactionsWithFiscalCodePaginatedOrderByCreationDateDesc(
        encryptedFiscalCode: String,
        skip: Int,
        limit: Int
    ): Flux<BaseTransactionView> {
        val pipeline = listOf(
            match(eq("userId", encryptedFiscalCode)),
            sort(Document("creationDate", -1)),
            skip(skip),
            limit(limit)
        )

        return Flux.from(
            getCollection().aggregate(pipeline, BaseTransactionView::class.java)
        )
    }


    /*@Aggregation(
        "{ \$match: { 'creationDate': { \$gte: ?0, \$lte: ?1 }, clientId: ?2, pspId: ?3, paymentTypeCode: ?4 } }",
        "{ \$group: { _id: '\$status', count: { \$sum: 1 } } }"
    )
    fun findMetricsGivenStartDateAndEndDateAndClientIdAndPspIdAndPaymentTypeCode(
        startDate: String,
        endDate: String,
        clientId: String,
        pspId: String,
        paymentTypeCode: String
    ): Flux<EcommerceStatusCount>*/
    fun findMetricsGivenStartDateAndEndDateAndClientIdAndPspIdAndPaymentTypeCode(
        startDate: String,
        endDate: String,
        clientId: String,
        pspId: String,
        paymentTypeCode: String
    ): Flux<EcommerceStatusCount> {
        val pipeline = listOf(
            match(
                and(
                    gte("creationDate", startDate),
                    lte("creationDate", endDate),
                    eq("clientId", clientId),
                    eq("pspId", pspId),
                    eq("paymentTypeCode", paymentTypeCode)
                )
            ),
            group("\$status", sum("count", 1))
        )
        return Flux.from(
            getCollection().aggregate(pipeline, EcommerceStatusCount::class.java)
        )
    }

    fun findById(
        transactionId: String
    ): BaseTransactionView? {
        val filter = Document("_id", ObjectId(transactionId))
        return getCollection().find(filter).first()
    }

    fun existsById(transactionId: String): Mono<Boolean> {
        val filter = Document("_id", ObjectId(transactionId))
        return Mono.from(getCollection().find(filter).limit(1).first())
            .map { true }
            .defaultIfEmpty(false)
    }

}
