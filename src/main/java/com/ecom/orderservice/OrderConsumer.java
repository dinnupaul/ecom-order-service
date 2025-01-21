package com.ecom.orderservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class OrderConsumer
{
    private final Logger logger = LoggerFactory.getLogger(OrderConsumer.class);


    @Autowired
    ObjectMapper mapper;

    @Autowired
    OrderController orderController;

    private final RedisTemplate<String, Object> redisTemplate;

    public OrderConsumer(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @KafkaListener(topics = "payment-topic", groupId = "ecom-order-service")
    public void consumeOrderCompletionEvents(String message) throws IOException {
        logger.info(String.format("#### -> about to consume payment topic"));
        PaymentEvent paymentEvent = mapper.readValue(message, PaymentEvent.class);
        logger.info(String.format("#### -> Consumed message from payment topic in order service-> %s", paymentEvent.getOrderId()));



        //String redisKey = "ORDER_" + paymentEvent.getOrderId();
        // OrderState orderState = (OrderState) redisTemplate.opsForValue().get(redisKey);

        if ("PAYMENT_SUCCESS".equals(paymentEvent.getPaymentStatus())) {
            orderController.confirmOrder(paymentEvent.getOrderRequest(),paymentEvent.getSagaState());
          //   orderState.setOrderStatus("CONFIRMED");
           /** sagaState.updateStepStatus("Order", "ORDER_CONFIRMED");
            redisTemplate.opsForValue().set("ORDER_" + orderRequest.getOrderStatus(), sagaState); **/

           //  redisTemplate.delete(redisKey); // Clean up after success
        } else{
            orderController.rollbackOrder(paymentEvent.getOrderRequest(),paymentEvent.getSagaState());

        }



        /*** else {
            // Retry or handle failure logic
            orderState.setOrderStatus("RETRY");
            // Optionally re-publish order event for retry
        } **/

       // redisTemplate.opsForValue().set(redisKey, orderState); // Update state
    }
      /****  //analytics_counter.increment();
        //Analytic datum =  mapper.readValue(message,Analytic.class);
        logger.info(String.format("#### -> about to consume inventory topic"));
        InventoryEvent inventoryEvent = mapper.readValue(message, InventoryEvent.class);
        logger.info(String.format("#### -> Consumed message from inventory topic in order service-> %s", inventoryEvent.getOrderId()));

        if ("ORDER_CREATED".equals(orderEvent.getEventType())) {
            // Check product availability
            invController.publishInventoryMessage(orderEvent.getRequest());
        }
        if ("INVENTORY_CONFIRMED".equals(event.getStatus())) {
            kafkaTemplate.send("payment-events", event.getOrderId(),
                    new PaymentEvent(event.getOrderId(), "PROCESS_PAYMENT"));
        } ***/
      @KafkaListener(topics = "order-retry-topic", groupId = "ecom-order-service")
      public void consumeInterimResponseEvents(String message) throws IOException {
          logger.info(String.format("#### -> about to consume payment topic"));
          InterimEvent interimEvent = mapper.readValue(message, InterimEvent.class);
          logger.info(String.format("#### -> Consumed message from payment topic in order service-> %s", interimEvent.getOrderId()));

          String redisKey = "ORDER_" + interimEvent.getOrderId();
          OrderState orderState = (OrderState) redisTemplate.opsForValue().get(redisKey);
          if (orderState == null) {
              // Handle missing state
              return;
          }
          // Update the specific step's status
          orderState.updateStepStatus(interimEvent.getStepName(), interimEvent.getStatus());

          // Save updated state back to Redis
          redisTemplate.opsForValue().set(redisKey, orderState);
          if (orderState.isRetryComplete()) {
              if (orderState.allStepsSuccessful()) {
                  orderState.setOrderStatus("CONFIRMED");
                  orderController.publishOrderCompletionMessage(interimEvent.getOrderId(),"CONFIRMED");
                 // kafkaTemplate.send("order-events", event.getOrderId(), new OrderEvent(event.getOrderId(), "ORDER_CONFIRMED", null));
              } else {
                  orderState.setOrderStatus("FAILED");
                  orderController.publishOrderCompletionMessage(interimEvent.getOrderId(),"FAILED");

                  // kafkaTemplate.send("order-events", event.getOrderId(), new OrderEvent(event.getOrderId(), "ORDER_FAILED", null));
              }
          }
      }



      }

