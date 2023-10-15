package com.atd.microservices.core.orderacknowledgement.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VendorRelationship {
	
	private String vendorName;
	private String vendorNumber;
	private String enabledFlag;
	private Long vendorId;
	 
}