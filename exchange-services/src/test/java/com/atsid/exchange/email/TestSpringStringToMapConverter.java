package com.atsid.exchange.email;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext-test.xml" })
public class TestSpringStringToMapConverter {
    @Autowired
    private SpringStringToMapConverter converter;

    @Test
    public void testConvertNullString() {
        Map<String, String> result = converter.convert(null);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testConvertEmptyString() {
        Map<String, String> result = converter.convert("");

        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testConvertString() {
        Map<String, String> result = converter.convert("a:b,c:d , e:f");

        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("b", result.get("a"));
        Assert.assertEquals("d", result.get("c"));
        Assert.assertEquals("f", result.get("e"));
    }

    @Test(expected = IllegalStateException.class)
    public void testConvertStringMalformedEntryNoValue() {
        converter.convert("a:b,c");
    }

    @Test(expected = IllegalStateException.class)
    public void testConvertStringMalformedEntryMultipleColon() {
        converter.convert("a:b:d,c:d");
    }
}