conn = new Mongo();
db = conn.getDB("history");

db.getCollection('pm-transactions-history').insertMany([{
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
                "paFiscalCode": "77777777777"
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
                "paFiscalCode": "77777777777"
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