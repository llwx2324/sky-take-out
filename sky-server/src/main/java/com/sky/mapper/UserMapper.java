package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.xmlbeans.impl.xb.xmlconfig.Extensionconfig;

import java.util.Map;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM user WHERE openid = #{openid}")
    User getByOpenid(String openid);

    void insert(User user);

    @Select("SELECT * FROM user WHERE id = #{userId}")
    User getById(Long userId);

    /**
     * 根据map中的条件统计用户数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}
