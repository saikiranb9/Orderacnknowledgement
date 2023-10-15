package com.atd.microservices.core.orderacknowledgement.domain;

import java.util.Map;

import com.atd.microservices.core.orderacknowledgement.domain.orderack.OrderAckData;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true, value = {"GENERATE855_FIELD","GENERATE855CSV_FIELD","GENERATE855XML_FIELD"})
public class OutgoingAckPayload {
	@JsonIgnore
	public static String GENERATE855_FIELD = "generate855";
	public static String GENERATE855CSV_FIELD = "generate855csv";
	public static String GENERATE855XML_FIELD = "generate855xml";
	private OrderAckData ack855;
	private Object po850;
	private Map<String, String> config;
}
