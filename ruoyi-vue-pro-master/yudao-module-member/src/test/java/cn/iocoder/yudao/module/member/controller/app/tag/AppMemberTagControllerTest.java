package cn.iocoder.yudao.module.member.controller.app.tag;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.member.controller.app.tag.vo.AppMemberTagRespVO;
import cn.iocoder.yudao.module.member.dal.dataobject.tag.MemberTagDO;
import cn.iocoder.yudao.module.member.dal.dataobject.user.MemberUserDO;
import cn.iocoder.yudao.module.member.service.tag.MemberTagService;
import cn.iocoder.yudao.module.member.service.user.MemberUserService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppMemberTagControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AppMemberTagController controller;

    @Mock
    private MemberUserService memberUserService;
    @Mock
    private MemberTagService memberTagService;

    @Test
    void getMyTags_shouldResolveCurrentUserTags() {
        MemberUserDO user = new MemberUserDO();
        user.setId(77L);
        user.setTagIds(Arrays.asList(1L, 2L));
        when(memberUserService.getUser(77L)).thenReturn(user);
        when(memberTagService.getTagList(Arrays.asList(1L, 2L))).thenReturn(Arrays.asList(
                MemberTagDO.builder().id(1L).name("高复购").build(),
                MemberTagDO.builder().id(2L).name("社群活跃").build()
        ));

        try (MockedStatic<SecurityFrameworkUtils> mockedStatic = mockStatic(SecurityFrameworkUtils.class)) {
            mockedStatic.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(77L);

            CommonResult<List<AppMemberTagRespVO>> result = controller.getMyTags();

            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
            assertEquals(2, result.getData().size());
            assertEquals("高复购", result.getData().get(0).getName());
            assertEquals("社群活跃", result.getData().get(1).getName());
        }

        verify(memberUserService).getUser(77L);
        verify(memberTagService).getTagList(Arrays.asList(1L, 2L));
    }
}
