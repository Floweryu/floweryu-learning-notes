看下面代码及输出：
```java
public class Five {
    public static void main(String[] args) {
        Integer a = Integer.valueOf(3);
        Integer b = Integer.valueOf(3);
        System.out.println(a == b);     // true

        Integer c = Integer.valueOf(300000);
        Integer d = Integer.valueOf(300000);
        System.out.println(c == d);     // false
        System.out.println(c.equals(d));    // true
    }
}
```

为什么结果不一致？

`Integer.valueOf`源码如下：

如果传入参数`i`在`low`和`high`之间，也就是`[-128, 127]`，会直接返回`IntegerCache.cache`里面的数据，并不会创建一个新`Integer`对象。

当大于上述范围后，才会创建一个新的对象。新对象用`==`判断由于内存地址不同，是不相等的，但用`equals`判断由于值一样，所以返回`true`

```java
public static Integer valueOf(int i) {
    if (i >= IntegerCache.low && i <= IntegerCache.high)
        return IntegerCache.cache[i + (-IntegerCache.low)];
    return new Integer(i);
}

private static class IntegerCache {
    static final int low = -128;
    static final int high;
    static final Integer cache[];

    static {
        // high value may be configured by property
        int h = 127;
        String integerCacheHighPropValue =
            sun.misc.VM.getSavedProperty("java.lang.Integer.IntegerCache.high");
        if (integerCacheHighPropValue != null) {
            try {
                int i = parseInt(integerCacheHighPropValue);
                i = Math.max(i, 127);
                // Maximum array size is Integer.MAX_VALUE
                h = Math.min(i, Integer.MAX_VALUE - (-low) -1);
            } catch( NumberFormatException nfe) {
                // If the property cannot be parsed into an int, ignore it.
            }
        }
        high = h;

        cache = new Integer[(high - low) + 1];
        int j = low;
        for(int k = 0; k < cache.length; k++)
            cache[k] = new Integer(j++);

        // range [-128, 127] must be interned (JLS7 5.1.7)
        assert IntegerCache.high >= 127;
    }

    private IntegerCache() {}
}
```

