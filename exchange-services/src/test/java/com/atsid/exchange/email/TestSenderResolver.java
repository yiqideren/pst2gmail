package com.atsid.exchange.email;

import com.pff.PSTMessage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext-test.xml" })
public class TestSenderResolver {
    @Autowired
    private SenderResolver resolver;
    @Mock
    private PSTMessage pstMessage;
    private Map<String, String> resolverMap;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        resolverMap = resolver.getResolverMap();
    }

    @After
    public void tearDown() {
        resolver.setResolverMap(resolverMap);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResolveSenderNullMessage() {
        resolver.resolveSender(null);
    }

    @Test(expected = IllegalStateException.class)
    public void testResolveSenderNonExchangeType() {
        Mockito.when(pstMessage.getSenderAddrtype()).thenReturn("SMTP");

        resolver.resolveSender(pstMessage);
    }

    @Test
    public void testResolveSenderEmptyMap() {
        Mockito.when(pstMessage.getSenderAddrtype()).thenReturn("EX");
        Mockito.when(pstMessage.getSenderEmailAddress()).thenReturn("OU=abc/blah/cn=test.user");
        resolver.setResolverMap(new HashMap<String, String>());

        String resolved = resolver.resolveSender(pstMessage);

        Assert.assertNotNull(resolved);
        Assert.assertEquals("", resolved);
    }

    @Test
    public void testResolveSender() {
        Mockito.when(pstMessage.getSenderAddrtype()).thenReturn("EX");
        Mockito.when(pstMessage.getSenderEmailAddress()).thenReturn("OU=def/blah/cn=test.user");

        String resolved = resolver.resolveSender(pstMessage);

        Assert.assertNotNull(resolved);
        Assert.assertEquals("test.user@test.com", resolved);
    }

    @Test(expected = IllegalStateException.class)
    public void testResolveSenderNullMap() {
        Mockito.when(pstMessage.getSenderAddrtype()).thenReturn("EX");
        Mockito.when(pstMessage.getSenderEmailAddress()).thenReturn("OU=def/blah/cn=test.user");
        resolver.setResolverMap(null);

        resolver.resolveSender(pstMessage);
    }

    @Test
    public void testResolveSenderNonResolvable() {
        Mockito.when(pstMessage.getSenderAddrtype()).thenReturn("EX");
        Mockito.when(pstMessage.getSenderEmailAddress()).thenReturn("OU=ghi/blah/cn=test.user");

        String resolved = resolver.resolveSender(pstMessage);

        Assert.assertNotNull(resolved);
        Assert.assertEquals("", resolved);
    }
}