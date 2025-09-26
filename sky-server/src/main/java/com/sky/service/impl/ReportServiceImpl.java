package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;


    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = begin.datesUntil(end.plusDays(1)).toList();
        //统计当天已完成的订单的总金额
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            Map map = new HashMap();
            //设置开始时间
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            map.put("beginTime", beginTime);
            //设置结束时间
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            map.put("endTime", endTime);
            //订单状态 5表示已完成
            map.put("status", 5);
            Double turnover = orderMapper.sumByMap(map);
            if (turnover == null) {
                turnover = 0.0;
            }
            turnoverList.add(turnover);
        }
        TurnoverReportVO turnoverReportVO = new TurnoverReportVO();
        //将list转换成string并用逗号分隔每个元素
        turnoverReportVO.setDateList(StringUtils.join(dateList, ','));
        turnoverReportVO.setTurnoverList(StringUtils.join(turnoverList, ','));
        return turnoverReportVO;
    }

    /**
     * 用户统计
     */
    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = begin.datesUntil(end.plusDays(1)).toList();
        //统计每天新增用户数
        List<Integer> newUserList = new ArrayList<>();
        //统计每天累计用户数
        List<Integer> totalUserList = new ArrayList<>();
        for (LocalDate date : dateList) {
            Map map = new HashMap();
            //设置结束时间
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            map.put("endTime", endTime);
            //统计累计用户数
            Integer totalUser = userMapper.countByMap(map);
            //设置开始时间
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            map.put("beginTime", beginTime);
            //统计新增用户数
            Integer newUser = userMapper.countByMap(map);
            newUserList.add(newUser);
            totalUserList.add(totalUser);
        }
        UserReportVO userReportVO = new UserReportVO();
        //将list转换成string并用逗号分隔每个元素
        userReportVO.setDateList(StringUtils.join(dateList, ','));
        userReportVO.setNewUserList(StringUtils.join(newUserList, ','));
        userReportVO.setTotalUserList(StringUtils.join(totalUserList, ','));
        return userReportVO;
    }

    /**
     * 订单统计
     */
    @Override
    public OrderReportVO orderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = begin.datesUntil(end.plusDays(1)).toList();
        //统计每天订单数
        List<Integer> orderCountList = new ArrayList<>();
        //统计每天有效订单数
        List<Integer> validOrderCountList = new ArrayList<>();
        //统计总订单数
        Integer totalOrderCount = 0;
        //统计总有效订单数
        Integer validOrderCount = 0;
        for (LocalDate date : dateList) {
            Map map = new HashMap();
            //设置开始时间
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            map.put("beginTime", beginTime);
            //设置结束时间
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            map.put("endTime", endTime);
            //统计订单数
            Integer orderCount = orderMapper.countByMap(map);
            orderCountList.add(orderCount);
            totalOrderCount += orderCount;
            //已完成的订单为有效订单
            map.put("status", 5);
            //统计有效订单数
            Integer validOrderCountByDay = orderMapper.countByMap(map);
            validOrderCountList.add(validOrderCountByDay);
            validOrderCount += validOrderCountByDay;
        }
        OrderReportVO orderReportVO = new OrderReportVO();
        //将list转换成string并用逗号分隔每个元素
        orderReportVO.setDateList(StringUtils.join(dateList, ','));
        orderReportVO.setOrderCountList(StringUtils.join(orderCountList, ','));
        orderReportVO.setValidOrderCountList(StringUtils.join(validOrderCountList, ','));
        orderReportVO.setTotalOrderCount(totalOrderCount);
        orderReportVO.setValidOrderCount(validOrderCount);
        //计算订单完成率，保留两位小数，避免除以0
        if (totalOrderCount == 0) {
            orderReportVO.setOrderCompletionRate(0.0);
        } else {
            orderReportVO.setOrderCompletionRate(validOrderCount.doubleValue() / totalOrderCount);
        }

        return orderReportVO;
    }

    @Override
    public SalesTop10ReportVO salesTop10(LocalDate begin, LocalDate end) {
        List<String> nameList = new ArrayList<>();
        List<Integer> numberList = new ArrayList<>();
        Map map = new HashMap();
        //设置开始时间
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        map.put("beginTime", beginTime);
        //设置结束时间
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        map.put("endTime", endTime);
        //查询销量前十的商品
        List<GoodsSalesDTO> goodsSalesDTOList = orderMapper.salesTop10(map);
        for (GoodsSalesDTO goodsSalesDTO : goodsSalesDTOList) {
            nameList.add(goodsSalesDTO.getName());
            numberList.add(goodsSalesDTO.getNumber());
        }
        SalesTop10ReportVO salesTop10ReportVO = new SalesTop10ReportVO();
        salesTop10ReportVO.setNameList(StringUtils.join(nameList, ','));
        salesTop10ReportVO.setNumberList(StringUtils.join(numberList, ','));
        return salesTop10ReportVO;
    }
}
