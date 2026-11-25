package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

// 实现一个拦截器
@Component
public class LoginInterceptor implements HandlerInterceptor {
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1:从请求头中获取session
        HttpSession session = request.getSession();
        // 2:从session中获取用户
        User user=(User) session.getAttribute("user");
        if (user==null) {
            // 3:如果用户不存在，则返回未登录结果
            response.setStatus(401);  // 未登录状态码,这是你自己规定的状态码，方便前端判断知道自己没有登录
            return false;
        }
        // 4:如果用户存在，则放行

        // 5:把用户放在ThreadLocal中
        // 使用user利用复制的方法创建一个userdto
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        UserHolder.saveUser(userDTO);
        return true;
    }

    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

        UserHolder.removeUser();
    }
}
