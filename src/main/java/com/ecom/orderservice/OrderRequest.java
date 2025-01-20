package com.ecom.orderservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String orderId;
    private String customerId;
    private String productId;
    private int quantity;
    private String orderStatus;

}
