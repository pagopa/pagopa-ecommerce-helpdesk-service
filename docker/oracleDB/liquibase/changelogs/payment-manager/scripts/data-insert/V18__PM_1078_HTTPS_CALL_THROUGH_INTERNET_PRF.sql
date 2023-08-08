DELETE FROM PP_CONFIG WHERE KEY = 'COBADGE_PANS_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'COBADGE_PANS_URL', 'https://bankingservices-sandbox.pagopa.it/api/pagopa/banking/v4.0/utils/payment-instruments/search', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'COBADGE_SEARCH_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'COBADGE_SEARCH_URL', 'https://bankingservices-sandbox.pagopa.it/api/pagopa/banking/v4.0/utils/payment-instruments/{searchRequestId}', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'SATISPAY_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'SATISPAY_URL', 'https://staging.authservices.satispay.com/g_provider/v1/consumers', SYSDATE);

UPDATE PP_PAYPAL_PSP_DETAILS
SET PSP_URL = 'https://api.prf.platform.pagopa.it/mock-payment-gateway/api/paypalpsp';