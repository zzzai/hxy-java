package cn.iocoder.yudao.module.member.controller.app.compat.crmeb;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.member.controller.app.auth.vo.AppAuthLoginRespVO;
import cn.iocoder.yudao.module.member.service.auth.MemberAuthService;
import cn.iocoder.yudao.module.system.enums.social.SocialTypeEnum;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrmebFrontWechatCompatControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private CrmebFrontWechatCompatController controller;

    @Mock
    private MemberAuthService memberAuthService;

    @Test
    void shouldProgramLoginSuccess() {
        AppAuthLoginRespVO loginRespVO = new AppAuthLoginRespVO();
        loginRespVO.setUserId(1024L);
        loginRespVO.setAccessToken("token_abc");
        loginRespVO.setOpenid("openid_1024");
        when(memberAuthService.socialLogin(any())).thenReturn(loginRespVO);

        CrmebFrontWechatCompatController.CrmebProgramLoginReqVO reqVO =
                new CrmebFrontWechatCompatController.CrmebProgramLoginReqVO();
        reqVO.setNickName("微信用户");
        CrmebCompatResult<CrmebFrontWechatCompatController.CrmebLoginRespVO> result =
                controller.programLogin("wx_code_001", reqVO);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals("login", result.getData().getType());
        assertEquals("token_abc", result.getData().getToken());
        assertEquals(1024L, result.getData().getUid());
        assertEquals("openid_1024", result.getData().getOpenid());

        ArgumentCaptor<cn.iocoder.yudao.module.member.controller.app.auth.vo.AppAuthSocialLoginReqVO> captor =
                ArgumentCaptor.forClass(cn.iocoder.yudao.module.member.controller.app.auth.vo.AppAuthSocialLoginReqVO.class);
        verify(memberAuthService).socialLogin(captor.capture());
        assertEquals(SocialTypeEnum.WECHAT_MINI_PROGRAM.getType(), captor.getValue().getType());
        assertEquals("wx_code_001", captor.getValue().getCode());
        assertEquals("crmeb-compat-miniapp-login", captor.getValue().getState());
    }

    @Test
    void shouldProgramLoginRejectBlankCode() {
        CrmebCompatResult<CrmebFrontWechatCompatController.CrmebLoginRespVO> result =
                controller.programLogin("  ", new CrmebFrontWechatCompatController.CrmebProgramLoginReqVO());
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("code"));
    }

    @Test
    void shouldProgramLoginReturnServiceExceptionMessage() {
        when(memberAuthService.socialLogin(any())).thenThrow(new ServiceException(500, "微信登录失败"));
        CrmebCompatResult<CrmebFrontWechatCompatController.CrmebLoginRespVO> result =
                controller.programLogin("wx_code_002", new CrmebFrontWechatCompatController.CrmebProgramLoginReqVO());
        assertEquals(500, result.getCode());
        assertEquals("微信登录失败", result.getMessage());
    }
}

