package com.ecom.orderservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Random;

@Service
public class Producer
{
    private static final Logger logger = LoggerFactory.getLogger(Producer.class);
    private static final String TOPIC = "order-topic";

    @Autowired //DEPENDENCY INJECTION PROMISE FULFILLED AT RUNTIME
    private KafkaTemplate<String, String> kafkaTemplate ;

    @Autowired
    ObjectMapper objectMapper;

    public void pubUpdateProductDetailsMessage(String principal,
                                            String description) throws JsonProcessingException // LOGIN | REGISTER
    {
        Analytic analytic = new Analytic();
        analytic.setObjectid(String.valueOf((new Random()).nextInt()));
        analytic.setType("UPDATE");
        analytic.setPrincipal(principal);
        analytic.setDescription(description);
        analytic.setTimestamp(LocalTime.now()); // SETTING THE TIMESTAMP OF THE MESSAGE

        // convert to JSON
        String datum =  objectMapper.writeValueAsString(analytic);

        logger.info(String.format("#### -> Producing message -> %s", datum));
        this.kafkaTemplate.send(TOPIC,datum);
    }

    public void publishOrderPlaceMessage(OrderRequest request, SagaState sagaState) throws JsonProcessingException {
        OrderEvent event = new OrderEvent(request.getOrderId(), "ORDER_CREATED", request,sagaState);
        String orderEventJson =  objectMapper.writeValueAsString(event);
        kafkaTemplate.send("order-topic", request.getOrderId(), orderEventJson);
    }

    public void publishOrderCompletionMessage(String orderId,String orderStatus,SagaState sagaState) throws JsonProcessingException {
        OrderEvent event = new OrderEvent(orderId, orderStatus, null,sagaState);
        String orderEventJson =  objectMapper.writeValueAsString(event);
        kafkaTemplate.send("order-topic", orderId, orderEventJson);
    }
}
