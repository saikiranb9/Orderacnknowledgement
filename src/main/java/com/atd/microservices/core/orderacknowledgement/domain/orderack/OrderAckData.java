package com.atd.microservices.core.orderacknowledgement.domain.orderack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

@lombok.Data
public class OrderAckData {
    private String locationNumber;
    private String customerPO;
    private List<Product> products;
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