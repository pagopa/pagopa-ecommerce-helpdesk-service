conn = new Mongo();
db = conn.getDB("ecommerce");

db.getCollection('eventstore').insertMany([{
  "_id": "45917e51-30ce-4cf1-aacd-b691b50e2710",
  "eventCode": "TRANSACTION_ACTIVATED_EVENT",
  "transactionId": "d35da9bdc5554054a64abbec6efa6baa",
  "creationDate": "2023-08-07T15:32:56.592837917Z[Etc/UTC]",
  "data": {
    "email": {
      "data": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
    },
    "paymentNotices": [
      {
        "paymentToken": "1dd0db6d104e4ed3aaf261e8b28e9302",
        "rptId": "97834130581302217169142236141",
        "description": "Diritti di segreteria - accreditamento attività formative - codice evento: 23o13742",
        "amount": 30000,
        "transferList": [
          {
            "paFiscalCode": "97834130581",
            "digitalStamp": false,
            "transferAmount": 30000,
            "transferCategory": "0101104TS"
          }
        ],
        "isAllCCP": false
      }
    ],
    "clientId": "CHECKOUT",
    "paymentTokenValiditySeconds": 900
  },
  "_class": "it.pagopa.ecommerce.commons.documents.v1.TransactionActivatedEvent"
},
{
  "_id": "2318b2d8-8b59-4280-8873-6b6b9e5c88c2",
  "eventCode": "TRANSACTION_AUTHORIZATION_REQUESTED_EVENT",
  "transactionId": "d35da9bdc5554054a64abbec6efa6baa",
  "creationDate": "2023-08-07T15:33:00.080620043Z[Etc/UTC]",
  "data": {
    "amount": 30000,
    "fee": 100,
    "paymentInstrumentId": "378d0b4f-8b69-46b0-8215-07785fe1aad4",
    "pspId": "BCITITMM",
    "paymentTypeCode": "CP",
    "brokerName": "00799960158",
    "pspChannelCode": "00799960158_10_ONUS",
    "paymentMethodName": "Carte",
    "pspBusinessName": "Intesa Sanpaolo S.p.A",
    "isPspOnUs": true,
    "authorizationRequestId": "bf5b0bb4-b88f-406a-b523-82e86c989305",
    "paymentGateway": "VPOS",
    "logo": "https://dev.checkout.pagopa.it/assets/creditcard/visa.png",
    "brand": "VISA",
    "paymentMethodDescription": "Carte di credito o debito"
  },
  "_class": "it.pagopa.ecommerce.commons.documents.v1.TransactionAuthorizationRequestedEvent"
},
{
  "_id": "3a722413-3490-4db7-8633-25013b6133a6",
  "eventCode": "TRANSACTION_AUTHORIZATION_COMPLETED_EVENT",
  "transactionId": "d35da9bdc5554054a64abbec6efa6baa",
  "creationDate": "2023-08-07T15:33:01.395881076Z[Etc/UTC]",
  "data": {
    "authorizationCode": "108811",
    "rrn": "232190000291",
    "timestampOperation": "2023-08-07T15:33:01.316120Z",
    "authorizationResultDto": "OK"
  },
  "_class": "it.pagopa.ecommerce.commons.documents.v1.TransactionAuthorizationCompletedEvent"
},
{
  "_id": "21cac25f-b619-4ff1-a380-c3a71ac05aec",
  "eventCode": "TRANSACTION_CLOSED_EVENT",
  "transactionId": "d35da9bdc5554054a64abbec6efa6baa",
  "creationDate": "2023-08-07T15:33:01.579738223Z[Etc/UTC]",
  "data": {
    "responseOutcome": "OK"
  },
  "_class": "it.pagopa.ecommerce.commons.documents.v1.TransactionClosedEvent"
},
{
  "_id": "700b5831-2f18-44ad-acff-2afdcdf77069",
  "eventCode": "TRANSACTION_USER_RECEIPT_REQUESTED_EVENT",
  "transactionId": "d35da9bdc5554054a64abbec6efa6baa",
  "creationDate": "2023-08-07T15:33:03.393242325Z[Etc/UTC]",
  "data": {
    "responseOutcome": "OK",
    "language": "it-IT",
    "paymentDate": "2023-08-07T17:32:56.341Z",
    "paymentDescription": "Diritti di segreteria - accreditamento attività formative - codice evento: 23o13742"
  },
  "_class": "it.pagopa.ecommerce.commons.documents.v1.TransactionUserReceiptRequestedEvent"
},
{
  "_id": "34f7e285-da8f-498f-b9bf-c7440688f507",
  "eventCode": "TRANSACTION_USER_RECEIPT_ADDED_EVENT",
  "transactionId": "d35da9bdc5554054a64abbec6efa6baa",
  "creationDate": "2023-08-07T15:33:06.987787137Z[Etc/UTC]",
  "data": {
    "responseOutcome": "OK",
    "language": "it-IT",
    "paymentDate": "2023-08-07T17:32:56.341Z",
    "paymentDescription": "Diritti di segreteria - accreditamento attività formative - codice evento: 23o13742"
  },
  "_class": "it.pagopa.ecommerce.commons.documents.v1.TransactionUserReceiptAddedEvent"
},
{
  "_id": "3e798c60-4b57-4368-aeda-5645c57b5d6e",
  "eventCode": "TRANSACTION_ACTIVATED_EVENT",
  "transactionId": "d35da9bdc5554054a64abbec6efa6bab",
  "creationDate": "2023-08-07T15:32:56.592837917Z[Etc/UTC]",
  "data": {
    "email": {
      "data": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
    },
    "paymentNotices": [
      {
        "paymentToken": "1dd0db6d104e4ed3aaf261e8b28e9302",
        "rptId": "97834130581302217169142236141",
        "description": "Diritti di segreteria - accreditamento attività formative - codice evento: 23o13742",
        "amount": 30000,
        "transferList": [
          {
            "paFiscalCode": "97834130581",
            "digitalStamp": false,
            "transferAmount": 30000,
            "transferCategory": "0101104TS"
          }
        ],
        "isAllCCP": false
      }
    ],
    "clientId": "CHECKOUT",
    "paymentTokenValiditySeconds": 900
  },
  "_class": "it.pagopa.ecommerce.commons.documents.v2.TransactionActivatedEvent"
}]);

