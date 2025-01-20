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
public class OrderState implements Serializable {
    private String orderId;
    private String orderStatus; // e.g., "PENDING", "CONFIRMED", "FAILED"
    private Map<String, String> stepStatuses = new HashMap<String,String>(); // Key: Step name, Value: Status
    private int retryCount;

    public void updateStepStatus(String stepName, String status) {
        stepStatuses.put(stepName, status);
    }

    public boolean allStepsSuccessful() {
        return stepStatuses.values().stream().allMatch(status -> status.equals("SUCCESS"));
    }

    public boolean isRetryComplete() {
        // Logic to determine if all retries are completed
        return retryCount >= 3 || allStepsSuccessful();
    }
}