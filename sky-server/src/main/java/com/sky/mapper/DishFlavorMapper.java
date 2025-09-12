package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.DishFlavor;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DishFlavorMapper {
    /**
     * 新增菜品口味
     */
    void insertBatch(List<DishFlavor> dishFlavorList);

    /**
     * 根据菜品id删除对应的口味数据
     * @param ids
     */
    void deleteByDishIds(List<Long> ids);
}
