package com.ecom.orderservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentEvent {
    private String orderId;
    private String paymentStatus;
    private OrderRequest orderRequest;
    private SagaState sagaState;
    private String traceId;
}