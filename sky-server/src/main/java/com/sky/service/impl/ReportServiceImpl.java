package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
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
    @Autowired
    private WorkspaceService workspaceService;


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

    @Override
    public void export(HttpServletResponse response) {
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);
        BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX));
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {

            //查询近30天的总的营业数据写入模板文件
            XSSFWorkbook workbook = new XSSFWorkbook(is);
            //获取第一个工作表
            var sheet = workbook.getSheetAt(0);
            //获取第二行第二列 -- 时间段
            sheet.getRow(1).getCell(1).setCellValue("时间：" + begin + "至" + end);
            //获取第四行第三列 -- 营业额
            sheet.getRow(3).getCell(2).setCellValue(businessData.getTurnover());
            //获取第五行第三列 -- 有效订单
            sheet.getRow(4).getCell(2).setCellValue(businessData.getValidOrderCount());
            //获取第四行第五列 -- 订单完成率
            sheet.getRow(3).getCell(4).setCellValue(businessData.getOrderCompletionRate());
            //获取第五行第五列 -- 平均客单价
            sheet.getRow(4).getCell(4).setCellValue(businessData.getUnitPrice());
            //获取第四行第七列 -- 新增用户数
            sheet.getRow(3).getCell(6).setCellValue(businessData.getNewUsers());

            //写具体到每天的数据
            for(int i = 0; i < 30; i++){
                LocalDate date = begin.plusDays(i);
                BusinessDataVO dailyData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                //第8行开始是具体数据
                var row = sheet.getRow(7 + i);
                //第二列 日期
                row.getCell(1).setCellValue(date.toString());
                //第三列 营业额
                row.getCell(2).setCellValue(dailyData.getTurnover());
                //第四列 有效订单
                row.getCell(3).setCellValue(dailyData.getValidOrderCount());
                //第五列 订单完成率
                row.getCell(4).setCellValue(dailyData.getOrderCompletionRate());
                //第六列 平均客单价
                row.getCell(5).setCellValue(dailyData.getUnitPrice());
                //第七列 新增用户数
                row.getCell(6).setCellValue(dailyData.getNewUsers());
            }

            workbook.write(response.getOutputStream());//将文件写入响应输出流

            //关闭资源
            workbook.close();
            is.close();

        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
