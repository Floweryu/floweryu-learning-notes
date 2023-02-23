## 数组中重复的数字

> 找出数组中重复的数字。
>
> 在一个长度为 n 的数组 nums 里的所有数字都在 0～n-1 的范围内。数组中某些数字是重复的，但不知道有几个数字重复了，也不知道每个数字重复了几次。请找出数组中任意一个重复的数字。
>
> ```
> 输入：
> [2, 3, 1, 0, 2, 5, 3]
> 输出：2 或 3
> ```
>
> **限制：**2 <= n <= 100000

#### 方法一： 集合Set

使用集合记录数组各个数字，遇到重复数字直接返回。

> 时间复杂度：O(n)，Set查找元素O(1)

```java
public int findRepeatNumber(int[] nums) {
    Set<Integer> set = new HashSet<>();
    for (int num : nums) {
        if (set.contains(num)) {
            return num;
        }
        set.add(num);
    }
    return -1;
}
```

#### 方法二： 原地交换数字

1. 遍历数组 nums ，设索引初始值为 i=0 :

   - 若 `nums[i]=i`，说明此数字已在对应索引位置，无需交换，因此跳过；

   - 若 `nums[nums[i]]=nums[i]`， 说明索引`nums[i]`处和索引`i`处的元素值都为`nums[i]`，即找到一组重复值；

   - 否则，交换索引`i`和索引`nums[i]`的元素值，将此数字交换置对应索引位置。

2. 若遍历完未返回，返回-1.

```java
public int findRepeatNumber(int[] nums) {
    int i = 0;
    while (i < nums.length) {
        if (nums[i] == i) {
            i++;
            continue;
        }
        if (nums[i] == nums[nums[i]]) {
            return nums[i];
        }
        int tmp = nums[i];
        nums[i] = nums[tmp];
        nums[tmp] = tmp;
    }
    return -1;
}
```

