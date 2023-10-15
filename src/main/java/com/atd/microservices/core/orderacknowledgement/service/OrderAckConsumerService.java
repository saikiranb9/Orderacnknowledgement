package com.atd.microservices.core.orderacknowledgement.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.atd.microservices.core.orderacknowledgement.configuration.KafkaConfigConstants;
import com.atd.microservices.core.orderacknowledgement.domain.orderack.IncomingOrderAck;
import com.atd.utilities.kafkalogger.constants.AnalyticsContants;
import com.atd.utilities.kafkalogger.operation.KafkaAnalyticsLogger;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OrderAckConsumerService {

	@Autowired
	private OrderAckProcessor ediProcessor;
	
	@Autowired
	private KafkaAnalyticsLogger serviceKafkaLogger;
	
	@Autowired
	private KafkaConfigConstants kafkaConfigConstants;
	
	@Autowired
	private EDITracerUtil ediTracerUtil;
	
	@Value("${spring.application.name}")
	private String appName;

	@KafkaListener(topics = "${orderacknowledgement.kafka.topic.inbound}", groupId = "group_orderacknowledgement", containerFactory = "kafkaListenerContainerFactory")
	public void analyticsMessageListener(@Payload IncomingOrderAck orderAckPayload) {
		try {
			log.debug("Recieved message: " + orderAckPayload);
			try {
				serviceKafkaLogger.logObject(AnalyticsContants.MessageSourceType.MESSAGE_SOURCE_TYPE_CONSUMER,
						kafkaConfigConstants.KAFKA_TOPIC_INBOUND, appName, orderAckPayload,
						ediTracerUtil.getTraceId(orderAckPayload.getMetadata().getTraceId()), "2xx");
			} catch (Exception e) {
				log.error("Error logging kafka messages to Analytics topic");
			}
			ediProcessor.process(orderAckPayload);			
		} catch (Exception e) {
			log.error("Failed in processing kakfa message", e);
		}
	}
}
