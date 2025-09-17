package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.SetmealDeleteException;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Override
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {
        //新增套餐
        Setmeal setmeal = new Setmeal();
        //属性拷贝
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.insert(setmeal);
        Long id = setmeal.getId();//主键返回
        //新增套餐菜品关联关系
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes != null && setmealDishes.size() > 0) {
            setmealDishes.forEach(item -> {
                item.setSetmealId(id);
            });
            setmealDishMapper.insertBatch(setmealDishes);
        }

    }

    @Override
    public PageResult page(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> list = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(list.getTotal(), list.getResult());
    }

    @Override
    @Transactional
    public void delete(List<Long> ids) {
        //在售的套餐不能删除
        if(ids != null && ids.size() > 0){
            for (Long id : ids) {
                Setmeal setmeal = setmealMapper.getById(id);
                if(setmeal != null && setmeal.getStatus() == 1){
                    throw new SetmealDeleteException("套餐正在售卖中，不能删除");
                }
            }
            //删除套餐
            setmealMapper.deleteByIds(ids);
            //删除套餐和菜品的关联关系
            setmealDishMapper.deleteBySetmealIds(ids);

        }
    }
}
