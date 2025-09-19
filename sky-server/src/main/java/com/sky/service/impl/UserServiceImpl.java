package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.sky.entity.User;
import com.sky.mapper.UserMapper;
import com.sky.properties.JwtProperties;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.JwtUtil;
import com.sky.vo.UserLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    public static final String WX_LOGIN_URL = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public UserLoginVO login(String code) {
        //获取openid
        String openid = getOpenId(code);
        //查有无此用户
        User user = userMapper.getByOpenid(openid);
        if (user == null) {
            //没有则注册
            user = new User();
            user.setOpenid(openid);
            user.setCreateTime(LocalDateTime.now());
            userMapper.insert(user);
        }
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());

        //获取JWT令牌
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);

        UserLoginVO userLoginVO = new UserLoginVO();
        userLoginVO.setId(user.getId());
        userLoginVO.setOpenid(openid);
        userLoginVO.setToken(token);
        return userLoginVO;
    }

    //调用微信提供的接口，获取openid
    private String getOpenId(String code) {
        Map<String,String> map = new HashMap<>();
        map.put("appid",weChatProperties.getAppid());
        map.put("secret",weChatProperties.getSecret());
        map.put("js_code",code);
        map.put("grant_type","authorization_code");
        String response = HttpClientUtil.doGet(WX_LOGIN_URL, map);
        // 返回的response是：String类型的：{"session_key":"...","openid":"...","unionid":"...","errcode":0,"errmsg":"ok"}
        log.info("微信登录响应:{}",response);
        //解析响应数据
        JSONObject jsonObject = JSONObject.parseObject(response);
        String openid = jsonObject.getString("openid");
        return openid;
    }
}
