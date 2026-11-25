package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

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

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1. 校验手机号--去网上找正则表达式，但是本项目提供好了正则表达式，直接使用就好
        if(RegexUtils.isPhoneInvalid(phone)){
            // 2. 手机号不合适，返回错误信息
            return Result.fail("手机号格式不正确");

        }
        // 3. 手机号合适，生成验证码
        String code= RandomUtil.randomNumbers(6);
        // 4. 把验证码保存起来，方便验证，保存验证码到Session
        session.setAttribute("code", code);
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

        Object sessionCode = session.getAttribute("code");
        if (sessionCode == null || !sessionCode.equals(code)) {
            // 4. 验证码不合适，返回错误信息
            return Result.fail("验证码错误");
        }
        // 5.判断用户是否存在
        User user = query().eq("phone", phone).one();
        if (user == null) {
            // 6.用户不存在那我就创建一个用户，实现登录注册一体化
            user=createUserWithPhone(phone);
        }
        // 7.用户存在放到session里面
        session.setAttribute("user", user);
        return Result.ok();
    }
    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName("user" +RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
