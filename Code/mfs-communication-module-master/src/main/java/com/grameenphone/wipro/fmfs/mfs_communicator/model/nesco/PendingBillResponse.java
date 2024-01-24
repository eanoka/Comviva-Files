package com.grameenphone.wipro.fmfs.mfs_communicator.model.nesco;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PendingBillResponse {
    private int status_code;
    private String status;// "000"
    private String status_title;// "API response is successful from Biller"
    private String account_name;// "MD. SAJAHAN ALI"
    private String account_address;// "ROSULPUR ,SHIBGONJ",
    private String message;

    private List<NescoBillDetail> detail;

    @JsonDeserialize(using = CustomDateDeserializer.class)
    public void setData(Object p) {
        if(p instanceof Map) {
            status = (String)((Map<?, ?>) p).get("status");
            message = (String)((Map<?, ?>) p).get("message");
            detail = new ArrayList<>();
        } else {
            detail = (List<NescoBillDetail>)p;
        }
    }

    public static class CustomDateDeserializer extends StdDeserializer<Object> {
        public CustomDateDeserializer() {
            super((Class)null);
        }

        @Override
        public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if(p.getCurrentToken() == JsonToken.START_ARRAY) {
                return p.readValueAs(new TypeReference<List<NescoBillDetail>>() {});
            } else {
                return p.readValueAs(Map.class);
            }
        }
    }

    public int getStatus_code() {
        return status_code;
    }

    public void setStatus_code(int status_code) {
        this.status_code = status_code;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus_title() {
        return status_title;
    }

    public void setStatus_title(String status_title) {
        this.status_title = status_title;
    }

    public String getAccount_name() {
        return account_name;
    }

    public void setAccount_name(String account_name) {
        this.account_name = account_name;
    }

    public String getAccount_address() {
        return account_address;
    }

    public void setAccount_address(String account_address) {
        this.account_address = account_address;
    }

    public List<NescoBillDetail> getData() {
        return detail;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}