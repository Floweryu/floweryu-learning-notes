> 在进行分页查询的时候，通常会使用LIMIT加偏移量的办法实现，但当偏移量非常大的时候，例如：LIMIT 10000， 20这样的查询，这是MySQL需要查询10020条记录然后只返回20条，前面的10000条都将会抛弃。这样的代价就非常高。

优化上述查询，有两种方案供选择：

- 在页面中限制分页的数量
- 优化大偏移量的性能

**使用索引覆盖扫描，不查询所有的列，然后根据需要进行一次关联，再返回所需的列**

例如下面查询：

```mysql
SELECT film_id, description FROM sakila.film ORDER BY title LIMIT 50, 5;
```

当偏移量很大的时候，可以改成下面查询：

```mysql
SELECT film.film_id, film.description FROM sakila.film INNER JOIN (
	SELECT film_id FROM sakila.film ORDER BY title LIMIT 50, 5
) AS lim USING(film_id);
```

这里先获取需要访问的记录，然后再根据关联列回原表查询所需要的列。

***

**将LIMIT查询转换为已知位置的查询，让MySQL通过范围扫描获得到对应的结果**

可以根据索引列，预先计算边界值，上面查询可以修改为：

```mysql
SELECT film_id, description FROM sakila.film
WHERE position BETWEEN 50 AND 54 ORDER BY position;
```

****

> LIMIT和OFFSET的问题，实际上是OFFSET的问题，它会导致MySQL扫描大量不需要的行然后再抛弃掉。

**可以使用标记记录上次读取数据的位置，下次就可以直接从该标记的位置开始扫描，这样可以避免OFFSET**

假设使用下面查询获取第一条结果：

```mysql
SELECT * FROM sakila.rental
ORDER BY rental_id DESC LIMIT 20;
```

假设上面查询的是主键16049到16030的租界记录，下一页查询就可以从16030这个点开始

```mysql
SELECT * FROM sakila.rental
WHERE rental_id < 16030
ORDER BY rental_id DESC LIMIT 2
```

