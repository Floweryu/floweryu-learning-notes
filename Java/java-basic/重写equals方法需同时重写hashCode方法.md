#### `hashCode()`介绍

`hashCode()`的作用是获取哈希码，也称为散列码；返回一个`int`整数。这个哈希码的作用就是确定该对象在哈希表中的索引位置。`hashCode()`定义在JDK的`Object.java`中，这就意味着`Java`中的任何类都包含有`hashCode()`函数.

散列表存储的是键值对(key-value).它的特点是：能根据“键”快速的检索出对应的“值”。这其中就利⽤到了散列码！

#### `hashCode()`的作用

当把对象加入`HashSet`时，`HashSet`会首先计算对象的`hashCode`来判断对象加入的位置，同时也会与该位置其它已经加入的对象的`hashCode`值作比较，如果没有相符的`hashCode`，则表示要加入的对象没有重复出现。但是如果发现相同的`hashCode`值的对象，这时就会调用`equals()`方法来检查`hashCode`相等的对象是否真的相同。如果两个真的相同，`HashSet`就不会将其加入操作。如果不同，就会重新排列到其它位置。

所以，`hashCode()`的作用就是获取哈希码，也称为散列码；它实际上是返回一个`int`整数。这个哈希码的作用是确定在哈希表中的索引位置。

##### `hashCode()`与`equals()`相关规定

1. 在java应用程序执行期间，如果在`equals`方法比较中所用的信息没有被修改，那么在同一个对象上多次调用`hashCode`方法时必须一致地返回相同的整数。如果多次执行同一个应用时，不要求该整数必须相同
2. 如果两个对象通过调用`equals`方法是相等的，那么这两个对象调用`hashCode`方法必须返回相同的整数。
3. 如果两个对象通过调用`equals`方法是不相等的，不要求这两个对象调用`hashCode`方法必须返回不同的整数。

在`Object`类中，`hashCode`方法是通过`Object`对象的地址计算出来的。因为`Object`对象只与自身相等，所以同一个对象的地址总是相等的，计算的哈希码也必然相等。对于不同的对象，地址不同，所获得的哈希码自然也不会相等。如果重写了`equals()`方法，但是没有重写`hashCode`方法，违反了第二条规定。

**示例代码**

```java
package equals.src;

import java.util.HashMap;
import java.util.Map;

public class App {
    public static void main(String[] args) {
        Map<String, Value> map1 = new HashMap<String, Value>();
        String s1 = new String("key");
        String s2 = new String("key");
        Value value = new Value(2);
        map1.put(s1, value);    // 把s1和value放到hashMap
        System.out.println("s1.equals(s2):" + s1.equals(s2)); // s1.equals(s2):true
        System.out.println("map1.get(s1):" + map1.get(s1)); // map1.get(s1):类Value的值－－>2
        System.out.println("map1.get(s2):" + map1.get(s2)); // map1.get(s2):类Value的值－－>2

        Map<Key, Value> map2 = new HashMap<Key, Value>();
        Key k1 = new Key("A");
        Key k2 = new Key("A");
        map2.put(k1, value);
        System.out.println("k1.equals(k2):" + k1.equals(k2));   // k1.equals(k2):true
        System.out.println("map2.get(k1):" + map2.get(k1));     // map2.get(k1):类Value的值－－>2
        System.out.println("map2.get(k2):" + map2.get(k2));     // map2.get(k2):null
    }

    static class Key {
        private String k;

        public Key(String key) {
            this.k = key;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Key) {
                Key key = (Key) obj;
                return k.equals(key.k);
            }
            return false;
        }
    }

    static class Value {
        private int v;

        public Value(int v) {
            this.v = v;
        }

        @Override
        public String toString() {
            return "类Value的值－－>" + v;
        }
    }

}

```

上述代码中：`Key`这个类重写了`equals()`方法，但是没有重写`hashCode()`方法。String类重写了`equals`方法和`hashCode`方法。

所以，`map1`可以得到`s1和s2`的值，因为`String`类比较的是内容，它的`hashCode`也是根据内容获取的哈希码。

`map2`只能获取`k1`的值，不能获取到`k2`的值。这是为什么？？**因为`Key`只重写了`equals`方法，并没有重写`hashCode`方法。这样的话，`equals`方法比较的是内容，所以打印为`true`。但是`hashCode`没有被重写，所以就调用超类`Object`的`hashCode`方法，而这个方法返回的是一个地址（是根据地址来获取hashcode的）。而`k1和k2`又是不同的对象，它们的地址肯定不同，所以获得的`hashcode`也不同。因为`k2`返回为`null`**