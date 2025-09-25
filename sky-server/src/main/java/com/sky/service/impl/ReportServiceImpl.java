package com.sky.service.impl;

import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
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
}
