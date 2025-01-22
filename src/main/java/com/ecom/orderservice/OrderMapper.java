package com.ecom.orderservice;

import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderView toOrderView(Order order) {
        OrderView orderView = new OrderView();
        orderView.setOrderId(order.getOrderId());
        orderView.setCustomerId(order.getCustomerId());
        orderView.setProductId(order.getProductId());
        orderView.setQuantity(order.getQuantity());
        orderView.setOrderStatus(order.getOrderStatus());
        return orderView;
    }
}
