package com.ecom.orderservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@RestController
@RequestMapping("api/v1/orders")
public class OrderController
{
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class.getName());
    @Autowired
    OrderRepository orderRepository;

    @Autowired
    Producer producer = new Producer();

    private final RedisTemplate<String, Object> redisTemplate;

    public OrderController(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // @PostMapping("/placeorder")
    /*** public ResponseEntity<String> placeOrder(@RequestBody OrderRequest request) throws JsonProcessingException {
        // Create initial order
        logger.info("Place order request: {}", request);
        Order order = new Order(request.getOrderId(), request.getCustomerId(),
                request.getProductId(), request.getQuantity(),"PENDING");

        // Save order state in Redis
        logger.info("Save order state in Redis");
        redisTemplate.opsForValue().set("ORDER_" + request.getOrderId(), order);

        // Publish order creation event
        logger.info("Publish order creation event");
        producer.publishOrderPlaceMessage(request);

        logger.info("Order placed and processing initiated");
        return ResponseEntity.ok("Order placed and processing initiated");
    } ***/


    @PostMapping("/placeorder")
    public ResponseEntity<?> placeOrder(@RequestBody OrderRequest request,
                                        @CookieValue(value = "orderId", required = false) String orderId,
                                        HttpServletResponse response) throws JsonProcessingException {
        // Create initial order state

        if (orderId == null) {
            orderId = UUID.randomUUID().toString();

            logger.info("Create initial order DB persistence with pending status");
            Order order = new Order(orderId, request.getCustomerId(),
                    request.getProductId(), request.getQuantity(),"PENDING");
            orderRepository.save(order);
            logger.info("Create initial order state");
            request.setOrderStatus("ORDER_CREATED");
            SagaState sagaState = new SagaState("ORDER_CREATED", request);
            sagaState.setOrderRequest(request);
            sagaState.getOrderRequest().setOrderId(orderId);
            sagaState.updateStepStatus("Order","ORDER_CREATED");
            sagaState.updateStepStatus("Inventory", "PENDING");
            sagaState.updateStepStatus("Payment", "PENDING");
            sagaState.setCurrentState("ORDER_CREATED");
            redisTemplate.opsForValue().set("ORDER_" + orderId, sagaState);

            // Set cookie for orderId
            ResponseCookie cookie = ResponseCookie.from("orderId", orderId)
                    .path("/")
                    .httpOnly(true)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            // Publish initial order creation event
            logger.info("Publish initial order creation event");
            producer.publishOrderPlaceMessage(request,sagaState);
            logger.info("Order placed and processing initiated");
            return ResponseEntity.ok()
                    .header("Set-Cookie", cookie.toString())
                    .body("Order placed and processing initiated");
        }else{
            SagaState sagaState = (SagaState) redisTemplate.opsForValue().get("ORDER_" + orderId);

            if("ORDER_CONFIRMED".equals(sagaState.getCurrentState())){
                return ResponseEntity.ok("Order placed successfully: " + orderId);
            }else if("ORDER_FAILED".equals(sagaState.getCurrentState())){
                return ResponseEntity.ok("Order request {} failed. Please try placing new order ");
            }else if("INVENTORY_FAILED".equals(sagaState.getCurrentState()) || "PAYMENT_FAILED".equals(sagaState.getCurrentState()) ){
                // Retry failed steps
                boolean isRetrySuccessful = retryFailedSteps(orderId);
                if (isRetrySuccessful) {
                    return ResponseEntity.ok("Retrying for order ID: " + orderId);
                } else if (!isRetrySuccessful && sagaState.getRetryCount()>=3){
                    rollbackOrder(sagaState.getOrderRequest(),sagaState);
                    return ResponseEntity.ok("Order Failed: " + orderId);
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Retry failed for order ID: " + orderId);
                }
            }

        }
        //String sessionId = UUID.randomUUID().toString();
       // SagaState sagaState = (SagaState) redisTemplate.opsForValue().get(sessionId);


       // logger.info("Create initial order DB persistence with pending status");
       // Order order = new Order(request.getOrderId(), request.getCustomerId(),
         //       request.getProductId(), request.getQuantity(),"PENDING");
       // orderRepository.save(order);
      //  logger.info("Create initial order state");
       // OrderState orderState = new OrderState(request.getOrderId(), "PENDING", new HashMap <String, String>(), 0);
        //orderState.updateStepStatus("Inventory", "PENDING");
        //orderState.updateStepStatus("Payment", "PENDING");

        // Track order in Redis
        /*** logger.info("Save order in Redis");
        redisTemplate.opsForValue().set("ORDER_ID_" + request.getOrderId(), order);

        logger.info("Save order state in Redis");
       // redisTemplate.opsForValue().set("ORDER_" + request.getOrderId(), orderState);


        // Store initial state in Redis
        SagaState sagaState = new SagaState("ORDER_CREATED", request);
        sagaState.updateStepStatus("Order","PENDING");
        sagaState.updateStepStatus("Inventory", "PENDING");
        sagaState.updateStepStatus("Payment", "PENDING");
        redisTemplate.opsForValue().set(sessionId, sagaState);

        // Set cookie
        ResponseCookie cookie = ResponseCookie.from("sessionId", sessionId)
                .httpOnly(true)
                .path("/")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());***/



        // Publish initial order creation event
       /**** logger.info("Publish initial order creation event");
        producer.publishOrderPlaceMessage(request);
***/
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Retry failed for order ID: " + orderId);

    }

    public boolean retryFailedSteps(String orderId) throws JsonProcessingException {
        boolean canRetry = false;
        // Fetch state from Redis
        SagaState sagaState = (SagaState) redisTemplate.opsForValue().get("ORDER_" + orderId);

        if(sagaState!=null && sagaState.getRetryCount()<3) {
            canRetry = true;
            sagaState.setRetryCount(sagaState.getRetryCount()+1);

            logger.info("Publish initial order creation event");
            producer.publishOrderPlaceMessage(sagaState.getOrderRequest(), sagaState);

        }

       /*** // Retry inventory check if not confirmed
        if (!sagaState.is) {
            kafkaTemplate.send("inventory-events", orderId, new OrderEvent(orderId, "RETRY_INVENTORY", sagaState.getOrderRequest()));
            return false;
        }

        // Retry payment if not confirmed
        if (!sagaState.isPaymentConfirmed()) {
            kafkaTemplate.send("payment-events", orderId, new OrderEvent(orderId, "RETRY_PAYMENT", sagaState.getOrderRequest()));
            return false;
        }

        // All steps successful
        sagaState.setStatus("COMPLETED");
        redisTemplate.opsForValue().set("ORDER_" + orderId, sagaState);***/
        return canRetry ;
    }

    public void confirmOrder(OrderRequest orderRequest,SagaState sagaState) {
        Order order = orderRepository.findById(orderRequest.getOrderId()).orElseThrow(() -> new RuntimeException("Order not found"));
        order.setOrderStatus("ORDER_CONFIRMED");
        orderRepository.save(order); // Persist success
        sagaState.updateStepStatus("Order", "ORDER_CONFIRMED");
        sagaState.setCurrentState("ORDER_CONFIRMED");
        redisTemplate.opsForValue().set("ORDER_" + orderRequest.getOrderId(), sagaState);

     //   redisTemplate.opsForValue().set("ORDER_" + orderId, order);
    }

    public void rollbackOrder(OrderRequest orderRequest,SagaState sagaState) {

        Order order = orderRepository.findById(orderRequest.getOrderId()).orElseThrow(() -> new RuntimeException("Order not found"));
        if(sagaState!=null && sagaState.getRetryCount()>3) {
            order.setOrderStatus("ORDER_FAILED");
            orderRepository.save(order);
            sagaState.updateStepStatus("Order", "ORDER_FAILED");
            sagaState.setCurrentState("ORDER_FAILED");
        }
         // Persist failure

        redisTemplate.opsForValue().set("ORDER_" + orderRequest.getOrderId(), sagaState);
       // redisTemplate.delete("ORDER_" + orderRequest.getOrderId());
    }

    public void publishOrderCompletionMessage(String orderId,String orderStatus) throws JsonProcessingException {
        // Publish initial order creation event
        logger.info("Retry order creation event");
        SagaState sagaState = (SagaState) redisTemplate.opsForValue().get("ORDER_" + orderId);
        producer.publishOrderCompletionMessage(orderId,orderStatus,sagaState);
    }

    @PostMapping("/update") // URIs SERVE CHUNKS OF DATA UNLIKE URLs WHICH SERVE PAGES
    public ResponseEntity<String> updateOrderDetails(@RequestBody Order order) throws JsonProcessingException {
        logger.info("initiating product update in Product Catalog Controller");
        orderRepository.save(order);
        logger.info(" product update completed successfully in productCatalog Table");
        logger.info(order.getOrderId()," initiating product topic");
        //producer.pubUpdateProductDetailsMessage(order.getOrderStatus(), "PRODUCT DETAILS UPDATED SUCCESSFULLY");

        return ResponseEntity.ok("Details Updated Successfully");
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable String id) {
        Optional<Order> product = orderRepository.findById(id);
        return product.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<Order>> getAllOrders() {
        List<Order> orders = orderRepository.findAll();

        if (orders.isEmpty()) {
            // Return a 204 No Content response if no orders are found.
            return ResponseEntity.noContent().build();
        }

        // Return the list of orders with a 200 OK status.
        return ResponseEntity.ok(orders);
    }

}
