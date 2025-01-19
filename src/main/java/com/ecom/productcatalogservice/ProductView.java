package com.ecom.productcatalogservice;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ProductView
{
    String productId;
    String productName;
    String productDesc;
    Integer price;
}
