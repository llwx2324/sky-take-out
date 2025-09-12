package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        //属性拷贝
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.insert(dish);
        Long dishId = dish.getId();//主键返回
        //菜品口味
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(flavor -> {
                flavor.setDishId(dishId);
            });
            dishFlavorMapper.insertBatch(flavors);
        }


    }

    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> list = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(list.getTotal(), list.getResult());
    }

    @Override
    @Transactional
    public void delete(List<Long> ids) {
        //起售中的菜品不能删除以及被套餐关联的菜品不能删除
        for (Long id : ids) {
            List<SetmealDish> list= setmealDishMapper.getByDishId(id);
            if(list!=null && !list.isEmpty()){
                throw new DeletionNotAllowedException("菜品被套餐关联，不能删除");
            }
            Dish dish = dishMapper.getById(id);
            if (dish == null) {
                continue;
            }
            if (dish.getStatus() == 1) {
                throw new DeletionNotAllowedException("启售中的菜品不能删除");
            }
        }
        dishMapper.delete(ids);
        //删除口味数据
        dishFlavorMapper.deleteByDishIds(ids);
    }

    @Override
    @Transactional
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        //属性拷贝
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.update(dish);
        Long dishId = dish.getId();
        //删除原有口味数据
        dishFlavorMapper.deleteByDishIds(List.of(dishId));
        //新增口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(flavor -> {
                flavor.setDishId(dishId);
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }


    @Override
    public DishVO getByIdWithFlavor(Long id) {
        Dish dish = dishMapper.getById(id);
        if (dish == null) {
            return null;
        }
        DishVO dishVO = new DishVO();
        //属性拷贝
        BeanUtils.copyProperties(dish, dishVO);
        //查询口味数据
        List<DishFlavor> flavors = dishFlavorMapper.getByDishId(id);
        dishVO.setFlavors(flavors);
        return dishVO;
    }

    @Override
    public void modifyStatus(Integer status, Long id) {
        Dish dish = new Dish();
        dish.setId(id);
        dish.setStatus(status);
        //若status为0：菜品停售，菜品所对应的套餐也要停售
        if (status == 0) {
            List<SetmealDish> list = setmealDishMapper.getByDishId(id);
            //获取套餐菜品关系列表中的所有套餐id
            List<Long> setmealIds = new ArrayList<>();
            if (list != null && !list.isEmpty()) {
                setmealIds = list.stream().map(SetmealDish::getSetmealId).distinct().toList();
            }
            //根据套餐id创建状态为0的套餐对象
            if (setmealIds.isEmpty()) {
                setmealIds.forEach(sid -> {;
                    Setmeal setmeal = new Setmeal();
                    setmeal.setId(sid);
                    setmeal.setStatus(0);
                    setmealMapper.update(setmeal);
                });
            }

        }
        dishMapper.update(dish);
    }

}
