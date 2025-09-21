package com.sky.mapper;


import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {

    /**
     * 查询购物车列表
     * @param shoppingCart
     * @return
     */
    List<ShoppingCart> list(ShoppingCart shoppingCart);

    /**
     * 修改购物车商品
     * @param cart
     */
    void update(ShoppingCart cart);

    /**
     * 新增购物车商品
     * @param shoppingCart
     */
    @Insert("INSERT INTO shopping_cart (user_id, name, image, dish_id, setmeal_id, dish_flavor, number, amount, create_time) " +
            "VALUES (#{userId}, #{name}, #{image}, #{dishId}, #{setmealId}, #{dishFlavor}, #{number}, #{amount}, #{createTime})")
    void insert(ShoppingCart shoppingCart);

    /**
     * 根据用户id删除购物车商品
     */
    @Delete("DELETE FROM shopping_cart WHERE user_id = #{userId}")
    void deleteByUserId(Long userId);

    /**
     * 根据id删除购物车商品
     * @param id
     */
    @Delete("DELETE FROM shopping_cart WHERE id = #{id}")
    void deleteById(Long id);
}
