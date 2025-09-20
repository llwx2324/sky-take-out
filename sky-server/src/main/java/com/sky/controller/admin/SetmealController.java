package com.sky.controller.admin;


import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/setmeal")
@Slf4j
@Api(tags = "套餐相关接口")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    /**
     * 新增套餐
     */
    @PostMapping
    @ApiOperation("新增套餐")
    @CacheEvict(cacheNames = "setmealCache", key = "#setmealDTO.categoryId") // 清除套餐缓存
    public Result save(@RequestBody SetmealDTO setmealDTO ) {
        log.info("新增套餐:{}", setmealDTO);
        setmealService.saveWithDish(setmealDTO);
        return Result.success();
    }

    /**
     * 分页查询套餐
     */
    @GetMapping("/page")
    @ApiOperation("分页查询套餐")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO) {
        log.info("分页查询套餐");
        PageResult pageResult = setmealService.page(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * （批量）删除套餐
     */
    @DeleteMapping
    @ApiOperation("（批量）删除套餐")
    @CacheEvict(cacheNames = "setmealCache", allEntries = true) // 清除所有套餐缓存
    public Result delete(@RequestParam("ids") List<Long> ids) {
        log.info("删除套餐:{}", ids);
        setmealService.delete(ids);
        return Result.success();
    }

    /**
     * 根据id查询套餐信息和对应的菜品信息
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询套餐信息")
    public Result<SetmealVO> getById(@PathVariable Long id) {
        log.info("根据id查询套餐信息:{}", id);
        SetmealVO setmealVO = setmealService.getByIdWithDish(id);
        return Result.success(setmealVO);
    }

    /**
     * 修改套餐
     */
    @PutMapping
    @ApiOperation("修改套餐")
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result update(@RequestBody SetmealDTO setmealDTO) {
        log.info("修改套餐:{}", setmealDTO);
        setmealService.updateWithDish(setmealDTO);
        return Result.success();
    }

    /**
     * 修改套餐售卖状态：停售/起售
     */
    @PostMapping("/status/{status}")
    @ApiOperation("修改套餐售卖状态")
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result modifyStatus(@PathVariable Integer status, Integer id) {
        log.info("修改套餐售卖状态：{},{}", status, id);
        setmealService.modifyStatus(status, id);
        return Result.success();
    }
}
