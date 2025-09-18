package com.sky.controller.admin;


import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * 店铺管理
 */
@RestController
@RequestMapping("/admin/shop")
@Api(tags = "店铺相关接口")
@Slf4j
public class AdminShopController {

    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * 设置店铺营业状态
     */
    @PutMapping("/{status}")
    @ApiOperation("设置店铺营业状态")
    public Result setShopStatus(@PathVariable Integer status){
        log.info("设置店铺营业状态: {}", status);
        //将店铺状态存入Redis
        redisTemplate.opsForValue().set("shop_status", status);
        return Result.success();
    }

    /**
     * 查询店铺营业状态
     */
    @GetMapping("/status")
    @ApiOperation("查询店铺营业状态")
    public Result<Integer> getShopStatus(){
        log.info("查询店铺营业状态");
        Integer status = (Integer) redisTemplate.opsForValue().get("shop_status");
        return Result.success(status);
    }
}
