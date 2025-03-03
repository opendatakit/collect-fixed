package org.odk.collect.openrosa.http;

import org.junit.Assert;
import org.junit.Test;

public class CaseInsensitiveEmptyHeadersTest {
    private final CaseInsensitiveHeaders headers = new CaseInsensitiveEmptyHeaders();

    @Test
    public void testGetHeaders() {
        Assert.assertEquals(0, headers.getHeaders().size());
    }

    @Test
    public void testContainsHeader() {
        Assert.assertFalse(headers.containsHeader(""));
    }

    @Test
    public void testNullHeaderLookup() {
        Assert.assertFalse(headers.containsHeader(null));
    }

    @Test
    public void testGetAnyValue() {
        Assert.assertNull(headers.getAnyValue(""));
    }

    @Test
    public void testGetValues() {
        Assert.assertNull(headers.getValues(""));
    }
}

