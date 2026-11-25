package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1. 校验手机号--去网上找正则表达式，但是本项目提供好了正则表达式，直接使用就好
        if(RegexUtils.isPhoneInvalid(phone)){
            // 2. 手机号不合适，返回错误信息
            return Result.fail("手机号格式不正确");

        }
        // 3. 手机号合适，生成验证码
        String code= RandomUtil.randomNumbers(6);
        // 4. 把验证码保存起来，方便验证，保存验证码到Redis;
        // redis 使用string类型，以手机号为键，有效期为两分钟

        stringRedisTemplate.opsForValue().set("login:code"+phone, code, 2, TimeUnit.MINUTES);

        // 5. 发送验证码
        log.debug("发送验证码成功，验证码："+code);
        // 返回结果
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1.校验手机号
        String phone = loginForm.getPhone();
        if(RegexUtils.isPhoneInvalid(phone)){
            // 2. 手机号不合适，返回错误信息
            return Result.fail("手机号格式不正确");
        }
        // 3.校验验证码
        String code = loginForm.getCode();
        String redisCode = stringRedisTemplate.opsForValue().get("login:code" + phone);
        if (redisCode == null || !redisCode.equals(code)) {
            // 4. 验证码不合适，返回错误信息
            return Result.fail("验证码错误");
        }
        // 5.判断用户是否存在
        User user = query().eq("phone", phone).one();
        if (user == null) {
            // 6.用户不存在那我就创建一个用户，实现登录注册一体化
            user=createUserWithPhone(phone);
        }
        // 7.用户存在放到Redis里面
        //7.1 生成随机的token，一是为了作为登录令牌，二是为了作为Redis的键
        String token = UUID.randomUUID().toString();
        // 7.2 将user对象转为Map对象，随后存储到redis中的哈希数据结构中
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        // 7.3 存储到redis的哈希数据结构中
        stringRedisTemplate.opsForHash().putAll("login:token:" + token, userMap);
        // 7.4 设置redis的过期时间
        stringRedisTemplate.expire("login:token:" + token, 30, TimeUnit.MINUTES);
        // 7.5 向前端返回token确保前端每次访问都会携带这个token
        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName("user" +RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
