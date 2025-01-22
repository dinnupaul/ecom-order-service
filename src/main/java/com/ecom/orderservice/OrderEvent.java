package com.ecom.orderservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private String orderId;
    private String eventType;
    private OrderRequest request;
    private SagaState sagaState;
    private String traceId;
}