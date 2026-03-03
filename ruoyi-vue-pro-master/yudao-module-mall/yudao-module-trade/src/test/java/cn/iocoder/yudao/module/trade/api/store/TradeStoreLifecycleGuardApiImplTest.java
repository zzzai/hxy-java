package cn.iocoder.yudao.module.trade.api.store;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.trade.api.store.dto.TradeStoreLifecycleGuardStatRespDTO;
import cn.iocoder.yudao.module.trade.dal.mysql.aftersale.AfterSaleMapper;
import cn.iocoder.yudao.module.trade.dal.mysql.order.TradeOrderMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class TradeStoreLifecycleGuardApiImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private TradeStoreLifecycleGuardApiImpl api;

    @Mock
    private TradeOrderMapper tradeOrderMapper;
    @Mock
    private AfterSaleMapper afterSaleMapper;

    @Test
    void getStoreLifecycleGuardStat_shouldReturnPendingOrderAndInflightTicketCount() {
        when(tradeOrderMapper.selectCountByPickUpStoreIdAndStatuses(eq(88L), any())).thenReturn(5L);
        when(afterSaleMapper.selectCountByPickUpStoreIdAndStatuses(eq(88L), any())).thenReturn(2L);

        TradeStoreLifecycleGuardStatRespDTO resp = api.getStoreLifecycleGuardStat(88L);

        assertEquals(5L, resp.getPendingOrderCount());
        assertEquals(2L, resp.getInflightTicketCount());
    }

}
