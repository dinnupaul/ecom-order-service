package com.ecom.orderservice;

import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class OrderView
{
    String orderId;
    String customerId;
    String productId;
    Integer quantity;
    String orderStatus;
}
