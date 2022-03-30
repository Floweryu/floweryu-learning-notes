> 最近使用到了Comparator接口的compare方法，思考了一下改方法的返回值跟升序降序的关系。

### 背景

#### 升序代码

```java
public void sortTest() {
    Integer[] nums = new Integer[]{1, 4, 3, 5, 2, 7, 6};
    Arrays.sort(nums, new Comparator<Integer>() {
        @Override
        public int compare(Integer o1, Integer o2) {
            return o1 - o2;
        }
    });
    for (Integer i : nums) {
        System.out.print(i + "  ");
    }
}

// 输出：1  2  3  4  5  6  7
```

#### 降序代码

```java
public void sortTest() {
    Integer[] nums = new Integer[]{1, 4, 3, 5, 2, 7, 6};
    Arrays.sort(nums, new Comparator<Integer>() {

        @Override
        public int compare(Integer o1, Integer o2) {
            return o2 - o1;
        }
    });
    for (Integer i : nums) {
        System.out.print(i + "  ");
    }
}
// 输出：7  6  5  4  3  2  1
```

**结论：`compare(Integer o1, Integer o2) `方法 `return o1 - o2` 是升序，`return o2 - o1` 是降序**

### 原理

找到`Arrays.sort`方法中带比较器的源码：

```java
public static <T> void sort(T[] a, Comparator<? super T> c) {
    if (c == null) {
        sort(a);
    } else {
        if (LegacyMergeSort.userRequested)
            legacyMergeSort(a, c);
        else
            TimSort.sort(a, 0, a.length, c, null, 0, 0);
    }
}
```
比较器不为null，则进入else方法，先去`legacyMergeSort(a, c)`方法中看看：
```java
private static <T> void legacyMergeSort(T[] a, Comparator<? super T> c) {
    T[] aux = a.clone();
    if (c==null)
        mergeSort(aux, a, 0, a.length, 0);
    else
        mergeSort(aux, a, 0, a.length, 0, c);
}
```
比较器不为null，进入else方法`mergeSort(aux, a, 0, a.length, 0, c)`：
```java
private static void mergeSort(Object[] src,
                                  Object[] dest,
                                  int low, int high, int off,
                                  Comparator c) {
        int length = high - low;

        // Insertion sort on smallest arrays
    	// 主要是这段代码
        if (length < INSERTIONSORT_THRESHOLD) {
            for (int i=low; i<high; i++)
                for (int j=i; j>low && c.compare(dest[j-1], dest[j])>0; j--)
                    swap(dest, j, j-1);
            return;
        }

        // Recursively sort halves of dest into src
        int destLow  = low;
        int destHigh = high;
        low  += off;
        high += off;
        int mid = (low + high) >>> 1;
        mergeSort(dest, src, low, mid, -off, c);
        mergeSort(dest, src, mid, high, -off, c);

        // If list is already sorted, just copy from src to dest.  This is an
        // optimization that results in faster sorts for nearly ordered lists.
        if (c.compare(src[mid-1], src[mid]) <= 0) {
           System.arraycopy(src, low, dest, destLow, length);
           return;
        }

        // Merge sorted halves (now in src) into dest
        for(int i = destLow, p = low, q = mid; i < destHigh; i++) {
            if (q >= high || p < mid && c.compare(src[p], src[q]) <= 0)
                dest[i] = src[p++];
            else
                dest[i] = src[q++];
        }
    }
```

上面代码关键看下面这部分：

```java
if (length < INSERTIONSORT_THRESHOLD) {
    for (int i=low; i<high; i++)
        for (int j=i; j>low && c.compare(dest[j-1], dest[j])>0; j--)
            swap(dest, j, j-1);
    return;
}
```

**这里面调用了`compare`方法，当该方法返回值大于0的时候就将数组前一个数和后一个数交换**

**如果是升序：`compare`方法返回`o1 - o2`，就是`return dest[j-1] - dest[j] `，即当`dest[j-1] > dest[j]`时交换，当`dest[j-1] <= dest[j]`时位置不变，从而就达到数组升序**

**如果时降序：`compare`方法返回`o2 - o1`，就是`return dest[j] - dest[j - 1]`，即当`dest[j] > dest[j-1]`时交换，从而达到数组降序**

###  综上所述：

- **`compare`方法返回值大于0，会交换前后两个数位置**
- **`compare`方法返回值小于等于0，位置不交换**

