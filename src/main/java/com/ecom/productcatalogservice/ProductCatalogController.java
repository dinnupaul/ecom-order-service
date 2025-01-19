package com.ecom.productcatalogservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("api/v1/products")
public class ProductCatalogController
{
    private static final Logger logger = LoggerFactory.getLogger(ProductCatalogController.class.getName());
    @Autowired
    ProductRepository productRepository;

    @Autowired
    Producer producer = new Producer();

    @PostMapping("/update") // URIs SERVE CHUNKS OF DATA UNLIKE URLs WHICH SERVE PAGES
    public ResponseEntity<String> updateProductDetails(@RequestBody Product product) throws JsonProcessingException {
        logger.info("initiating product update in Product Catalog Controller");
        productRepository.save(product);
        logger.info(" product update completed successfully in productCatalog Table");
        logger.info(product.getProductname()," initiating product topic");
        producer.pubUpdateProductDetailsMessage(product.getProductname(), "PRODUCT DETAILS UPDATED SUCCESSFULLY");

        return ResponseEntity.ok("Details Updated Successfully");
    }

    //@GetMapping("get/restros")
    //public ResponseEntity<?> getRestros() throws InterruptedException
    //{
    //    Thread.sleep(1000);
     //   return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();

    //}

    @GetMapping("/getAll")
    public ResponseEntity<Product> getProduct(@PathVariable String id) {
        Product product = productRepository.getReferenceById(id);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();

                //new ResponseEntity(product.get(0));
                //product.map(ResponseEntity::ok)
               // .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
