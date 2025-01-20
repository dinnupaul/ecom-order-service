package com.ecom.orderservice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "orders", schema = "order_service")
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "order_id", nullable = false, length = 50)
    private String orderId;
    @Column(name = "customer_id", nullable = false, length = 50)
    private String customerId;
    @Column(name = "product_id", nullable = false, length = 50)
    private String productId;
    @Column(name = "quantity", length = 50)
    private Integer quantity;
    @Column(name = "order_status", length = 200)
    private String orderStatus;

    public Order(String orderId, String customerId, String productId, Integer quantity, String orderStatus) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.orderStatus = orderStatus;
    }

    public Order() {

    }
// Getters and Setters

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }
}