db.getCollection('transactions-view').insertMany([{
  "_id": "d35da9bdc5554054a64abbec6efa6baa",
  "clientId": "CHECKOUT",
  "email": {
    "data": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
  },
  "status": "NOTIFIED_OK",
  "creationDate": "2023-08-07T15:32:56.686566542Z[Etc/UTC]",
  "paymentNotices": [
    {
      "paymentToken": "1dd0db6d104e4ed3aaf261e8b28e9302",
      "rptId": "97834130581302217169142236141",
      "description": "Diritti di segreteria - accreditamento attività formative - codice evento: 23o13742",
      "amount": 30000,
      "transferList": [
        {
          "paFiscalCode": "97834130581",
          "digitalStamp": false,
          "transferAmount": 30000,
          "transferCategory": "0101104TS"
        }
      ],
      "isAllCCP": false
    }
  ],
  "rrn": "232190000291",
  "paymentGateway": "VPOS",
  "sendPaymentResultOutcome": "OK",
  "authorizationCode": "108811",
  "_class": "it.pagopa.ecommerce.commons.documents.v1.Transaction"
},
{
  "_id": "d35da9bdc5554054a64abbec6efa6bab",
  "clientId": "CHECKOUT",
  "email": {
    "data": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
  },
  "status": "NOTIFIED_OK",
  "creationDate": "2023-08-07T15:32:56.686566542Z[Etc/UTC]",
  "paymentNotices": [
    {
      "paymentToken": "1dd0db6d104e4ed3aaf261e8b28e9302",
      "rptId": "97834130581302217169142236141",
      "description": "Diritti di segreteria - accreditamento attività formative - codice evento: 23o13742",
      "amount": 30000,
      "transferList": [
        {
          "paFiscalCode": "97834130581",
          "digitalStamp": false,
          "transferAmount": 30000,
          "transferCategory": "0101104TS"
        }
      ],
      "isAllCCP": false
    }
  ],
  "rrn": "232190000291",
  "paymentGateway": "VPOS",
  "sendPaymentResultOutcome": "OK",
  "authorizationCode": "108811",
  "_class": "it.pagopa.ecommerce.commons.documents.v2.Transaction",
  "transactionId": "d35da9bdc5554054a64abbec6efa6bab"
}]);
db.getCollection('dead-letter-events').insertMany([{
"_id": "52c23cd0-2545-4815-a649-ca6aeb237bbb",
"queueName": "test",
"insertionDate": "2023-11-30T14:50:27.001Z",
"data": "data"
},{
"_id": "8ce30990-917c-4969-8bab-36fdd5ca95c9",
"queueName": "pagopa-ecommerce-transactions-dead-letter-queue",
"insertionDate": "2023-11-30T14:50:27.001Z",
"data": "ECOMMERCE"
},{
"_id": "351e228e-d2fb-47aa-9645-f750ae98b0ac",
"queueName": "pagopa-ecommerce-notifications-service-errors-queue",
"insertionDate": "2023-11-30T14:50:27.001Z",
"data": "NOTIFICATIONS_SERVICE"
}]);
