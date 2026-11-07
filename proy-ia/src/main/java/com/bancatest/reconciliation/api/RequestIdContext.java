package com.bancatest.reconciliation.api;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class RequestIdContext {

    private String requestId;

    public String requestId() {
        return requestId;
    }

    public void requestId(String requestId) {
        this.requestId = requestId;
    }
}
