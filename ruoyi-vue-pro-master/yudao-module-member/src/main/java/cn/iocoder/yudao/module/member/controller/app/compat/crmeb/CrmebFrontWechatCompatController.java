package cn.iocoder.yudao.module.member.controller.app.compat.crmeb;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.member.controller.app.auth.vo.AppAuthLoginRespVO;
import cn.iocoder.yudao.module.member.controller.app.auth.vo.AppAuthSocialLoginReqVO;
import cn.iocoder.yudao.module.member.service.auth.MemberAuthService;
import cn.iocoder.yudao.module.system.enums.social.SocialTypeEnum;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;

/**
 * CRMEB 前台微信接口兼容层（P0）
 */
@RestController
@RequestMapping("/api/front/wechat")
@Validated
@Hidden
@Slf4j
public class CrmebFrontWechatCompatController {

    /**
     * 兼容层默认 state。允许请求体传入 state 覆盖。
     */
    private static final String COMPAT_DEFAULT_STATE = "crmeb-compat-miniapp-login";

    @Resource
    private MemberAuthService memberAuthService;

    @PostMapping("/authorize/program/login")
    @PermitAll
    public CrmebCompatResult<CrmebLoginRespVO> programLogin(
            @RequestParam("code") String code,
            @RequestBody(required = false) CrmebProgramLoginReqVO reqVO) {
        if (StrUtil.isBlank(code)) {
            return CrmebCompatResult.failed("code 不能为空");
        }
        try {
            AppAuthSocialLoginReqVO loginReqVO = new AppAuthSocialLoginReqVO();
            loginReqVO.setType(SocialTypeEnum.WECHAT_MINI_PROGRAM.getType());
            loginReqVO.setCode(code);
            loginReqVO.setState(resolveState(reqVO));

            AppAuthLoginRespVO loginRespVO = memberAuthService.socialLogin(loginReqVO);
            CrmebLoginRespVO respVO = new CrmebLoginRespVO();
            respVO.setType("login");
            respVO.setToken(loginRespVO.getAccessToken());
            respVO.setUid(loginRespVO.getUserId());
            respVO.setOpenid(loginRespVO.getOpenid());
            return CrmebCompatResult.success(respVO);
        } catch (ServiceException e) {
            log.warn("[crmeb-wechat-program-login][code({}) req({}) 登录失败]", code, reqVO, e);
            return CrmebCompatResult.failed(e.getMessage());
        } catch (Exception e) {
            log.warn("[crmeb-wechat-program-login][code({}) req({}) 登录异常]", code, reqVO, e);
            return CrmebCompatResult.failed("登录失败");
        }
    }

    private String resolveState(CrmebProgramLoginReqVO reqVO) {
        if (reqVO == null || StrUtil.isBlank(reqVO.getState())) {
            return COMPAT_DEFAULT_STATE;
        }
        return reqVO.getState().trim();
    }

    @Data
    public static class CrmebProgramLoginReqVO {
        private String type;
        @JsonProperty("spread_spid")
        private Integer spreadSpid;
        private String avatar;
        @JsonProperty("nickName")
        private String nickName;
        private String state;
    }

    @Data
    public static class CrmebLoginRespVO {
        /**
         * CRMEB 协议字段：login/register/start
         */
        private String type;
        private String token;
        private Long uid;
        private String openid;
    }
}
