package com.ecom.orderservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InterimEvent {
    private String orderId;
    private String stepName; // e.g., "Inventory", "Payment"
    private String status; // e.g., "RETRY", "SUCCESS", "FAILED"
}