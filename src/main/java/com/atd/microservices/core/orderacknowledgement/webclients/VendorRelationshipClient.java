package com.atd.microservices.core.orderacknowledgement.webclients;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriUtils;

import com.atd.microservices.core.orderacknowledgement.domain.VendorRelationship;
import com.atd.microservices.core.orderacknowledgement.exception.OrderAckException;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class VendorRelationshipClient {

	@Autowired
	private WebClient webClient;
	
	@Value("${spring.application.name}")
	private String applicationName;

	@Value("${orderacknowledgement.vendorRelationshipUrl}")
	private String vendorRelationshipUrl;
	
	public Mono<VendorRelationship> getSupplierInfoByVendorName(String vendorName) {
		try { 
			return webClient.get()
				.uri(URI.create(String.format(vendorRelationshipUrl, encodePath(vendorName))))
				.header("XATOM-CLIENTID", applicationName)
				.retrieve()
				.onStatus(HttpStatus::isError,
						exceptionFunction -> Mono
								.error(new OrderAckException("VendorRelationship Service's API returned Error:"
										+ exceptionFunction.rawStatusCode())))
				.bodyToMono(VendorRelationship.class);
		} catch (Exception e) {
			log.error("Error while invoking VendorRelationship Service's API", e);
			return Mono.error(new OrderAckException(
					"Error while invoking VendorRelationship Service API", e));
		}
	}
	
	public String encodePath(String path) {
		try {
			path = UriUtils.encodePath(path, "UTF-8");
		} catch (Exception ex) {
			throw new RuntimeException(ex.getCause());
		}
		return path;
	}
}
