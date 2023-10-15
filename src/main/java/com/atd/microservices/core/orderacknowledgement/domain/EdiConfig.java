package com.atd.microservices.core.orderacknowledgement.domain;

import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class EdiConfig {
	public static String UUID = "uuid";
    private String fillOrKill;
    private String customerID;
    private String fulfillmentType;
    @NotNull
    private String uuid;
}
