package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    public static final String WX_LOGIN="https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private WeChatProperties properties;

    @Autowired
    private UserMapper userMapper;

    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {
        String openid = getOpenId(userLoginDTO.getCode());
        //判断openid是否为空，为空则用户登陆失败，抛出业务异常
        if(openid == null){
            throw new RuntimeException(MessageConstant.LOGIN_FAILED);
        }
        //判断当前用户是否为新用户
        User user=userMapper.getByOpenId(openid);
        if(user==null){
            //如果是新用户，自动完成注册
            user= User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }
        return user;
    }
    private String getOpenId(String code) {
        //调用微信接口服务，获得当前用户id
        Map<String,String> map=new HashMap<>();
        map.put("appid",properties.getAppid());
        map.put("secret",properties.getSecret());
        map.put("js_code",code);
        map.put("grant_type","authorization_code");

        String json=HttpClientUtil.doGet(WX_LOGIN,map);
        JSONObject jsonObject = JSON.parseObject(json);
        String openid=jsonObject.getString("openid");
        return openid;
    }
}
