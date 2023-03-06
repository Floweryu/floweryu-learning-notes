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

### 方法一： 集合Set

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

### 方法二： 原地交换数字

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
        // 交换位置，直到nums[i] = i
        int tmp = nums[i];
        nums[i] = nums[tmp];
        nums[tmp] = tmp;
    }
    return -1;
}
```

## 二维数组中的查找

> 在一个 n * m 的二维数组中，每一行都按照从左到右 **非递减** 的顺序排序，每一列都按照从上到下 **非递减** 的顺序排序。请完成一个高效的函数，输入这样的一个二维数组和一个整数，判断数组中是否含有该整数。
>
> ```
> [
>   [1,   4,  7, 11, 15],
>   [2,   5,  8, 12, 19],
>   [3,   6,  9, 16, 22],
>   [10, 13, 14, 17, 24],
>   [18, 21, 23, 26, 30]
> ]
> ```
>
> 给定 target = `5`，返回 `true`。
>
> 给定 target = `20`，返回 `false`。

### 方法一：二分查找

由于矩阵的行和列都是有序的，所以可以遍历行或者列使用二分查找来判断。

```java
public boolean findNumberIn2DArray(int[][] matrix, int target) {
    if (matrix.length == 0 || matrix[0].length == 0) {
        return false;
    }
    for (int[] ints : matrix) {
        // 二分查找
        int l = 0, r = ints.length - 1;
        while (l <= r) {
            int index = (l + r) / 2;
            if (ints[index] == target) {
                return true;
            } else if (ints[index] < target) {
                l++;
            } else if (ints[index] > target) {
                r--;
            }
        }
    }
    return false;
}
```

### 方法二： 二叉搜索树思想（消去行列）

如下图所示：将矩阵逆时针旋转，可以发现其类似于**二叉搜索树**。即每个元素，左分支元素更小，右分支元素更大。

<img src="https://floweryu-image.oss-cn-shanghai.aliyuncs.com/image202303062026709.png" alt="image-20230306202617089" style="zoom:50%;" />

因此，以左下角元素或者右上角元素为基础`flag`，则有：

- 若`target < flag`：则`target`一定在`flag`所在行的上面，这样`flag`所在**行**就可以消除。
- 若`target > flag`：则`target`一定在`flag`所在行的下面，这样`flag`所在**列**就可以消除。

复杂度分析：

- 时间复杂度：`O(M + N)`，M和N为矩阵行数和列数
- 空间复杂度：`O(1)`

```java
public boolean findNumberIn2DArray(int[][] matrix, int target) {
    // 从左下角元素开始
    int i = matrix.length - 1, j = 0;
    while (i >= 0 && j <= matrix[0].length - 1) {
        if (matrix[i][j] == target) {
            return true;
        } else if (matrix[i][j] > target) {
            // 消除行
            i--;
        } else if (matrix[i][j] < target) {
            // 消除列
            j++;
        }
    }
    return false;
}
```

