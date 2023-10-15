package com.atd.microservices.core.orderacknowledgement.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import brave.Tracer;

@Component
public class EDITracerUtil {
	
	@Autowired
	private Tracer tracer;

	public String getTraceId(String payloadTraceId) {
		String traceId = null;
		if (tracer != null) {
			traceId = tracer.currentSpan().context().traceIdString();
		} else {
			traceId = payloadTraceId;
		}
		return traceId;
	}

}
