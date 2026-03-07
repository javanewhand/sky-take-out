package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;


/*
* 定时任务类
* */
@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;

    /*
    * 处理超时任务
    * */
    @Scheduled(cron = "0 0/15 * * * ?")
    public void processTimeoutOrder() {
        log.info("定时检查超时订单任务开始执行：{}", LocalDateTime.now());
        LocalDateTime localDateTime = LocalDateTime.now().plusMinutes(-15);
        List<Orders> ordersList = orderMapper.processTimeoutOrder(Orders.PENDING_PAYMENT, localDateTime);
        if(ordersList.size() > 0 && ordersList!=null) {
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelTime(localDateTime);
                orders.setCancelReason("订单超时已取消");
                orderMapper.update(orders);
            }
        }
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrder() {
        log.info("定时处理超时派送中订单：{}", LocalDateTime.now());
        LocalDateTime localDateTime = LocalDateTime.now().plusMinutes(-60);
        List<Orders> ordersList = orderMapper.processTimeoutOrder(Orders.DELIVERY_IN_PROGRESS, localDateTime);
        if(ordersList.size() > 0 && ordersList!=null) {
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.COMPLETED);
                orders.setCancelTime(localDateTime);
                orderMapper.update(orders);
            }
        }
    }
}
