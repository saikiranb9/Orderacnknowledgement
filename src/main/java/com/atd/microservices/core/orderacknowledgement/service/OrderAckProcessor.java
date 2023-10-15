package com.atd.microservices.core.orderacknowledgement.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.atd.microservices.core.orderacknowledgement.configuration.KafkaConfigConstants;
import com.atd.microservices.core.orderacknowledgement.domain.EDIData;
import com.atd.microservices.core.orderacknowledgement.domain.EDIMapperPayload;
import com.atd.microservices.core.orderacknowledgement.domain.EdiConfig;
import com.atd.microservices.core.orderacknowledgement.domain.OutgoingAckPayload;
import com.atd.microservices.core.orderacknowledgement.domain.VendorRelationship;
import com.atd.microservices.core.orderacknowledgement.domain.orderack.CompleteCsvAck;
import com.atd.microservices.core.orderacknowledgement.domain.orderack.CsvAck;
import com.atd.microservices.core.orderacknowledgement.domain.orderack.IncomingOrderAck;
import com.atd.microservices.core.orderacknowledgement.exception.OrderAckException;
import com.atd.microservices.core.orderacknowledgement.webclients.EDIAnalyticsDataClient;
import com.atd.microservices.core.orderacknowledgement.webclients.EDIConfigClient;
import com.atd.microservices.core.orderacknowledgement.webclients.VendorRelationshipClient;
import com.atd.utilities.kafkalogger.constants.AnalyticsContants;
import com.atd.utilities.kafkalogger.operation.KafkaAnalyticsLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class OrderAckProcessor {

	@Autowired
	private KafkaConfigConstants kafkaConfigConstants;

	@Autowired
	@Qualifier("ediMapperKafkaTemplate")
	private KafkaTemplate<String, EDIMapperPayload> ediMapperKafkaTemplate;
	
	@Autowired
	@Qualifier("csvKafkaTemplate")
	private KafkaTemplate<String, CompleteCsvAck> csvKafkaTemplate;

	@Autowired
	private KafkaAnalyticsLogger serviceKafkaLogger;

	@Autowired
	private EDIAnalyticsDataClient ediAnalyticsDataClient;

	@Autowired
	private EDIConfigClient configClient;

	@Value("${spring.application.name}")
	private String appName;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private EDITracerUtil ediTracerUtil;
	
	@Autowired
	VendorRelationshipClient vendorRelationshipClient;

	public void process(IncomingOrderAck orderAckPayload) {
		EDIData ediData = null;
		EDIMapperPayload ediMapperPayload = null;
		CompleteCsvAck completeCsvAck = null;
		// Fetch Config
		Map<String, String> config = configClient.findBySenderCodeAndReceiverCode(
				orderAckPayload.getMetadata().getSenderCode(), orderAckPayload.getMetadata().getReceiverCode()).block();
		if (StringUtils.equalsIgnoreCase(config.get(OutgoingAckPayload.GENERATE855_FIELD), "true")) {
			try {
				// Fetch EDIAnalyticsData
				ediData = ediAnalyticsDataClient.findByTraceIdAndType(
						ediTracerUtil.getTraceId(orderAckPayload.getMetadata().getTraceId()), "850").block();
				if (ediData == null) {
					throw new OrderAckException(
							"Could not found EDIAnalyticsData by incoming traceId and document type");
				}
				OutgoingAckPayload outgoingAckPayload = new OutgoingAckPayload();
				outgoingAckPayload.setAck855(orderAckPayload.getData());
				outgoingAckPayload.setPo850(ediData.getProcessedData());
				outgoingAckPayload.setConfig(config);
				ediData.setUuid(config.get(EdiConfig.UUID));
				ediData.setCustomerPO(orderAckPayload.getData().getCustomerPO());
				// Save to EDIAnalyticsData Service
				createEDIAnalyticsDataSuccess(ediData);
	
				if(StringUtils.equalsIgnoreCase(config.get(OutgoingAckPayload.GENERATE855CSV_FIELD), "true")) {
					completeCsvAck = createExtFileAckPayload(orderAckPayload);
					csvKafkaTemplate.send(kafkaConfigConstants.KAFKA_TOPIC_CSV, completeCsvAck);
				} else if(StringUtils.equalsIgnoreCase(config.get(OutgoingAckPayload.GENERATE855XML_FIELD), "true")) {
					completeCsvAck = createExtFileAckPayload(orderAckPayload); 
					csvKafkaTemplate.send(kafkaConfigConstants.KAFKA_TOPIC_CSV, completeCsvAck);
				} else {
					// Push to EDIMAPPER Kafka Topic			
					ediMapperPayload = createEDIMapperPayload(ediData, outgoingAckPayload,
							orderAckPayload.getData().getCustomerPO());
					ediMapperKafkaTemplate.send(kafkaConfigConstants.KAFKA_TOPIC_OUTBOUND, ediMapperPayload);
				}
				
			} catch (Exception e) {
				log.error("Order Ack processing Error: {}", e);
				try {
					createEDIAnalyticsDataError(config.get(EdiConfig.UUID), ediData,
							ediTracerUtil.getTraceId(orderAckPayload.getMetadata().getTraceId()), e);
				} catch (Exception e2) {
					log.error("Error saving the Error EDI data to EDIANALYTICS DB", e2);
				}
			}

			// Push to Analytic Topic (Even if there's error)
			if (ediMapperPayload != null) {
				try {
					serviceKafkaLogger.logObject(AnalyticsContants.MessageSourceType.MESSAGE_SOURCE_TYPE_PRODUCER,
							appName, kafkaConfigConstants.KAFKA_TOPIC_OUTBOUND, ediMapperPayload,
							ediTracerUtil.getTraceId(orderAckPayload.getMetadata().getTraceId()), "2xx");
				} catch (Exception e) {
					log.error("Error logging kafka messages to Analytics topic");
				}
			}
		}
	}

	private void createEDIAnalyticsDataSuccess(EDIData ediAnalyticsData) throws Exception {
		ediAnalyticsData.setLastProcessStage(appName);
		ediAnalyticsData.setStatus("2xx");
		ediAnalyticsData.setType("855");
		ediAnalyticsData.setSourceTopic(kafkaConfigConstants.KAFKA_TOPIC_INBOUND);
		ediAnalyticsDataClient.saveEDIData(Mono.just(ediAnalyticsData)).block();
	}

	private void createEDIAnalyticsDataError(String uuid, EDIData ediAnalyticsData, String traceId, Exception e)
			throws Exception {
		if (ediAnalyticsData == null) {
			ediAnalyticsData = new EDIData();
			ediAnalyticsData.setTraceId(traceId);
			ediAnalyticsData.setUuid(uuid);
		}
		ediAnalyticsData.setLastProcessStage(appName);
		ediAnalyticsData.setStatus("5xx");
		ediAnalyticsData.setType("855");
		ediAnalyticsData.setSourceTopic(kafkaConfigConstants.KAFKA_TOPIC_INBOUND);
		ediAnalyticsData
				.setErrorMessage(e.getMessage() + (e.getCause() != null ? e.getCause().getLocalizedMessage() : ""));
		ediAnalyticsDataClient.saveEDIData(Mono.just(ediAnalyticsData)).block();
	}

	private EDIMapperPayload createEDIMapperPayload(EDIData ediData, OutgoingAckPayload outgoingAckPayload,
			String customerPO) throws JsonProcessingException {
		EDIMapperPayload ediMapperPayload = new EDIMapperPayload();
		ediMapperPayload.setSendercode(ediData.getReceivercode());
		ediMapperPayload.setReceivercode(ediData.getSendercode());
		ediMapperPayload.setStandard(ediData.getStandard());
		ediMapperPayload.setType(ediData.getType());
		ediMapperPayload.setVersion(ediData.getVersion());
		// Stringify
		ediMapperPayload.setData(objectMapper.writeValueAsString(outgoingAckPayload));
		ediMapperPayload.setCustomerPO(customerPO);
		return ediMapperPayload;
	}
	
	private CompleteCsvAck createExtFileAckPayload(IncomingOrderAck orderAckPayload) {
		CompleteCsvAck completeCsvAck = new CompleteCsvAck();
		List<CsvAck> lines = new ArrayList<>();
		orderAckPayload.getData().getProducts().stream().forEach(product -> {
			CsvAck csvAck = new CsvAck();
			try {
				VendorRelationship vendorRelationship = vendorRelationshipClient
						.getSupplierInfoByVendorName(product.getUnknownFields().get("vendorName")).block();
				if(null != vendorRelationship.getVendorName()) {	
					csvAck.setVendorNumber(vendorRelationship.getVendorNumber());
				} else {
					csvAck.setVendorNumber("Not Found");
				}
			} catch(RuntimeException e) {
				log.error("No Supplier Info Found for Vendor Name : {}", product.getUnknownFields().get("vendorName"));
				csvAck.setVendorNumber("Not Found");
			}
			csvAck.setVendorName(product.getUnknownFields().get("vendorName").toString());
			csvAck.setPoNumber(orderAckPayload.getData().getCustomerPO());
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu/MM/dd");
			LocalDate localDate = LocalDate.now();
			String date = formatter.format(localDate);
			csvAck.setDate(date);
			csvAck.setProductNumber(product.getId());
			csvAck.setRequestedQuantity(product.getTotalReqQty().toString());
			csvAck.setFilledQuantity(product.getFilledQty().toString());
			if( null != product.getUnknownFields().get("cost") ) {
				csvAck.setCost(product.getUnknownFields().get("cost"));
			} else {
				csvAck.setCost("0.0");
			}
			lines.add(csvAck);
		});
		completeCsvAck.setLines(lines);
		return completeCsvAck;
	}
}
