package cn.iocoder.yudao.module.product.service.history;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.product.controller.admin.history.vo.ProductBrowseHistoryPageReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.history.ProductBrowseHistoryDO;
import cn.iocoder.yudao.module.product.dal.mysql.history.ProductBrowseHistoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;

/**
 * 商品浏览记录 Service 实现类
 *
 * @author owen
 */
@Service
@Validated
public class ProductBrowseHistoryServiceImpl implements ProductBrowseHistoryService {

    private static final int USER_STORE_MAXIMUM = 100;

    @Resource
    private ProductBrowseHistoryMapper browseHistoryMapper;

    @Override
    public void createBrowseHistory(Long userId, Long spuId) {
        // 用户未登录时不记录
        if (userId == null) {
            return;
        }

        // 情况一：同一个商品，只保留最新的一条记录
        ProductBrowseHistoryDO history = browseHistoryMapper.selectByUserIdAndSpuId(userId, spuId);
        if (history != null) {
            browseHistoryMapper.deleteById(history);
        } else {
            // 情况二：限制每个用户的浏览记录的条数
            // 先查询数量，只有超过限制时才查询并删除最早的记录
            Long count = browseHistoryMapper.selectCountByUserId(userId);
            if (count >= USER_STORE_MAXIMUM) {
                ProductBrowseHistoryDO oldest = browseHistoryMapper.selectOldestByUserId(userId);
                if (oldest != null) {
                    browseHistoryMapper.deleteById(oldest);
                }
            }
        }

        // 插入
        ProductBrowseHistoryDO browseHistory = new ProductBrowseHistoryDO()
                .setUserId(userId)
                .setSpuId(spuId);
        browseHistoryMapper.insert(browseHistory);
    }

    @Override
    public void hideUserBrowseHistory(Long userId, Collection<Long> spuIds) {
        browseHistoryMapper.updateUserDeletedByUserId(userId, spuIds, true);
    }

    @Override
    public PageResult<ProductBrowseHistoryDO> getBrowseHistoryPage(ProductBrowseHistoryPageReqVO pageReqVO) {
        return browseHistoryMapper.selectPage(pageReqVO);
    }

}