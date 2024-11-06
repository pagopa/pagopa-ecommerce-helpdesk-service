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
    "_id": "91b5279d-8679-4521-ae91-b93e80e724b5",
    "transactionId": "cb2db0aabfc047ff826c96aa4323b548",
    "creationDate": "2024-05-09T09:47:24.404523565Z[Etc/UTC]",
    "data": {
      "email": {
        "data": "30a0014a-2251-448c-862b-8bcbf9241fd8"
      },
      "paymentNotices": [
        {
          "paymentToken": "a28b58bb97ec45948e173b38c9a00ae5",
          "rptId": "77777777777302000611488182800",
          "description": "TARI/TEFA 2021",
          "amount": 12000,
          "transferList": [
            {
              "paFiscalCode": "77777777777",
              "digitalStamp": false,
              "transferAmount": 10000,
              "transferCategory": "0101101IM"
            },
            {
              "paFiscalCode": "01199250158",
              "digitalStamp": false,
              "transferAmount": 2000,
              "transferCategory": "0201102IM"
            }
          ],
          "isAllCCP": true,
          "companyName": "company PA"
        }
      ],
      "clientId": "CHECKOUT",
      "paymentTokenValiditySeconds": 900,
      "transactionGatewayActivationData": {
        "orderId": "E1715248039581_TYe",
        "correlationId": "04c95e86-e3be-49f7-adb5-dd821f3810e8",
        "_class": "it.pagopa.ecommerce.commons.documents.v2.activation.NpgTransactionGatewayActivationData"
      }
    },
    "eventCode": "TRANSACTION_ACTIVATED_EVENT",
    "_class": "it.pagopa.ecommerce.commons.documents.v2.TransactionActivatedEvent"
  },
  {
    "_id": "b27efa9d-5e85-42af-b95a-b0e5fdd958dd",
    "transactionId": "cb2db0aabfc047ff826c96aa4323b548",
    "creationDate": "2024-05-09T09:47:28.417499875Z[Etc/UTC]",
    "data": {
      "amount": 12000,
      "fee": 150,
      "paymentInstrumentId": "e7058cac-5e1a-4002-8994-5bab31e9f385",
      "pspId": "BPPIITRRXXX",
      "paymentTypeCode": "CP",
      "brokerName": "97103880585",
      "pspChannelCode": "97103880585_07",
      "paymentMethodName": "CARDS",
      "pspBusinessName": "Poste Italiane",
      "isPspOnUs": false,
      "authorizationRequestId": "E1715248039581_TYe",
      "paymentGateway": "NPG",
      "paymentMethodDescription": "Carte di Credito e Debito",
      "transactionGatewayAuthorizationRequestedData": {
        "logo": "https://assets.cdn.platform.pagopa.it/creditcard/mastercard.png",
        "brand": "MC",
        "sessionId": "16f66656-9738-44c1-af0c-a332856a1182",
        "confirmPaymentSessionId": "5ffdfd28-5022-42ec-8ce5-2d38d5f0ff29",
        "_class": "it.pagopa.ecommerce.commons.documents.v2.authorization.NpgTransactionGatewayAuthorizationRequestedData"
      }
    },
    "eventCode": "TRANSACTION_AUTHORIZATION_REQUESTED_EVENT",
    "_class": "it.pagopa.ecommerce.commons.documents.v2.TransactionAuthorizationRequestedEvent"
  },
  {
    "_id": "44b1dc3a-0225-45e5-af50-8a3609f51871",
    "transactionId": "cb2db0aabfc047ff826c96aa4323b548",
    "creationDate": "2024-05-09T09:47:50.036610378Z[Etc/UTC]",
    "data": {
      "authorizationCode": "991878",
      "rrn": "241309005605",
      "timestampOperation": "2024-05-09T09:47:48.785Z",
      "transactionGatewayAuthorizationData": {
        "operationResult": "EXECUTED",
        "operationId": "919182225234541309",
        "paymentEndToEndId": "919182225234541309",
        "errorCode": "000",
        "_class": "it.pagopa.ecommerce.commons.documents.v2.authorization.NpgTransactionGatewayAuthorizationData"
      }
    },
    "eventCode": "TRANSACTION_AUTHORIZATION_COMPLETED_EVENT",
    "_class": "it.pagopa.ecommerce.commons.documents.v2.TransactionAuthorizationCompletedEvent"
  },
  {
    "_id": "8d32fb2e-a1d8-4b91-a2ea-806f909d9985",
    "transactionId": "cb2db0aabfc047ff826c96aa4323b548",
    "creationDate": "2024-05-09T09:47:50.314949590Z[Etc/UTC]",
    "eventCode": "TRANSACTION_CLOSURE_REQUESTED_EVENT",
    "_class": "it.pagopa.ecommerce.commons.documents.v2.TransactionClosureRequestedEvent"
  },
  {
    "_id": "72dec6ba-342e-41ea-8c18-937ea5c6403d",
    "transactionId": "cb2db0aabfc047ff826c96aa4323b548",
    "creationDate": "2024-05-09T09:47:51.713772207Z[Etc/UTC]",
    "data": {
      "responseOutcome": "OK"
    },
    "eventCode": "TRANSACTION_CLOSED_EVENT",
    "_class": "it.pagopa.ecommerce.commons.documents.v2.TransactionClosedEvent"
  },
  {
    "_id": "7a6333a4-03ff-42d1-a3ad-dffd639f3e5d",
    "transactionId": "cb2db0aabfc047ff826c96aa4323b548",
    "creationDate": "2024-05-09T09:47:55.228454139Z[Etc/UTC]",
    "data": {
      "responseOutcome": "OK",
      "language": "it-IT",
      "paymentDate": "2024-05-09T11:47:24.289Z"
    },
    "eventCode": "TRANSACTION_USER_RECEIPT_REQUESTED_EVENT",
    "_class": "it.pagopa.ecommerce.commons.documents.v2.TransactionUserReceiptRequestedEvent"
  },
  {
    "_id": "7fda0510-c22c-4780-b672-12c282bcb4f0",
    "transactionId": "cb2db0aabfc047ff826c96aa4323b548",
    "creationDate": "2024-05-09T09:47:56.621417461Z[Etc/UTC]",
    "data": {
      "responseOutcome": "OK",
      "language": "it-IT",
      "paymentDate": "2024-05-09T11:47:24.289Z"
    },
    "eventCode": "TRANSACTION_USER_RECEIPT_ADDED_EVENT",
    "_class": "it.pagopa.ecommerce.commons.documents.v2.TransactionUserReceiptAddedEvent"
  }
]);

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
    "_id": "cb2db0aabfc047ff826c96aa4323b548",
    "clientId": "CHECKOUT",
    "email": {
      "data": "30a0014a-2251-448c-862b-8bcbf9241fd8"
    },
    "status": "NOTIFIED_OK",
    "creationDate": "2024-05-09T09:47:24.901858230Z[Etc/UTC]",
    "paymentNotices": [
      {
        "paymentToken": "a28b58bb97ec45948e173b38c9a00ae5",
        "rptId": "77777777777302000611488182800",
        "description": "TARI/TEFA 2021",
        "amount": 12000,
        "transferList": [
          {
            "paFiscalCode": "77777777777",
            "digitalStamp": false,
            "transferAmount": 10000,
            "transferCategory": "0101101IM"
          },
          {
            "paFiscalCode": "01199250158",
            "digitalStamp": false,
            "transferAmount": 2000,
            "transferCategory": "0201102IM"
          }
        ],
        "isAllCCP": true,
        "companyName": "company PA"
      }
    ],
    "rrn": "241309005605",
    "paymentGateway": "NPG",
    "sendPaymentResultOutcome": "OK",
    "authorizationCode": "991878",
    "authorizationErrorCode": "000",
    "gatewayAuthorizationStatus": "EXECUTED",
    "_class": "it.pagopa.ecommerce.commons.documents.v2.Transaction"
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

