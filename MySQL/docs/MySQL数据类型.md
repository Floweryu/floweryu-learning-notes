## VARCHAR

VARCHAR用于存储变长字符串，是最常见的字符串数据类型。比定长类型更节省时间，因为它仅使用必要的空间，所以越短的字符串使用空间越少。除非MySQL表使用`ROW_FORMAT=FIXED`创建，每一行都会使用定长存储，就会浪费空间。

**VARCHAR需要使用1到2个额外字节保存字符串的长度**。如果列的最大长度小于或等于255字节，则只使用1个字节表示，否则使用2个字节。一个VARCHAR(10)的列需要11个字节的存储空间，VARCHAR(1000)的列需要1002个字节，因为需要2个字节存储长度信息。

VARCHAR节省了存储空间，对性能有帮助。但是由于是变长的，在UPDATE时可能使行变得比原来更长，这就导致需要做额外的工作，不同存储引擎处理方式也不一样。

## CHAR

在`InnoDB`引擎下：

CHAR类型是定长的，适合存储很短的字符串，或者所有值都接近同一个长度。

当存储CHAR值时，MySQL会删除所有末尾的空格，CHAR也会根据需要用空格填充以方便比较。

对于经常变更的数据，CHAR比VARCHAR更好，因为定长的CHAR不容易产生碎片。对于非常短的列，CHAR比VACHAR在存储空间上更有效率，因为VARCHAR需要额外字节来保存长度。

【示例】

```mysql
mysql> CREATE TABLE char_test (char_col CHAR(10));
mysql> INSERT INTO char_test(char_col) VALUES ('string1'), ('  string2'), ('string3  ');
mysql> SELECT CONCAT("'", char_col, "'") FROM char_test;
+----------------------------+
| CONCAT("'", char_col, "'") |
+----------------------------+
| 'string1'                  |
| '  string2'                |
| 'string3'                  |
+----------------------------+
3 rows in set (0.00 sec)
# 可以发现string3后面的空格被截断了
```

但是`char_col`字段使用`VARCHAR(10)`，查询结果如下：

```mysql
+----------------------------+
| CONCAT("'", char_col, "'") |
+----------------------------+
| 'string1'                  |
| '  string2'                |
| 'string3  '                |
+----------------------------+
3 rows in set (0.00 sec)
```

## ENUM

枚举值内部实际存储的是数字，并且存储的顺序是按照内部存储的整数，而不是定义的字符串。

```mysql
mysql> CREATE TABLE enum_test(e ENUM('fish', 'apple', 'dog') NOT NULL);
mysql> INSERT INTO enum_test(e) VALUES('fish'), ('dog'), ('apple');
mysql> SELECT e + 0 FROM enum_test;
+-------+
| e + 0 |
+-------+
|     1 |
|     3 |
|     2 |
+-------+
3 rows in set (0.00 sec)
mysql> SELECT e FROM enum_test ORDER BY e;
+-------+
| e     |
+-------+
| fish  |
| apple |
| dog   |
+-------+
3 rows in set (0.00 sec)
```

## 日期和时间类型

### DATETIME

这个类型能保存大范围的值，从1001年到9999年，精度为秒。

日期和时间封装到格式为`YYYYMMDDHHMMSS`的整数中，与时区无关。使用8个字节的存储空间。

默认情况下，MySQL使用下面格式显示DATETIME值：`2021-07-21 09:52:50`。这是ANSI标准定义的日期和时间表示方法。

### TIMESTAMP

TIMESTAMP类型保存了从1970年1月1日午夜（格林尼治标准时间）以来的秒数，它和UNIX时间戳相同。

TIMESTAMP只使用4个字节的存储空间，所以它的范围比DATETIME小的多：只能表示从1970年到2038年。

MySQL提供了`FROM_UNIXTIME()`函数把UNIX时间戳转换为日期，并提供`UNIX_TIMESTAMP()`函数把日期转换为Unix时间戳。

TIMESTAMP的显示也依赖于时区。

默认情况下，如果插入时没有指定第一个TIMESTAMP列的值，MySQL则设置这个列的值为当前时间。在更新一行记录时，MySQL也会更新TIMSTAMP的值，除非在UPDATE时指定了值。TIMESTAMP默认是`NOT NULL`.

## BIT

BIT(1)定义一个包含一个位的字段，BIT(2)存储2个位，以此类推，BIT列最大长度是64个位。

MySQL把BIT当字符串类型，而不是数字类型。当检索BIT(1)的值时，结果是一个包含二进制0或1的字符串，而不是ASCII码的"0"或"1"。

在数字上下文场景中检测时，结果将是位字符串单纯转换成数字。

```mysql
mysql> CREATE TABLE bittest(a bit(8));
mysql> INSERT INTO bittest VALUES(b'00111001');
mysql> SELECT a, a + 0 FROM bittest;
+------------+-------+
| a          | a + 0 |
+------------+-------+
| 0x39       |    57 |
+------------+-------+
1 row in set (0.00 sec)
```

