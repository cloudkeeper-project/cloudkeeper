package com.svbio.cloudkeeper.model.util;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;

public class ImmutableListTest {
    @Test
    public void subListTestBadArguments() {
        ImmutableList<Integer> list = ImmutableList.copyOf(Arrays.asList(1, 2, 3, 4));

        try {
            list.subList(0, 5);
            Assert.fail();
        } catch (IndexOutOfBoundsException ignored) { }

        try {
            list.subList(-1, 2);
            Assert.fail();
        } catch (IndexOutOfBoundsException ignored) { }

        try {
            list.subList(2, 1);
            Assert.fail();
        } catch (IllegalArgumentException ignored) { }
    }

    @Test
    public void subListTest() {
        ImmutableList<Integer> list = ImmutableList.copyOf(Arrays.asList(1, 2, 3, 4, 5));

        Assert.assertTrue(list.subList(2, 2) instanceof ImmutableList.EmptyList<?>);

        ImmutableList<Integer> singletonList = list.subList(2, 3);
        Assert.assertTrue(singletonList instanceof ImmutableList.SingletonList<?>);
        Assert.assertEquals(singletonList, Collections.singletonList(3));

        ImmutableList<Integer> smallList = list.subList(2, 5);
        Assert.assertTrue(smallList instanceof ImmutableList.SubList<?>);
        Assert.assertEquals(smallList, Arrays.asList(3, 4, 5));


        Assert.assertTrue(smallList.subList(1, 1) instanceof ImmutableList.EmptyList<?>);

        ImmutableList<Integer> singletonSublist = smallList.subList(1, 2);
        Assert.assertTrue(singletonSublist instanceof ImmutableList.SingletonList<?>);
        Assert.assertEquals(singletonSublist, Collections.singletonList(4));

        ImmutableList<Integer> smallSublist = smallList.subList(1, 3);
        Assert.assertTrue(smallSublist instanceof ImmutableList.SubList<?>);
        Assert.assertEquals(smallSublist, Arrays.asList(4, 5));
    }
}
