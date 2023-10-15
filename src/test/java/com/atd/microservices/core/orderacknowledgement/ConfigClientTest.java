package com.atd.microservices.core.orderacknowledgement;

import java.io.IOException;
import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.atd.microservices.core.orderacknowledgement.domain.orderack.IncomingOrderAck;
import com.atd.microservices.core.orderacknowledgement.service.EDIJsonHeaderUtil;
import com.atd.microservices.core.orderacknowledgement.service.OrderAckConsumerService;
import com.atd.microservices.core.orderacknowledgement.service.OrderAckProcessor;
import com.atd.microservices.core.orderacknowledgement.webclients.EDIConfigClient;
import com.atd.utilities.kafkalogger.operation.KafkaAnalyticsLogger;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = {
		"kafka.bootstrap.server.url=null",
		"kafka.security.protocol=null",
		"orderacknowledgement.kafka.topic.inbound=TEST_TOPIC",
		"orderacknowledgement.kafka.topic.outbound=TEST_TOPICS",
		"ssl.truststore.password=null",
		"ssl.truststore.location=null",
		"kafka.analytic.topic=null",
		"orderacknowledgement.ediAnalyticsDataUrl=null",
		"orderacknowledgement.ediAnalyticsDataFindByTraceIdAndTypeUrl=null",
		"orderacknowledgement.ediconfigUrl=https://develop-edi.gcp.atd-us.com/ediconfig/customer/{senderCode}/{receiverCode}",
		"orderacknowledgement.vendorRelationshipUrl=null",
		"orderacknowledgement.kafka.topic.csv=null",
		"orderacknowledgement.kafka.topic.xml=null"})
public class ConfigClientTest {
	
	@MockBean
	private OrderAckConsumerService ediReaderConsumerService;
   
	@Autowired
	private EDIConfigClient configClient;
	
	@MockBean	
	private KafkaTemplate<String, String> ediMapperKafkaTemplate;
	
	@MockBean
	private KafkaAnalyticsLogger serviceKafkaLogger;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	@Autowired
	private OrderAckProcessor orderAckProcessor;

    @Test
    public void testConfigClient() throws InterruptedException, IOException {    	
    	Map<String, String> map = configClient.findBySenderCodeAndReceiverCode("01-007914401", "12-7047353003").block();
    	log.info("{}", map);
    }
    
    @Test
    public void testProcessor() throws JsonParseException, JsonMappingException, IOException {
    	Resource resource = new ClassPathResource("orderack.json"); 
    	IncomingOrderAck incomingOrderAck = objectMapper.readValue(resource.getFile(), IncomingOrderAck.class);
    	log.info("{}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(incomingOrderAck));
    	orderAckProcessor.process(incomingOrderAck);
    }

}
