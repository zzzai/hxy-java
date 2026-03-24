package cn.iocoder.yudao.module.member.controller.app.tag;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.member.controller.app.tag.vo.AppMemberTagRespVO;
import cn.iocoder.yudao.module.member.dal.dataobject.tag.MemberTagDO;
import cn.iocoder.yudao.module.member.dal.dataobject.user.MemberUserDO;
import cn.iocoder.yudao.module.member.service.tag.MemberTagService;
import cn.iocoder.yudao.module.member.service.user.MemberUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "用户 App - 会员标签")
@RestController
@RequestMapping("/member/tag")
@Validated
public class AppMemberTagController {

    @Resource
    private MemberUserService memberUserService;
    @Resource
    private MemberTagService memberTagService;

    @GetMapping("/my")
    @Operation(summary = "获得当前用户标签")
    public CommonResult<List<AppMemberTagRespVO>> getMyTags() {
        MemberUserDO user = memberUserService.getUser(getLoginUserId());
        if (user == null || user.getTagIds() == null || user.getTagIds().isEmpty()) {
            return success(Collections.emptyList());
        }

        List<MemberTagDO> tags = new ArrayList<>(memberTagService.getTagList(user.getTagIds()));
        tags.sort(Comparator.comparingInt(tag -> user.getTagIds().indexOf(tag.getId())));

        List<AppMemberTagRespVO> result = new ArrayList<>(tags.size());
        for (MemberTagDO tag : tags) {
            AppMemberTagRespVO respVO = new AppMemberTagRespVO();
            respVO.setId(tag.getId());
            respVO.setName(tag.getName());
            result.add(respVO);
        }
        return success(result);
    }
}
