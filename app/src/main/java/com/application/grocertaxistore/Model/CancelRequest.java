package com.application.grocertaxistore.Model;

import com.google.firebase.Timestamp;

public class CancelRequest {
    private String requestID, orderID, placingTime, requestTime;
    private Timestamp requestTimestamp;

    public CancelRequest() {
    }

    public CancelRequest(String requestID, String orderID, String placingTime, String requestTime, Timestamp requestTimestamp) {
        this.requestID = requestID;
        this.orderID = orderID;
        this.placingTime = placingTime;
        this.requestTime = requestTime;
        this.requestTimestamp = requestTimestamp;
    }

    public String getRequestID() {
        return requestID;
    }

    public void setRequestID(String requestID) {
        this.requestID = requestID;
    }

    public String getOrderID() {
        return orderID;
    }

    public void setOrderID(String orderID) {
        this.orderID = orderID;
    }

    public String getPlacingTime() {
        return placingTime;
    }

    public void setPlacingTime(String placingTime) {
        this.placingTime = placingTime;
    }

    public String getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(String requestTime) {
        this.requestTime = requestTime;
    }

    public Timestamp getRequestTimestamp() {
        return requestTimestamp;
    }

    public void setRequestTimestamp(Timestamp requestTimestamp) {
        this.requestTimestamp = requestTimestamp;
    }
}
