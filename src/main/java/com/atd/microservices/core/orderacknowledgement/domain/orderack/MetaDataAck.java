package com.atd.microservices.core.orderacknowledgement.domain.orderack;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

@Data
public class MetaDataAck {
    private String traceId;
    private String senderId;
    private String senderCode;
    private String receiverCode;
    @JsonIgnore
    Map<String, String> unknownFields = new HashMap<>();
    @JsonAnyGetter
    public Map<String, String> otherFields() {
        return unknownFields;
    }
    
    @JsonAnySetter
    public void setOtherField(String name, String value) {
        unknownFields.put(name, value);
    }
}