package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService {
    /**
     * 新增套餐，同时保存套餐菜品的关联关系
     * @param setmealDTO
     */
    void saveWithDish(SetmealDTO setmealDTO);

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    PageResult page(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 删除套餐，同时删除套餐和菜品的关联关系
     * @param ids
     */
    void delete(List<Long> ids);

    /**
     * 修改套餐，同时修改套餐和菜品的关联关系
     * @param setmealDTO
     */
    void updateWithDish(SetmealDTO setmealDTO);

    /**
     * 根据id查询套餐信息和对应的菜品信息
     * @param id
     * @return
     */
    SetmealVO getByIdWithDish(Long id);
}
