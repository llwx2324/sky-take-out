package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;

    private Orders orders; //跳过微信支付用


    @Override
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        Long userId = BaseContext.getCurrentId();

        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId()); //该订单对应的地址
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(ShoppingCart.builder().userId(userId).build()); //当前用户购物车数据



        //插入订单表
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setUserId(userId);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setStatus(Orders.PENDING_PAYMENT); //待付款
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);//未支付
        orders.setUserName(addressBook.getConsignee());
        orders.setPhone(addressBook.getPhone());
        String address = (addressBook.getProvinceName() == null ? "" : addressBook.getProvinceName())
                + (addressBook.getCityName() == null ? "" : addressBook.getCityName())
                + (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName())
                + (addressBook.getDetail() == null ? "" : addressBook.getDetail());
        orders.setAddress(address);
        orders.setConsignee(addressBook.getConsignee());
        this.orders = orders; //赋值给成员变量
        orderMapper.insert(orders);

        //插入订单明细表
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for(ShoppingCart shoppingCart : shoppingCartList){
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(shoppingCart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);
        //清空购物车
        shoppingCartMapper.deleteByUserId(userId);

        //返回订单相关信息
        OrderSubmitVO orderSubmitVO = new OrderSubmitVO();
        orderSubmitVO.setId(orders.getId());
        orderSubmitVO.setOrderTime(orders.getOrderTime());
        orderSubmitVO.setOrderAmount(orders.getAmount());
        orderSubmitVO.setOrderNumber(orders.getNumber());

        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @Override
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

//        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code","ORDERPAID");
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));
        Integer OrderPaidStatus = Orders.PAID;//支付状态，已支付
        Integer OrderStatus = Orders.TO_BE_CONFIRMED;  //订单状态，待接单
        LocalDateTime check_out_time = LocalDateTime.now();//更新支付时间
        orderMapper.updateStatus(OrderStatus, OrderPaidStatus, check_out_time, this.orders.getId());

        Map map = new HashMap();
        map.put("type", 1); // 1代表来单提醒 2代表用户催单
        map.put("orderId", this.orders.getId());
        map.put("content", "订单号：" + this.orders.getNumber());
        webSocketServer.sendToAllClient(JSONObject.toJSONString(map)); // 给商家端发送来单提醒


        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    @Override
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 历史订单查询
     * @return
     */
    @Override
    public PageResult historyOrders(OrdersPageQueryDTO ordersPageQueryDTO){
        List<OrderVO> orderVOList = new ArrayList<>();

        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize()); //插件会拦截下一次查询

        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        ordersPageQueryDTO.setUserId(userId);
        // 查询订单 -- 只是查询出来某页的orders
        Page<Orders> pageOrdersList = orderMapper.pageQuery(ordersPageQueryDTO);
        // 查询订单明细
        for(Orders orders : pageOrdersList){ // 于是查询出来某页的orders，vo也是某页的vo
            List<OrderDetail> orderDetailList = orderDetailMapper.listByOrderId(orders.getId());
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(orders, orderVO);
            orderVO.setOrderDetailList(orderDetailList);
            orderVOList.add(orderVO);
        }
        return new PageResult(pageOrdersList.getTotal(), orderVOList); //orderVOList也只是某页的orderVO列表
    }

    /**
     * 根据id查询订单详情
     * @param id
     * @return
     */
    @Override
    public OrderVO getByIdWithOrderDetail(Long id){
        //查询订单
        Orders orders = orderMapper.getById(id);
        if(orders == null){
            throw new OrderBusinessException("订单不存在");
        }
        //查询订单明细
        List<OrderDetail> orderDetailList = orderDetailMapper.listByOrderId(orders.getId());
        //将订单明细中的菜品和数量拼接成字符串赋给orderDishes
        StringBuilder orderDishes = new StringBuilder();
        for(OrderDetail orderDetail : orderDetailList){
            orderDishes.append(orderDetail.getName()).append("x").append(orderDetail.getNumber()).append(",");
        }
        if(orderDishes.length() > 0){
            orderDishes.deleteCharAt(orderDishes.length() - 1); //删除最后一个逗号
        }
        //封装vo
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        orderVO.setOrderDishes(orderDishes.toString());
        return orderVO;
    }

    /**
     * 取消订单
     * @param id
     */
    @Override
    public void cancel(Long id){
        //查询订单
        Orders orders = orderMapper.getById(id);
        if(orders == null){
            throw new OrderBusinessException("订单不存在");
        }
        //只有待付款、待接单、已接单、派送中的订单才能取消
        if(!(orders.getStatus().equals(Orders.PENDING_PAYMENT) || orders.getStatus().equals(Orders.TO_BE_CONFIRMED)
                || orders.getStatus().equals(Orders.CONFIRMED) || orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS))){
            throw new OrderBusinessException("该订单不能取消");
        }
        //更新订单状态为已取消
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 再来一单
     * @param id
     */
    @Override
    public void repetition(Long id){
        //查询订单
        Orders orders = orderMapper.getById(id);
        if(orders == null){
            throw new OrderBusinessException("订单不存在");
        }
        //查询订单明细
        List<OrderDetail> orderDetailList = orderDetailMapper.listByOrderId(orders.getId());
        if(orderDetailList == null || orderDetailList.size() == 0){
            throw new OrderBusinessException("订单无菜品，不能再来一单");
        }
        //将订单明细数据添加到购物车
        Long userId = BaseContext.getCurrentId();
        List<ShoppingCart> shoppingCartList = new ArrayList<>();
        for(OrderDetail orderDetail : orderDetailList){
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail, shoppingCart);
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartList.add(shoppingCart);
        }
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 订单搜索 -- S端
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO){
        List<OrderVO> orderVOList = new ArrayList<>();

        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        // 查询订单 -- 只是查询出来某页的orders
        Page<Orders> pageOrdersList = orderMapper.pageQuery(ordersPageQueryDTO);
        // 查询订单明细，将订单明细的菜品和数量拼接成orderDishes，然后封装到vo
        for(Orders orders : pageOrdersList){
            List<OrderDetail> orderDetailList = orderDetailMapper.listByOrderId(orders.getId());
            StringBuilder orderDishes = new StringBuilder();
            for(OrderDetail orderDetail : orderDetailList){
                orderDishes.append(orderDetail.getName()).append("x").append(orderDetail.getNumber()).append(",");
            }
            if(orderDishes.length() > 0){
                orderDishes.deleteCharAt(orderDishes.length() - 1); //删除最后一个逗号
            }
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(orders, orderVO);
            orderVO.setOrderDishes(orderDishes.toString());
            orderVOList.add(orderVO);
        }

        return new PageResult(pageOrdersList.getTotal(), orderVOList);
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        //查询订单
        Orders orders = orderMapper.getById(ordersConfirmDTO.getId());
        if(orders == null){
            throw new OrderBusinessException("订单不存在");
        }
        //只有待接单的订单才能接单
        if(!orders.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException("该订单不能接单");
        }
        //更新订单状态为已接单
        orders.setStatus(Orders.CONFIRMED);
        orderMapper.update(orders);
    }

    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        //查询订单
        Orders orders = orderMapper.getById(ordersRejectionDTO.getId());
        if(orders == null){
            throw new OrderBusinessException("订单不存在");
        }
        //只有待接单的订单才能拒单
        if(!orders.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException("该订单不能拒单");
        }
        //更新订单状态为已取消
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());
        orders.setCancelReason(ordersRejectionDTO.getRejectionReason());
        //更新支付状态为退款
        if(orders.getPayStatus().equals(Orders.PAID)){
            orders.setPayStatus(Orders.REFUND);
        }
        orderMapper.update(orders);
    }

    @Override
    public void delivery(Long id) {
        //查询订单
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException("订单不存在");
        }
        //只有已接单的订单才能派送
        if (!orders.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException("该订单不能派送");
        }
        //更新订单状态为派送中
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(orders);
    }

    @Override
    public void complete(Long id) {
        //查询订单
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException("订单不存在");
        }
        //只有派送中的订单才能完成
        if (!orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException("该订单不能完成");
        }
        //更新订单状态为已完成
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    @Override
    public void adminCancel(OrdersCancelDTO ordersCancelDTO) {
        //查询订单
        Orders orders = orderMapper.getById(ordersCancelDTO.getId());
        if(orders == null){
            throw new OrderBusinessException("订单不存在");
        }
        //只有待付款、已接单、派送中、已完成的订单才能取消
        if(!(orders.getStatus().equals(Orders.PENDING_PAYMENT) || orders.getStatus().equals(Orders.CONFIRMED)
                || orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS) || orders.getStatus().equals(Orders.COMPLETED))){
            throw new OrderBusinessException("该订单不能取消");
        }
        //更新订单状态为已取消
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        //更新支付状态为退款
        if(orders.getPayStatus().equals(Orders.PAID)){
            orders.setPayStatus(Orders.REFUND);
        }
        orderMapper.update(orders);
    }

    @Override
    public OrderStatisticsVO orderStatistics() {
        //统计待接单、已接单、派送中的订单数
        Integer toBeConfirmed = orderMapper.countByStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countByStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countByStatus(Orders.DELIVERY_IN_PROGRESS);
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    @Override
    public void reminder(Long id) {
        //查询订单
        Orders orders = orderMapper.getById(id);
        if(orders == null){
            throw new OrderBusinessException("订单不存在");
        }

        Map map = new HashMap();
        map.put("type", 2); // 1代表来单提醒 2代表用户催单
        map.put("orderId", id);
        map.put("content", "订单号：" + orders.getNumber());
        webSocketServer.sendToAllClient(JSONObject.toJSONString(map)); // 给商家端发送催单提醒
    }
}
