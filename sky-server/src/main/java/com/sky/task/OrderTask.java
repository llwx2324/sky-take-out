package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单定时任务
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 取消超时未支付订单 -- 每 1 分钟执行一次
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void cancelTimeOutOrders() {
        log.info("取消超时未支付订单..., {}", LocalDateTime.now());
        //将状态为"待付款"且下单时间超过15分钟的订单设置为"已取消"
        List<Orders> ordersList= orderMapper.selectByStatusAndOrderTime(Orders.PENDING_PAYMENT, LocalDateTime.now().minusMinutes(15));
        if(ordersList != null && ordersList.size() > 0){
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("超时未支付，系统自动取消");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }

    }

    /**
     * 将派送时间过长的订单设置为"已完成" -- 每 10 分钟执行一次
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void finishDeliveryOrders() {
        log.info("将派送时间过长的订单设置为已完成..., {}", LocalDateTime.now());
        //将状态为"派送中"且下单时间离现在超过3小时的订单设置为"已完成"
        List<Orders> ordersList= orderMapper.selectByStatusAndOrderTime(Orders.DELIVERY_IN_PROGRESS, LocalDateTime.now().minusHours(3));
        if(ordersList != null && ordersList.size() > 0){
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.COMPLETED);
                orders.setDeliveryTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }
    }
}
