package com.zbkj.common.interceptor;

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Base64;

/**
 *  Swagger 文档
 *  +----------------------------------------------------------------------
 *  | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
 *  +----------------------------------------------------------------------
 *  | Copyright (c) 2016~2025 https://www.crmeb.com All rights reserved.
 *  +----------------------------------------------------------------------
 *  | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
 *  +----------------------------------------------------------------------
 *  | Author: CRMEB Team <admin@crmeb.com>
 *  +----------------------------------------------------------------------
 */
public class SwaggerInterceptor extends HandlerInterceptorAdapter {
    private String username;
    private String password;
    private Boolean check;
    public SwaggerInterceptor(String username, String password, Boolean check) {
        this.username = username;
        this.password = password;
        this.check = check;
    }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authorization = request.getHeader("Authorization");
        boolean isAuthSuccess = httpBasicAuth(authorization);
        if (!isAuthSuccess) {
            response.setCharacterEncoding("utf-8");
            response.setStatus(401);
//            response.setStatus(401,"Unauthorized");
            response.setHeader("WWW-authenticate", "Basic realm=\"Realm\"");
            try (PrintWriter writer = response.getWriter()) {
                writer.print("Forbidden, unauthorized user");
            }
        }
        return isAuthSuccess;
    }
    public boolean httpBasicAuth(String authorization) throws IOException {
        if(check){
            if (authorization != null && authorization.split(" ").length == 2) {
                String userAndPass = new String(Base64.getDecoder().decode(authorization.split(" ")[1]));
                String username = userAndPass.split(":").length == 2 ? userAndPass.split(":")[0] : null;
                String password = userAndPass.split(":").length == 2 ? userAndPass.split(":")[1] : null;
                return this.username.equals(username) && this.password.equals(password);
            }
            return false;
        }
        return true;
    }
}
