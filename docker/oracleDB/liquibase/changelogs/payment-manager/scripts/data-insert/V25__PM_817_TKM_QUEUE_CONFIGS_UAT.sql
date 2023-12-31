UPDATE PP_CONFIG SET VALUE = '${kafkaAppenderBootstrapServers}' WHERE "KEY" = 'SERVER_KAFKA_AZURE';
UPDATE PP_CONFIG SET VALUE = 'SASL_SSL' WHERE "KEY" = 'SECURITY_PROTOCOL_AZURE';
UPDATE PP_CONFIG SET VALUE = 'PLAIN' WHERE "KEY" = 'SASL_MECHANISM_AZURE';
UPDATE PP_CONFIG SET VALUE = '${tkmReadTokenParPanProducerSaslJaasConfig}' WHERE "KEY" = 'JAAS_CONFIG_READ_QUEUE';
UPDATE PP_CONFIG SET VALUE = '${tkmDeleteCardProducerSaslJaasConfig}' WHERE "KEY" = 'JAAS_CONFIG_DELETE_QUEUE';
UPDATE PP_CONFIG SET VALUE = '${kafkaReadQueueTopic}' WHERE "KEY" = 'READ_QUEUE_TOPIC';
UPDATE PP_CONFIG SET VALUE = '${kafkaDeleteQueueTopic}' WHERE "KEY" = 'DELETE_QUEUE_TOPIC';