package com.ecom.orderservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SagaState implements Serializable {
    private String currentState; // Current saga state (e.g., ORDER_CREATED, PAYMENT_FAILED)
    private OrderRequest orderRequest; // Original order request
    private Map<String, String> stepStatus = new HashMap<>(); // Status of each step
    private int retryCount = 0; // Retry counter

    public SagaState(String currentState, OrderRequest request) {
        this.orderRequest = request;
        this.currentState = currentState;
    }

    public void updateStepStatus(String step, String status) {
        stepStatus.put(step, status);
    }
}
