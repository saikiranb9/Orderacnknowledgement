package com.atd.microservices.core.orderacknowledgement.domain.orderack;

import lombok.Data;

@Data
public class CsvAck {

	private String vendorName;
	private String poNumber;
	private String vendorNumber;
	private String date;
	private String productNumber;
	private String requestedQuantity;
	private String filledQuantity;
	private String cost;
}