package com.zbkj.common.utils;

import org.junit.Assert;
import org.junit.Test;

public class ConfigSwitchUtilTest {

    @Test
    public void isOnShouldSupportOneAndQuotedOne() {
        Assert.assertTrue(ConfigSwitchUtil.isOn("1"));
        Assert.assertTrue(ConfigSwitchUtil.isOn(" '1' "));
        Assert.assertTrue(ConfigSwitchUtil.isOn("\"1\""));
    }

    @Test
    public void isOnShouldSupportBooleanLikeValues() {
        Assert.assertTrue(ConfigSwitchUtil.isOn("true"));
        Assert.assertTrue(ConfigSwitchUtil.isOn(" ON "));
        Assert.assertTrue(ConfigSwitchUtil.isOn("yes"));
    }

    @Test
    public void isOnShouldReturnFalseForEmptyOrZero() {
        Assert.assertFalse(ConfigSwitchUtil.isOn(null));
        Assert.assertFalse(ConfigSwitchUtil.isOn(""));
        Assert.assertFalse(ConfigSwitchUtil.isOn("0"));
        Assert.assertFalse(ConfigSwitchUtil.isOn("'0'"));
    }

    @Test
    public void normalizeShouldStripWrappingQuotesAndTrim() {
        Assert.assertEquals("1", ConfigSwitchUtil.normalize("  '1' "));
        Assert.assertEquals("abc", ConfigSwitchUtil.normalize("  \"abc\" "));
        Assert.assertEquals("nested", ConfigSwitchUtil.normalize("  \"'nested'\" "));
    }

    @Test
    public void normalize01ShouldMapToZeroOrOne() {
        Assert.assertEquals("1", ConfigSwitchUtil.normalize01(" yes "));
        Assert.assertEquals("0", ConfigSwitchUtil.normalize01("unknown"));
    }
}
