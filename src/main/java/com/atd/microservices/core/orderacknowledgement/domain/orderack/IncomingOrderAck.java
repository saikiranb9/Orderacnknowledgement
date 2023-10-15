package com.atd.microservices.core.orderacknowledgement.domain.orderack;

@lombok.Data
public class IncomingOrderAck {

    private MetaDataAck metadata;
    private OrderAckData data;

}
