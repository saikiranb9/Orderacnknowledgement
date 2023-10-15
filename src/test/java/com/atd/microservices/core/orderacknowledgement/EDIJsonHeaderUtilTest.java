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

import com.atd.microservices.core.orderacknowledgement.service.EDIJsonHeaderUtil;
import com.atd.microservices.core.orderacknowledgement.service.OrderAckConsumerService;
import com.atd.utilities.kafkalogger.operation.KafkaAnalyticsLogger;
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
		"orderacknowledgement.ediconfigUrl=null",
		"orderacknowledgement.vendorRelationshipUrl=null",
		"orderacknowledgement.kafka.topic.csv=null",
		"orderacknowledgement.kafka.topic.xml=null"
})
public class EDIJsonHeaderUtilTest {
	
	@MockBean
	private OrderAckConsumerService ediReaderConsumerService;
   
	@Autowired
	private EDIJsonHeaderUtil ediHeaderUtil;
	
	@MockBean	
	private KafkaTemplate<String, String> ediMapperKafkaTemplate;
	
	@MockBean
	private KafkaAnalyticsLogger serviceKafkaLogger;
	
	@Autowired
	private ObjectMapper objectMapper;

    @Test
    public void testExtractHeaderInfoFromEDIDoc() throws InterruptedException, IOException {
    	Resource resource = new ClassPathResource("edi_processed_data_850.json"); 
    	Object ediMapperData = objectMapper.readValue(resource.getFile(), Object.class);
    	String ediJsonString = objectMapper.writeValueAsString(ediMapperData);
    	
    	Map<String, String> headers = ediHeaderUtil.extractHeaderInfoFromEDIDoc(ediJsonString);
    	Assert.assertEquals(headers.size(), 5);
    	Assert.assertEquals(headers.get(EDIJsonHeaderUtil.KEY_TYPE), "850");
    }

}
