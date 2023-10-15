package com.atd.microservices.core.orderacknowledgement.webclients;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.atd.microservices.core.orderacknowledgement.domain.EDIData;
import com.atd.microservices.core.orderacknowledgement.exception.OrderAckException;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class EDIAnalyticsDataClient {
	
	@Autowired
	private WebClient webClient;
	
	@Value("${spring.application.name}")
	private String applicationName;	
	
	@Value("${orderacknowledgement.ediAnalyticsDataUrl}")
	private String ediAnalyticsDataUrl;
	
	@Value("${orderacknowledgement.ediAnalyticsDataFindByTraceIdAndTypeUrl}")
	private String ediAnalyticsDataFindByTraceIdAndTypeUrl;

	public Mono<EDIData> saveEDIData(Mono<EDIData> ediData) {
		try { 
			return webClient.put()
				.uri(ediAnalyticsDataUrl)
				.header("XATOM-CLIENTID", applicationName)
				.body(ediData, EDIData.class)
				.retrieve()
				.onStatus(HttpStatus::isError,
						exceptionFunction -> Mono
								.error(new OrderAckException("EDIAnalyticsData Service's Save API returned Error:"
										+ exceptionFunction.rawStatusCode())))
				.bodyToMono(EDIData.class);
		} catch (Exception e) {
			log.error("Error while invoking EDIAnalyticsData Service's Save API", e);
			return Mono.error(new OrderAckException(
					"Error while invoking EDIAnalytics OrderAckData Service Save API", e));
		}
	}	
	
	public Mono<EDIData> findByTraceIdAndType(String traceId, String type) {
		try { 
			return webClient.get()
				.uri(ediAnalyticsDataFindByTraceIdAndTypeUrl, traceId, type)
				.header("XATOM-CLIENTID", applicationName)
				.retrieve()
				.onStatus(HttpStatus::isError,
						exceptionFunction -> Mono.error(new OrderAckException(
								"EDIAnalyticsData Service's findByTraceIdAndType API returned Error:"
										+ exceptionFunction.rawStatusCode())))
				.bodyToMono(EDIData.class);
		} catch (Exception e) {
			log.error("Error while invoking EDIAnalyticsData Service findByTraceIdAndType API", e);
			return Mono.error(new OrderAckException(
					"Error while invoking EDIAnalyticsData Service findByTraceIdAndType API", e));
		}
	}

}
