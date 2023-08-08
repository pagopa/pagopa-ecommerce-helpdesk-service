UPDATE PP_PAYPAL_PSP_DETAILS
SET PSP_URL='https://api.prf.platform.pagopa.it/mock-payment-gateway/api/paypalpsp'
WHERE PSP_URL='https://api.prf.platform.pagopa.it/payment-manager/clients/paypal-psp/v1'