db2 = conn.getDB("ecommerce-history");

db2.getCollection('pm-transactions-view').insertMany([{
    "userInfo": {
        "userFiscalCode": "RSSMRA80A01H501U",
        "notificationEmail": "mario.rossi@example.com",
        "authenticationType": 1
    },
    "transactionInfo": {
        "creationDate": "2024-05-10T11:14:32.556+02:00",
        "status": 1,
        "statusDetails": 1,
        "amount": 30000,
        "fee": 100,
        "grandTotal": 30100,
        "rrn": "241310000102",
        "authorizationCode": "00",
        "paymentMethodName": "Pagamento con carte"
    },
    "paymentInfo": {
        "origin": "CITTADINANZA_DIGITALE",
        "details": [
            {
                "subject": "TARI 2021",
                "iuv": "02055446688775544",
                "idTransaction": "feae3da1480841a6ba11520cbd36090b",
                "creditorInstitution": "EC_TE",
                "paFiscalCode": "77777777777",
                "amount": 30000
            }
        ]
    },
    "pspInfo": {
        "pspId": "UNCRITMM",
        "businessName": "UniCredit S.p.A",
        "idChannel": "00348170101_01_ONUS"
    },
    "product": "PM"
},
{
    "userInfo": {
        "userFiscalCode": "NVEGVN80A01H501U",
        "notificationEmail": "giovanni.neve@test.com",
        "authenticationType": 1
    },
    "transactionInfo": {
        "creationDate": "2024-06-10T11:14:32.556+02:00",
        "status": 1,
        "statusDetails": 1,
        "amount": 45000,
        "fee": 150,
        "grandTotal": 45150,
        "rrn": "241310000103",
        "authorizationCode": "01",
        "paymentMethodName": "Pagamento con carte"
    },
    "paymentInfo": {
        "origin": "CITTADINANZA_DIGITALE",
        "details": [
            {
                "subject": "TARI 2022",
                "iuv": "02055446688775545",
                "idTransaction": "feae3da1480841a6ba11520cbd36091c",
                "creditorInstitution": "EC_TE",
                "paFiscalCode": "77777777777",
                "amount": 45000
            }
        ]
    },
    "pspInfo": {
        "pspId": "UNCRITMM",
        "businessName": "UniCredit S.p.A",
        "idChannel": "00348170101_01_ONUS"
    },
    "product": "PM"
}]);