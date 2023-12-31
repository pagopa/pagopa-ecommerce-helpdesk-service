DELETE FROM PP_CONFIG WHERE KEY = 'BANCOMAT_PGP_PRIVATE_KEY_PATH';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'BANCOMAT_PGP_PRIVATE_KEY_PATH', '/home/site/appconfig/Bancomat/certificati/private_PGP', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'BASE_HOSTNAME';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'BASE_HOSTNAME', 'https://api.uat.platform.pagopa.it/payment-manager', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'CALLBACK_URL_MOD1';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'CALLBACK_URL_MOD1', 'https://api.uat.platform.pagopa.it/wallet/resume', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'CHECKOUT_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'CHECKOUT_URL', 'https://api.uat.platform.pagopa.it/wallet/checkout?id=', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'CHECKOUT_URL_WEBVIEW';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'CHECKOUT_URL_WEBVIEW', 'https://api.uat.platform.pagopa.it/payment-manager/pp-restapi-CD/v3/webview/checkout?id=', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'COBADGE_PGP_PRIVATE_KEY_PATH';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'COBADGE_PGP_PRIVATE_KEY_PATH', '/home/site/appconfig/Fabrick/certificati/Fabrick-BS-PM-UAT_priv.pgp', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'COBADGE_PUB_KEY_PATH';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'COBADGE_PUB_KEY_PATH', '/home/site/appconfig/Fabrick/certificati/pgp-pagopa-fabrick-sandbox.pgp', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'PATH_BATCH_BIN_TABLE';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'PATH_BATCH_BIN_TABLE', '/home/site/appconfig/batchSource/', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'PATH_PUSH_CONFIG';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'PATH_PUSH_CONFIG', '/home/site/appconfig/pagopa/userData/appKeys/', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'PAYPAL_ONBOARDING_RETURN_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'PAYPAL_ONBOARDING_RETURN_URL', 'https://api.uat.platform.pagopa.it/payment-manager/pp-restapi-CD/v3/webview/paypal/onboarding/continue/{sessionUuid}', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'URL_LOGO_PSP';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'URL_LOGO_PSP', 'https://api.uat.platform.pagopa.it/payment-manager/pp-restapi/${apiVersion}/resources/psp/', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'URL_SERVICE_LOGO_PSP';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'URL_SERVICE_LOGO_PSP', 'https://api.uat.platform.pagopa.it/payment-manager/pp-restapi/${apiVersion}/resources/service/', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'VPOS_3DS_MERCHANT_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'VPOS_3DS_MERCHANT_URL', 'https://api.uat.platform.pagopa.it/wallet/home', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'VPOS_3DS_RETURN_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'VPOS_3DS_RETURN_URL', 'https://api.uat.platform.pagopa.it/wallet/resume', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'VPOS_3DS_RETURN_URL_WEBVIEW';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'VPOS_3DS_RETURN_URL_WEBVIEW', 'https://api.uat.platform.pagopa.it/payment-manager/pp-restapi-CD/v3/webview/checkout/resume', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'VPOS_3DS2_METHOD_RETURN_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'VPOS_3DS2_METHOD_RETURN_URL', 'https://api.uat.platform.pagopa.it/wallet/{id}/resumeMethod3ds2', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'VPOS_3DS2_METHOD_RETURN_URL_WEBVIEW';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'VPOS_3DS2_METHOD_RETURN_URL_WEBVIEW', 'https://api.uat.platform.pagopa.it/payment-manager/pp-restapi-CD/v3/webview/checkout/{id}/resumeMethod3ds2', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'VPOS_3DS2_RETURN_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'VPOS_3DS2_RETURN_URL', 'https://api.uat.platform.pagopa.it/wallet/{id}/resume3ds2', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'VPOS_3DS2_RETURN_URL_WEBVIEW';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'VPOS_3DS2_RETURN_URL_WEBVIEW', 'https://api.uat.platform.pagopa.it/payment-manager/pp-restapi-CD/v3/webview/checkout/{id}/resume3ds2', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'XPAY_RESUME_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'XPAY_RESUME_URL', 'https://api.uat.platform.pagopa.it/wallet/resume/xpay/', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'XPAY_RESUME_URL_WEBVIEW';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'XPAY_RESUME_URL_WEBVIEW', 'https://api.uat.platform.pagopa.it/payment-manager/pp-restapi-CD/v3/webview/checkout/resume/xpay/', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'XPAY_RESUME_VERIFICATION_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'XPAY_RESUME_VERIFICATION_URL', 'https://api.uat.platform.pagopa.it/wallet/resume/xpayVerification/', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'XPAY_RESUME_VERIFICATION_URL_WEBVIEW';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'XPAY_RESUME_VERIFICATION_URL_WEBVIEW', 'https://api.uat.platform.pagopa.it/payment-manager/pp-restapi-CD/v3/webview/checkout/resume/xpayVerification/', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'BASE_HOSTNAME';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'BASE_HOSTNAME', 'https://api.uat.platform.pagopa.it', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'CHECKOUT_URL_WEBVIEW';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'CHECKOUT_URL_WEBVIEW', 'https://api.uat.platform.pagopa.it/pp-restapi-CD/v3/webview/checkout?id=', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'PAYPAL_ONBOARDING_RETURN_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'PAYPAL_ONBOARDING_RETURN_URL', 'https://api.uat.platform.pagopa.it/pp-restapi-CD/v3/webview/paypal/onboarding/continue/{sessionUuid}', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'VPOS_3DS_RETURN_URL_WEBVIEW';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'VPOS_3DS_RETURN_URL_WEBVIEW', 'https://api.uat.platform.pagopa.it/pp-restapi-CD/v3/webview/checkout/resume', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'VPOS_3DS2_METHOD_RETURN_URL_WEBVIEW';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'VPOS_3DS2_METHOD_RETURN_URL_WEBVIEW', 'https://api.uat.platform.pagopa.it/pp-restapi-CD/v3/webview/checkout/{id}/resumeMethod3ds2', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'VPOS_3DS2_RETURN_URL_WEBVIEW';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'VPOS_3DS2_RETURN_URL_WEBVIEW', 'https://api.uat.platform.pagopa.it/pp-restapi-CD/v3/webview/checkout/{id}/resume3ds2', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'XPAY_RESUME_URL_WEBVIEW';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'XPAY_RESUME_URL_WEBVIEW', 'https://api.uat.platform.pagopa.it/pp-restapi-CD/v3/webview/checkout/resume/xpay/', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'XPAY_RESUME_VERIFICATION_URL_WEBVIEW';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'XPAY_RESUME_VERIFICATION_URL_WEBVIEW', 'https://api.uat.platform.pagopa.it/pp-restapi-CD/v3/webview/checkout/resume/xpayVerification/', SYSDATE);
DELETE FROM PP_CONFIG WHERE "KEY" = 'S4S_BASE_PATH';
INSERT INTO PP_CONFIG (ID_CONFIG, "KEY", CREATION_DATE, "VALUE") VALUES (seq_config.NEXTVAL, 'S4S_BASE_PATH', SYSDATE, 'http://s4snode.onprem.local:8240/S4S-PM/PMRestService');

UPDATE PP_PAYPAL_PSP_DETAILS
SET PSP_URL = 'https://api.uat.platform.pagopa.it/payment-manager/clients/paypal-psp/v1';