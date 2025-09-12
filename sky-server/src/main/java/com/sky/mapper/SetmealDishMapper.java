package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 根据菜品id查询套餐菜品关联数据
     */
    @Select("select * from setmeal_dish where dish_id = #{dishId}")
    List<SetmealDish> getByDishId(Long dishId);
}
