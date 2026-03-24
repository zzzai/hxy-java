package cn.iocoder.yudao.module.promotion.dal.mysql.giftcard;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.promotion.controller.app.giftcard.AppGiftCardTemplatePageReqVO;
import cn.iocoder.yudao.module.promotion.dal.dataobject.giftcard.GiftCardTemplateDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface GiftCardTemplateMapper extends BaseMapperX<GiftCardTemplateDO> {

    default PageResult<GiftCardTemplateDO> selectAppPage(AppGiftCardTemplatePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<GiftCardTemplateDO>()
                .eqIfPresent(GiftCardTemplateDO::getStatus, parseStatus(reqVO.getStatus()))
                .orderByDesc(GiftCardTemplateDO::getId));
    }

    @Update("UPDATE gift_card_template SET stock = stock + #{delta} WHERE id = #{id}")
    int updateStock(@Param("id") Long id, @Param("delta") int delta);

    default Integer parseStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        if ("ENABLE".equalsIgnoreCase(status.trim())) {
            return CommonStatusEnum.ENABLE.getStatus();
        }
        if ("DISABLE".equalsIgnoreCase(status.trim())) {
            return CommonStatusEnum.DISABLE.getStatus();
        }
        return null;
    }
}
