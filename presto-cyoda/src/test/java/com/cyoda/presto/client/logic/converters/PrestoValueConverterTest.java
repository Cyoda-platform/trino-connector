package com.cyoda.presto.client.logic.converters;

import org.testng.annotations.Test;

public class PrestoValueConverterTest {
    @Test
    public void testStringify(){
        for (ValueHolder<?> valueHolder : ValueHolder.MAP.values()){
            String s = valueHolder.testStringify();
            assert s != null && s.length() > 0;
            System.out.println(valueHolder.dataType + ": " + s);
        }
    }

    @Test
    public void testConversions(){
        for (ValueHolder<?> valueHolder : ValueHolder.MAP.values()){
            if (!valueHolder.testSliceConversions() && !valueHolder.testLongConversions()){
                System.out.println("WARNING: No conversion test for dataType " + valueHolder.dataType);
            }
        }
    }
}
