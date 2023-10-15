package com.atd.microservices.core.orderacknowledgement.domain.orderack;

import java.util.List;

import lombok.Data;

@Data
public class CompleteCsvAck {

	private List<CsvAck> lines;
}
