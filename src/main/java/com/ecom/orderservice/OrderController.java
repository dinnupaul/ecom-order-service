package com.ecom.orderservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("api/v1/orders")
public class OrderController
{
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class.getName());
    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OrderMapper orderMapper;

    @Autowired
    Producer producer = new Producer();

    private final RedisTemplate<String, Object> redisTemplate;

    public OrderController(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    @PostMapping("/placeorder")
    public ResponseEntity<?> placeOrder(@RequestBody OrderRequest request,
                                        @CookieValue(value = "orderId", required = false) String orderId,
                                        HttpServletResponse response) throws JsonProcessingException {
        // Generate or extract trace ID
        String traceId = MDC.get("traceId");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
            MDC.put("traceId", traceId); // Add trace ID to MDC for logging
        }
        logger.info("Processing order with trace ID: {}", traceId);

        // Create initial order state
        if (orderId == null) {
            logger.info("Create initial order state in  order service");
            orderId = UUID.randomUUID().toString();

            logger.info("Create initial order DB persistence with pending status");
            Order order = new Order(orderId, request.getCustomerId(),
                    request.getProductId(), request.getQuantity(),"PENDING");
            orderRepository.save(order);
            logger.info("save initial order state in DB");
            request.setOrderStatus("ORDER_CREATED");
            SagaState sagaState = new SagaState("ORDER_CREATED", request);
            sagaState.setOrderRequest(request);
            sagaState.getOrderRequest().setOrderId(orderId);
            sagaState.updateStepStatus("Order","ORDER_CREATED");
            sagaState.updateStepStatus("Inventory", "PENDING");
            sagaState.updateStepStatus("Payment", "PENDING");
            sagaState.setCurrentState("ORDER_CREATED");
            logger.info("update sagaState in cache from order service");
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
            String newTraceId = UUID.randomUUID().toString();
            MDC.put("traceId", newTraceId);
           // MDC.put("parentTraceId", originalTraceId);

            logger.info("Followup call in  order service, : initial order state exists");
            SagaState sagaState = (SagaState) redisTemplate.opsForValue().get("ORDER_" + orderId);

            if("ORDER_CONFIRMED".equals(sagaState.getCurrentState())){
                logger.info("ORDER_CONFIRMED flow in  order service");
                return ResponseEntity.ok("Order placed successfully: " + orderId);
            }else if("ORDER_FAILED".equals(sagaState.getCurrentState())){
                logger.info("ORDER_FAILED flow in  order service");
                return ResponseEntity.ok("Order request {} failed. Please try placing new order ");
            }else if("INVENTORY_FAILED".equals(sagaState.getCurrentState()) || "PAYMENT_FAILED".equals(sagaState.getCurrentState()) ){
                // Retry failed steps
                logger.info("INVENTORY_FAILED/PAYMENT_FAILED flow in  order service, Retry failed steps");
                boolean isRetrySuccessful = retryFailedSteps(orderId);
                if (isRetrySuccessful) {
                    logger.info(" Retry initiated in order service ");
                    return ResponseEntity.ok("Retrying for order ID: " + orderId);
                } else if (!isRetrySuccessful && sagaState.getRetryCount()>=3){
                    logger.info(" Retry failed and rollback initiated in order service ");
                    rollbackOrder(sagaState.getOrderRequest(),sagaState);
                    return ResponseEntity.ok("Order Failed: " + orderId);
                } else {
                    logger.info(" Retry failed bad request in order service ");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Retry failed for order ID: " + orderId);
                }
            }

        }
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
        return canRetry ;
    }

    public void confirmOrder(OrderRequest orderRequest,SagaState sagaState) {
        Order order = orderRepository.findById(orderRequest.getOrderId()).orElseThrow(() -> new RuntimeException("Order not found"));
        order.setOrderStatus("ORDER_CONFIRMED");
        orderRepository.save(order); // Persist success
        sagaState.updateStepStatus("Order", "ORDER_CONFIRMED");
        sagaState.setCurrentState("ORDER_CONFIRMED");
        redisTemplate.opsForValue().set("ORDER_" + orderRequest.getOrderId(), sagaState);
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
        return ResponseEntity.ok("Details Updated Successfully");
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable String id) {
        Optional<Order> product = orderRepository.findById(id);
        return product.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/getAll")
    public ResponseEntity<?> getAllOrders() {
        try {
            List<Order> orders = orderRepository.findAll();

            if (orders.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No orders available.");
            }

            return ResponseEntity.ok(orders.stream()
                    .map(orderMapper::toOrderView)
                    .collect(Collectors.toList()));

        } catch (Exception e) {
            // Log the error (logging can be added here for production)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching orders: " + e.getMessage());
        }
    }


    @GetMapping("/all")
    public List<OrderView> fetchAllOrders() {
        try {
            List<Order> orders = orderRepository.findAll();

            if (orders.isEmpty()) {
                return new ArrayList<>();
            }

            return orders.stream()
                    .map(orderMapper::toOrderView)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

}
