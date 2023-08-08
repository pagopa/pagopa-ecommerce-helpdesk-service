DELETE FROM PP_CONFIG WHERE KEY = 'BANCOMAT_PGP_PRIVATE_KEY_PATH';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'BANCOMAT_PGP_PRIVATE_KEY_PATH', '/home/site/appconfig/Bancomat/certificati/private_PGP', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'BASE_HOSTNAME';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'BASE_HOSTNAME', 'https://api.prf.platform.pagopa.it', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'CALLBACK_URL_MOD1';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'CALLBACK_URL_MOD1', 'https://api.prf.platform.pagopa.it/wallet/resume', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'CHECKOUT_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'CHECKOUT_URL', 'https://api.prf.platform.pagopa.it/wallet/checkout?id=', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'CHECKOUT_URL_WEBVIEW';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'CHECKOUT_URL_WEBVIEW', 'https://api.prf.platform.pagopa.it/pp-restapi-CD/v3/webview/checkout?id=', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'COBADGE_PGP_PRIVATE_KEY_PATH';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'COBADGE_PGP_PRIVATE_KEY_PATH', '/home/site/appconfig/Bancomat/certificati/private_PGP', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'COBADGE_PUB_KEY_PATH';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'COBADGE_PUB_KEY_PATH', '/home/site/appconfig/Cobadge/cobadgePubKey.asc', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'PATH_BATCH_BIN_TABLE';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'PATH_BATCH_BIN_TABLE', '/home/site/appconfig/batchSource/', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'PATH_PUSH_CONFIG';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'PATH_PUSH_CONFIG', '/home/site/appconfig/pagopa/userData/appKeys/', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'PAYPAL_ONBOARDING_RETURN_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'PAYPAL_ONBOARDING_RETURN_URL', 'https://api.prf.platform.pagopa.it/pp-restapi-CD/v3/webview/paypal/onboarding/continue/{sessionUuid}', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'TKM_PUB_KEY_PATH';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'TKM_PUB_KEY_PATH', '/home/site/appconfig/Tkm/read-queue-pgp-key-sit_public.asc', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'URL_LOGO_PSP';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'URL_LOGO_PSP', 'https://api.prf.platform.pagopa.it/payment-manager/pp-restapi/${apiVersion}/resources/psp/', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'URL_SERVICE_LOGO_PSP';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'URL_SERVICE_LOGO_PSP', 'https://api.prf.platform.pagopa.it/payment-manager/pp-restapi/${apiVersion}/resources/service/', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'VPOS_3DS_MERCHANT_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'VPOS_3DS_MERCHANT_URL', 'https://api.prf.platform.pagopa.it/wallet/home', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'VPOS_3DS_RETURN_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'VPOS_3DS_RETURN_URL', 'https://api.prf.platform.pagopa.it/wallet/resume', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'VPOS_3DS_RETURN_URL_WEBVIEW';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'VPOS_3DS_RETURN_URL_WEBVIEW', 'https://api.prf.platform.pagopa.it/pp-restapi-CD/v3/webview/checkout/resume', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'VPOS_3DS2_METHOD_RETURN_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'VPOS_3DS2_METHOD_RETURN_URL', 'https://api.prf.platform.pagopa.it/wallet/{id}/resumeMethod3ds2', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'VPOS_3DS2_METHOD_RETURN_URL_WEBVIEW';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'VPOS_3DS2_METHOD_RETURN_URL_WEBVIEW', 'https://api.prf.platform.pagopa.it/pp-restapi-CD/v3/webview/checkout/{id}/resumeMethod3ds2', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'VPOS_3DS2_RETURN_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'VPOS_3DS2_RETURN_URL', 'https://api.prf.platform.pagopa.it/wallet/{id}/resume3ds2', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'VPOS_3DS2_RETURN_URL_WEBVIEW';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'VPOS_3DS2_RETURN_URL_WEBVIEW', 'https://api.prf.platform.pagopa.it/pp-restapi-CD/v3/webview/checkout/{id}/resume3ds2', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'XPAY_RESUME_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'XPAY_RESUME_URL', 'https://api.prf.platform.pagopa.it/wallet/resume/xpay/', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'XPAY_RESUME_URL_WEBVIEW';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'XPAY_RESUME_URL_WEBVIEW', 'https://api.prf.platform.pagopa.it/pp-restapi-CD/v3/webview/checkout/resume/xpay/', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'XPAY_RESUME_VERIFICATION_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'XPAY_RESUME_VERIFICATION_URL', 'https://api.prf.platform.pagopa.it/wallet/resume/xpayVerification/', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'XPAY_RESUME_VERIFICATION_URL_WEBVIEW';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'XPAY_RESUME_VERIFICATION_URL_WEBVIEW', 'https://api.prf.platform.pagopa.it/pp-restapi-CD/v3/webview/checkout/resume/xpayVerification/', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'BANCOMAT_PANS_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'BANCOMAT_PANS_URL', 'https://api.prf.platform.pagopa.it/pmmockserviceapi/bancomat/pagopa/pan', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'COBADGE_PANS_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'COBADGE_PANS_URL', 'https://api.prf.platform.pagopa.it/payment-manager/clients/cobadge/v4/utils/payment-instruments/search', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'COBADGE_SEARCH_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'COBADGE_SEARCH_URL', 'https://api.prf.platform.pagopa.it/payment-manager/clients/cobadge/v4/utils/payment-instruments/{searchRequestId}', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'SATISPAY_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'SATISPAY_URL', 'https://api.prf.platform.pagopa.it/payment-manager/clients/satispay/v1/consumers', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'VPOS_3DS2_METHOD_RETURN_URL_IO_PAY';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'VPOS_3DS2_METHOD_RETURN_URL_IO_PAY', 'https://api.prf.platform.pagopa.it/api/checkout/payment-transactions/v1/transactions/{id}/method', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'VPOS_3DS2_RETURN_URL_IO_PAY';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'VPOS_3DS2_RETURN_URL_IO_PAY', 'https://api.prf.platform.pagopa.it/api/checkout/payment-transactions/v1/transactions/{id}/challenge', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'XPAY_RESUME_URL_IO_PAY';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'XPAY_RESUME_URL_IO_PAY', 'https://api.prf.platform.pagopa.it/api/checkout/payment-transactions/v1/transactions/xpay/', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'XPAY_RESUME_VERIFICATION_URL_IO_PAY';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'XPAY_RESUME_VERIFICATION_URL_IO_PAY', 'https://api.prf.platform.pagopa.it/api/checkout/payment-transactions/v1/transactions/xpay/verification/', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'BATCH_BB_WITH_SP';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'BATCH_BB_WITH_SP', 'N', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'BPD_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'BPD_URL', 'https://api.prf.platform.pagopa.it/pmmockserviceapi/bpd/pm/payment-instrument', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'BUYER_BANKS_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'BUYER_BANKS_URL', 'https://api.prf.platform.pagopa.it/payment-manager/buyerbanks/v1/banks', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'S4S_BASE_PATH';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'S4S_BASE_PATH', 'https://api.prf.platform.pagopa.it/pmmockserviceapi/S4S-PM/PmService', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'VPOS_3DS2_ENDPOINT';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'VPOS_3DS2_ENDPOINT', 'https://api.prf.platform.pagopa.it/pmmockserviceapi/vpos/authorize3dsV2', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'VPOS_ENDPOINT';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'VPOS_ENDPOINT', 'https://api.prf.platform.pagopa.it/pmmockserviceapi/vpos/authorize', SYSDATE);

UPDATE PP_PAYPAL_PSP_DETAILS
SET PSP_URL = 'https://api.prf.platform.pagopa.it/payment-manager/clients/paypal-psp/v1';
