package com.ecom.productcatalogservice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
@Entity
@Table(name = "products", schema = "product_catalog")
public class Product {

    @Id
    @Column(name = "product_id", nullable = false, length = 50)
    private String productid;
    @Column(name = "name", length = 50)
    private String productname;
    @Column(name = "description", length = 200)
    private String description;
    @Column(name = "price", length = 50)
    private Integer price;
    // private int availableQuantity;

    // Getters and Setters

    public String getProductid() {
        return productid;
    }

    public void setProductid(String productid) {
        this.productid = productid;
    }

    public String getProductname() {
        return productname;
    }

    public void setProductname(String productname) {
        this.productname = productname;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }
}
