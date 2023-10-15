package com.atd.microservices.core.orderacknowledgement.webclients;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.atd.microservices.core.orderacknowledgement.domain.EdiConfig;
import com.atd.microservices.core.orderacknowledgement.exception.OrderAckException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class EDIConfigClient {

	@Autowired
	private WebClient webClient;

	@Value("${spring.application.name}")
	private String applicationName;

	@Value("${orderacknowledgement.ediconfigUrl}")
	private String ediconfigUrl;

	public Mono<Map<String, String>> findBySenderCodeAndReceiverCode(String senderCode, String receiverCode) {
		try {
			return webClient.get().uri(ediconfigUrl, senderCode, receiverCode).headers(headers -> {
				headers.set("XATOM-CLIENTID", applicationName);
			}).retrieve()
					.onStatus(HttpStatus::isError,
							exceptionFunction -> Mono.error(new OrderAckException(
									"EdiConfig OrderAckData Service findBySenderCodeAndReceiverCode API returned Error:"
											+ exceptionFunction.rawStatusCode())))
					.bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {
					});
		} catch (Exception e) {
			log.error("Error while invoking EdiConfig OrderAckData Service findBySenderCodeAndReceiverCode API", e);
			return Mono.error(new OrderAckException(
					"Error while invoking EdiConfig OrderAckData Service findBySenderCodeAndReceiverCode API", e));
		}
	}

}
