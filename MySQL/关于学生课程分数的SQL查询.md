# 1. 建表

有如下4张表：
学生表：`student(s_id,s_name,s_birth,s_sex)` ——`学生编号,学生姓名, 出生年月,学生性别`
课程表：`course(c_id,c_name,t_id)` ——`课程编号, 课程名称, 教师编号`
教师表：`teacher(t_id,t_name) `——`教师编号,教师姓名`
成绩表：`score(s_id,c_id,s_s_score)` ——`学生编号,课程编号,分数`

## 1.2 学生表

```mysql
-- 1 创建表格
CREATE TABLE IF NOT EXISTS student(
s_id  VARCHAR(20) not null,
s_name VARCHAR(20) NOT NULL,
s_birth VARCHAR(20) NOT NULL,
s_sex VARCHAR(10) NOT NULL,
PRIMARY KEY(s_id)
) engine=innodb default charset=utf8;
-- 2 插入数据
insert into student(s_id,s_name,s_birth,s_sex) values('01' , '赵雷' , '1990-01-01' , '男');
insert into student(s_id,s_name,s_birth,s_sex) values('02' , '钱电' , '1990-12-21' , '男');
insert into student(s_id,s_name,s_birth,s_sex) values('03' , '孙风' , '1990-05-20' , '男');
insert into student(s_id,s_name,s_birth,s_sex) values('04' , '李云' , '1990-08-06' , '男');
insert into student(s_id,s_name,s_birth,s_sex) values('05' , '周梅' , '1991-12-01' , '女');
insert into student(s_id,s_name,s_birth,s_sex) values('06' , '吴兰' , '1992-03-01' , '女');
insert into student(s_id,s_name,s_birth,s_sex) values('07' , '郑竹' , '1989-07-01' , '女');
insert into student(s_id,s_name,s_birth,s_sex) values('08' , '王菊' , '1990-01-20' , '女');
```

## 1.2 课程表

```mysql
-- 1 创建表格
create table if not exists course
(
c_id VARCHAR(20) not null,
c_name varchar(20) default null,
t_id VARCHAR(20) not null,
primary key (c_id)
) engine=innodb default charset=utf8;
-- 2 插入数据
insert into course(c_id,c_name,t_id) values('01','语文','02');
insert into course(c_id,c_name,t_id) values('02','数学','01');
insert into course(c_id,c_name,t_id) values('03','英语','03');
```

## 1.3 教师表

```mysql
-- 1 创建表格
create table if not exists teacher 
(
t_id VARCHAR(20) not null,
t_name varchar(20) DEFAULT null,
primary key (t_id)
) engine=innodb default charset=utf8;
-- 2 插入数据
insert into teacher(t_id,t_name) values('01','张三');
insert into teacher(t_id,t_name) values('02','李四');
insert into teacher(t_id,t_name) values('03','王五');
```

## 1.4 成绩表

```mysql
-- 1 创建表格
create table if not exists score
(
 s_id VARCHAR(20) not null,
 c_id VARCHAR(20) not null,
 s_score VARCHAR(20) default null
) engine=INNODB default charset=utf8;
-- 2 插入数据
insert into score(s_id,c_id,s_score) values('01','01','80');
insert into score(s_id,c_id,s_score) values('01','02','90');
insert into score(s_id,c_id,s_score) values('01','03','99');
insert into score(s_id,c_id,s_score) values('02','01','70');
insert into score(s_id,c_id,s_score) values('02','02','60');
insert into score(s_id,c_id,s_score) values('02','03','80');
insert into score(s_id,c_id,s_score) values('03','01','80');
insert into score(s_id,c_id,s_score) values('03','02','80');
insert into score(s_id,c_id,s_score) values('03','03','80');
insert into score(s_id,c_id,s_score) values('04','01','50');
insert into score(s_id,c_id,s_score) values('04','02','30');
insert into score(s_id,c_id,s_score) values('04','03','20');
insert into score(s_id,c_id,s_score) values('05','01','76');
insert into score(s_id,c_id,s_score) values('05','02','87');
insert into score(s_id,c_id,s_score) values('06','01','31');
insert into score(s_id,c_id,s_score) values('06','03','34');
insert into score(s_id,c_id,s_score) values('07','02','89');
insert into score(s_id,c_id,s_score) values('07','03','98');
```

# 2. SQL语句查询

1. 查询"01"课程比"02"课程成绩高的学生的信息及课程分数

```mysql
-- 1.0 查询学生信息和课程分数，子查询是学生01课程比02课程高【1】
select student.*, score.c_id, score.s_score
from student join score on student.s_id = score.s_id
where student.s_id in 
(
select a.s_id from (select * from score where score.c_id = 01) as a
left join (select * from score where score.c_id = 02) as b
on a.s_id = b.s_id
where a.s_score > b.s_score and  b.s_id is not null
);

-- 1.1 步骤1：获取"01"课程比"02"课程成绩高学生ID【2】
select a.s_id
from (select s_id,s_score from score where c_id = 01) as a,(select s_id,s_score from score where c_id = 02) as b
where a.s_id = b.s_id and a.s_score > b.s_score and b.s_score is not null;

-- 1.2 步骤2：从获取的ID中读取学生信息【3】
select score.c_id,score.s_score, student.*
from student left join score on student.s_id = score.s_id
where score.s_id in (select a.s_id
from (select s_id,s_score from score where c_id = 01) as a,(select s_id,s_score from score where c_id = 02) as b
where a.s_id = b.s_id and a.s_score > b.s_score and b.s_score is not null);
```

2. 查询"01"课程比"02"课程成绩低的学生的信息及课程分数

```mysql
-- 2.0 方法1 
select student.*, score.c_id, score.s_score
from student join score on student.s_id = score.s_id
where student.s_id in (
select a.s_id from (select * from score where score.c_id = 01) as a
left join (select * from score where score.c_id = 02) as b 
on a.s_id = b.s_id
where a.s_score < b.s_score and a.s_score is not null
);

--  2.1 步骤1 获取"01"课程比"02"课程成绩低的学生s_id【6】
select a.s_id 
from (select s_id, s_score from score where c_id = 01) as a, 
     (select s_id, s_score from score where c_id = 02) as b
where a.s_id = b.s_id and a.s_score < b.s_score and a.s_score is not null;

-- 2.2 步骤2：根据学生s_id读取学生信息【7】
select score.c_id, score.s_score, student.*
from score right join student on score.s_id = student.s_id
where score.s_id in 
(select a.s_id from (select s_id, s_score from score where c_id = 01) as a, 
(select s_id, s_score from score where c_id = 02) as b
where a.s_id = b.s_id and a.s_score < b.s_score and a.s_score is not null);
```

3. 查询平均成绩大于等于60分的同学的学生编号和学生姓名和平均成绩

```mysql
select student.s_id, student.s_name, round(avg(score.s_score),1) as s_avg
from student join score on student.s_id = score.s_id
group by score.s_id  
having avg(score.s_score)>=60;

-- 3.1 步骤1:首先，筛选平均成绩大于等于60分学生s_id【9】
select s_id, round(avg(s_score),1) as avg_score 
from score 
group by s_id 
having avg_score >= 60;

-- 3.2 根据s_id筛选学生信息【10】
select  a.s_name,b.*
from student as a left join 
(select s_id, round(avg(s_score),1) as avg_score from score group by s_id ) as b
on a.s_id = b.s_id
where b.avg_score >= 60;
```

4. 查询平均成绩小于60分的同学的学生编号和学生姓名和平均成绩

```mysql
select student.s_id, student.s_name, round(avg(score.s_score),1) as s_avg
from student join score on student.s_id = score.s_id
group by score.s_id
having round(avg(score.s_score),1) <= 60 and round(avg(score.s_score),1) is not null;

-- 4.1 步骤1：查询平均成绩小于60分的同学的学生编号【2】
select s_id, round(avg(s_score),1) as avg_score from score group by s_id having avg_score < 60;

-- 4.2 步骤2：根据查询s_id查出需要的信息【3】
select student.s_id, student.s_name, a.avg_score
from student join 
(select s_id, round(avg(s_score),1) as avg_score from score group by s_id having avg_score < 60) as a 
on student.s_id = a.s_id;
```

5.  查询所有同学的学生编号、学生姓名、选课总数、所有课程的总成绩

```mysql
select student.s_id, student.s_name, count(distinct(score.c_id)), sum(score.s_score)
from student join score on student.s_id = score.s_id
group by score.s_id;

-- 5.1 步骤1：选课总数和课程总成绩是课程表数据，因此子查询需要将上述数据+s_id获取【5】
select s_id, count(distinct c_id) as num_id, sum(s_score) as sum_score
from score 
group by s_id;

-- 5.2 步骤2：根据获取的s_id信息，join学生姓名【6】
select student.s_name, a.*
from student join 
(select s_id, count(distinct c_id) as num_id, sum(s_score) as sum_score
from score 
group by s_id) as a
on student.s_id = a.s_id;
```

6. 查询"李"姓老师的数量

```mysql
select count(distinct(t_id))
from teacher 
where teacher.t_name like '李%';
```

7. 查询学过"张三"老师授课的同学的信息

```mysql
select student.*
from student join score on student.s_id = score.s_id
where score.c_id in 
( select course.c_id
from course join teacher on course.t_id = teacher.t_id
where teacher.t_name = '张三');

-- 学生信息是在学生表，学生表主键和得分表连接，得分表c_id和课程表主键连接，课程表t_id和老师表主键连接
-- 7.1 步骤1：找到张三老师教授的课程c_id【9】
select  c_id
from teacher as t left join course on t.t_id = course.t_id
where t_name = '张三';

-- 7.2 步骤2：通过课程c_id找到学生s_id信息【10】
select student.*
from student join score on student.s_id = score.s_id 
where score.c_id in
(select  c_id
from teacher as t left join course on t.t_id = course.t_id
where t_name = '张三');

-- 7.3 查询学过"张三"老师授课的同学的信息【join才是准确的】【11】
select student.*
from teacher right join course on teacher.t_id = course.t_id
             right join score on course.c_id = score.c_id
             right join student on score.s_id = student.s_id
where teacher.t_name = '张三';


-- 7.4 查询学过"张三"老师授课的同学的信息【join才是准确的】【12】
select student.*
from student left join score on student.s_id = score.s_id
             left join course on course.c_id = score.c_id
             left join teacher on teacher.t_id = course.t_id
where t_name = '张三';
```

8. 查询没学过"张三"老师授课的同学的信息

```mysql
select student.*
from student
where student.s_id not in -- 以下嵌套子查询是找出学过张三老师课程的学生s_id
(
select student.s_id
from student join score on student.s_id = score.s_id
where score.c_id  in 
(
select course.c_id
from course  join teacher on course.t_id = teacher.t_id 
where teacher.t_name = '张三'
)
);

-- 8.1 步骤1：先找出张三老师教课的课程代码，学过张三老师课程的学生s_id【2】
select score.s_id
from score  join course on score.c_id = course.c_id
            join teacher on course.t_id = teacher.t_id -- 这里用left join ，因为张三老师可能教授多门课程
where t_name = '张三';

-- 8.2 步骤2：排除学过张三老师课程的学生s_id的信息【3】
select distinct student.*
from student left join score on student.s_id = score.s_id  -- 这里用left join ，可能存在学生没有选目前的课程
where student.s_id not in 
( select score.s_id
from score  join course on score.c_id = course.c_id
            join teacher on course.t_id = teacher.t_id 
where t_name = '张三');

```

9. 查询学过编号为"01"并且也学过编号为"02"的课程的同学的信息

```mysql
select student.*
from student
where student.s_id IN  -- where并联两个嵌套循环
(
select score.s_id
from score
where score.c_id = 01
)
and student.s_id IN
(
select score.s_id
from score
where score.c_id = 02
);

-- 9.1 查询同时学过01和02课程同学的S_ID信息【5】
select a.s_id 
from 
(select s_id
from score join course on score.c_id = course.c_id -- 这里也可以不需要导入course表，最好是不要引入，且看第10题
where score.c_id = 01) as a join 
(select s_id
from score join course on score.c_id = course.c_id
where score.c_id = 02) as b
on a.s_id = b.s_id;

-- 9.2 排除上述同学余下的同学信息【6】
select student.*
from student 
where student.s_id in 
(select a.s_id 
from                          -- 两个子查询取交集
(select s_id
from score join course on score.c_id = course.c_id
where score.c_id = 01) as a join 
(select s_id
from score join course on score.c_id = course.c_id
where score.c_id = 02) as b
on a.s_id = b.s_id);
```

10. 查询学过编号为"01"但是没有学过编号为"02"的课程的同学的信息

```mysql
select student.*
from student 
where student.s_id IN
(
select score.s_id
from score
where score.c_id = 01  # score.c_id != 02 and 
)
and student.s_id not IN
(
select score.s_id
from score
where score.c_id = 02
);

-- 10.2 【8】
select student.*
from student 
where student.s_id in (select s_id from score where c_id = 01)
and student.s_id not in (select s_id from score where c_id = 02);
```

11. 查询没有学全所有课程的同学的信息

```mysql
-- 11.1 查询没有学全所有课程的同学的信息【9】
/* 连接student表和score表，对同学进行分组，每组使用count，最后使用having过滤 */
select student.*
from student left join score on student.s_id = score.s_id -- 防止有人根本就没有选修课程
group by score.s_id
having count(score.c_id) < (select count(c_id) from course);

-- 11.2 
select student.*
from student
where s_id not in 
(select s_id     
from (select s_id,COUNT(DISTINCT c_id) as std__course from score group by s_id) as a
where std__course = (select count(distinct c_id) as total_course from course));

-- 11.3 找出本该学习的课程总数
select count(distinct c_id) as total_course from course; -- 找出本该学习的课程总数

-- 11.4 找出各人学习了多少课程
select s_id,COUNT(DISTINCT c_id) as std__course from score group by s_id;  -- 找出各人学习了多少课程


-- 11.5 查询没有学全所有课程的同学的信息
select *
from student
where s_id in 
(select s_id
from (select student.s_id, count(distinct c_id) as std_course
from student left join score on student.s_id = score.s_id
group by student.s_id) as a
where std_course < (select count(distinct c_id) from course ));

-- 11.6 筛选同时选修了三门课程的学生s_id
select student.*
from student
where student.s_id in  
(
select a.s_id 
from (select * from score where score.c_id = 01) as a  
left join (select * from score where score.c_id = 02) as b  
on a.s_id = b.s_id
)
and student.s_id in 
(
select a.s_id
from (select * from score where score.c_id = 01) as a
left join (select * from score where score.c_id = 03) as c 
on a.s_id = c.s_id
)
and student.s_id in 
(
select b.s_id
from (select * from score where score.c_id = 02) as b
left join (select * from score where score.c_id = 03) as c  
on b.s_id = c.s_id
);
```

12. 查询至少有一门课与学号为"01"的同学所学相同的同学的信息

```mysql
-- 子查询找出01同学学过的课程
-- score表中查询含有这些课程对应的s_id，去除01
select distinct student.*
from student join score on student.s_id = score.s_id
where score.c_id IN
(
select c_id 
from score
where score.s_id = 01
)
and score.s_id != 01;

-- 12.1 查看学号为"01"的同学学了哪些课程【2】
select distinct course.c_id 
from score join course on score.c_id = course.c_id -- 防止01同学学了课程，但是没有成绩，加上join
where s_id = 01;

-- 12.2 查找学过上述课程的同学，且排除学号01【3】
select distinct student.* 
from student left join score score on student.s_id = score.s_id
where c_id in 
(select distinct course.c_id 
from score join course on score.c_id = course.c_id -- 防止01同学学了课程，但是没有成绩，加上join
where s_id = 01)
and score.s_id != 01;
```

13. 查询和"01"号的同学学习的课程完全相同的其他同学的信息

```mysql
-- 找出至少与01同学学习过的相同课程的同学；
-- 对学习过共同课程的同学分组，计算其分组后学习的不同课程科目数，进行对比
select student.*
from student join score on student.s_id = score.s_id 
where score.c_id  in 
( 
select score.c_id
from score 
where score.s_id = 01)
and student.s_id != 01
group by student.s_id
having count(distinct score.c_id) = 
(
select count(distinct c_id)
from score 
where s_id = 01
);

-- 13.1 这是一个有问题的解法，因为可能存在01学的是1/2/4 【5】
select distinct *
from student left join score on student.s_id = score.s_id 
where c_id in 
(select course.c_id from score right join course on course.c_id = score.c_id where s_id = 01)
and student.s_id != 01
group by student.s_id 
having count(distinct c_id) = 
(select count(distinct course.c_id) from score right join course on course.c_id = score.c_id where s_id = 01); 

-- 13.1 筛选01同学学习过的课程，且与学生s_id对应【6】
select student.s_id, t.c_id
from student, (select score.c_id from score where s_id = 01) as t;

-- 13.2 上述获得的数据与score对应，筛选出同时满足s_id,c_id信息，且score.c_id存在空值的学生id【7】
select distinct t1.s_id 
from score right join (select student.s_id, t.c_id from student, (select score.c_id from score where s_id = 01) as t) as t1
on score.s_id = t1.s_id and score.c_id = t1.c_id 
where score.c_id is  null; 

-- 13.3 根据上述步骤取学生s_id余数【8】
select student.* from student
where s_id not in 
(select distinct t1.s_id 
from score right join (select student.s_id, t.c_id from student, (select score.c_id from score where s_id = 01) as t) as t1
on score.s_id = t1.s_id and score.c_id = t1.c_id 
where score.c_id is  null)
and s_id != 01; 
```

14. 查询没学过"张三"老师讲授的任一门课程的学生姓名

```mysql
-- 子查询张三老师教过的课程代码
-- 子查询学过可查鞥代码的s_id
--  不包含这些s_id
select student.*
from student 
where student.s_id not in 
(
select score.s_id 
from score
where score.c_id = 
(
select course.c_id
from course join teacher on course.t_id = teacher.t_id 
where teacher.t_name = '张三'
)
);

-- 14.1 查询张三老师教授的课程 【10】
select c_id
from teacher left join course on teacher.t_id = course.t_id 
where t_name = '张三';

-- 14.2 筛选学过张三老师的学生s_id  【11】
select distinct s_id from score
where c_id = 
(select c_id
from teacher left join course on teacher.t_id = course.t_id 
where t_name = '张三');

-- 14.3 筛选课程不包含02的学生信息，考虑到可能存在有些学生没有选课，需要使用左连接 【12】
select distinct student.*
from student left join score on student.s_id = score.s_id
where student.s_id not in 
(select distinct s_id from score
where c_id = 
(select c_id
from teacher left join course on teacher.t_id = course.t_id 
where t_name = '张三'));
```

15. 查询两门及其以上不及格课程的同学的学号，姓名及其平均成绩

```mysql
-- 15.1 查询两门及其以上不及格课程的同学的学号，姓名及其平均成绩 【1】
-- 子查询不及格的信息，统计不及格有2个及其以上的数据
--  对s_id分组求均值
select student.s_id, student.s_name, avg(score.s_score)
from student join score on student.s_id = score.s_id
where student.s_id in 
(
select distinct score.s_id
from score 
where score.s_score < 60
group by score.s_id 
having count(score.s_score) >= 2
)
group by student.s_id; 

# 15.2 查询两门及其以上不及格课程的同学的学号，姓名及其平均成绩 【2】
select student.s_id, student.s_name, avg(score.s_score)
from student join  score on student.s_id = score.s_id
where score.s_score < 60
group by student.s_id #, student.s_name
having count(score.c_id) >= 2;

-- 15.3                                                          【3】
select distinct score.* 
from score 
where score.s_score < 60
group by score.s_id 
having count(score.s_score) >= 2;

-- 15.1 查询不及格的数据，分组筛选不及格大于等于2，再返回其as_id  【4】
select s_id from score where s_score < 60 group by s_id having count(distinct c_id) >= 2;

-- 15.2 根据上述查询返回的信息，重新查找器数据和均值成绩           【5】
select student.s_id, student.s_name, round(avg(score.s_score),1) as avg_score
from student left join score on student.s_id = score.s_id 
where student.s_id in (select s_id from score where s_score < 60 group by s_id having count(distinct c_id) >= 2)
group by s_id;
```

16. 检索"01"课程分数小于60，按分数降序排列的学生信息

```mysql
-- 16 检索"01"课程分数小于60，按分数降序排列的学生信息 【6】
select student.*, score.s_score
from student join score on student.s_id = score.s_id
where score.s_score < 60 and score.c_id = 01
order by score.s_score Desc;

-- 16.1 检索"01"课程分数小于60，按分数降序排列的学生信息 【7】
select student.*, score.s_score
from student join score on student.s_id = score.s_id 
where score.s_score < 60 and score.c_id = 01 
order by s_score desc;
```

17. 按平均成绩从高到低显示所有学生的所有课程的成绩以及平均成绩

```mysql
select score.s_id as sid,
  (select s_score from score where s_id = sid and c_id = 01) as '语文',  -- sid 必须设置，否则子查询返回多个值会报错
  (select s_score from score where s_id = sid and c_id = 02) as '数学',
  (select s_score from score where s_id = sid and c_id = 03) as '英语',
  round(avg(score.s_score),0) as '平均成绩'
from score
group by score.s_id
order by avg(score.s_score) desc;

-- 17.1 查询各人平均成绩  【9】
select s_id, round(avg(s_score),0) as avg_score
from score group by s_id;

-- 17.2 连接score表      【10】
select score.*, t.avg_score
from score join 
(select s_id, round(avg(s_score),0) as avg_score
from score group by s_id)as t
on score.s_id = t.s_id
order by t.avg_score desc;
```

18. 查询各科成绩最高分、最低分和平均分

    ```mysql
    /* 以如下形式显示：课程ID，课程name，最高分，最低分，平均分，及格率，中等率，优良率，优秀率
       及格为>=60，中等为：70-80，优良为：80-90，优秀为：>=90
       where c_id = a.c_id是分组形成课程ID和课程name的要求透视表  */
    select a.c_id as '课程ID', course.c_name as '课程name',
    		(select s_score from score where c_id = a.c_id order by s_score desc limit 1) as '最高分',  
    		(select s_score from score where c_id = a.c_id order by s_score limit 1) as '最低分',
    		(select round(avg(s_score),1) from score where c_id = a.c_id) as '平均分',
    		(select count(distinct s_score) from score where c_id = a.c_id and s_score >= 60)/
    		(select count(distinct s_score) from score where c_id = a.c_id) as '及格率',
    		(select count(distinct s_score) from score where c_id = a.c_id and s_score >=70 and s_score < 80)/
    		(select count(distinct s_score) from score where c_id = a.c_id) as '中等率',
    		(select count(distinct s_score) from score where c_id = a.c_id and s_score >=80 and s_score <90 )/
    		(select count(distinct s_score) from score where c_id = a.c_id) as '优良率',
    		(select count(distinct s_score) from score where c_id = a.c_id and s_score >=90)/
    		(select count(distinct s_score) from score where c_id = a.c_id) as '优秀率'
    from score as a
    		join course on a.c_id = course.c_id
    group by a.c_id;
    
    -- 18.1 查询各科成绩最高分、最低分和平均分【2】
    /* 思路：分别找出各科成绩最高分、最低分和均分，连接三个数据集*/
    select c.c_id, c.c_name, h.max_score, l.min_score, v.avg_score
    from course as c 
    left join (select c_id, max(s_score) as max_score from score group by c_id) as h -- 最高成绩
    on c.c_id = h.c_id
    left join (select c_id, min(s_score) as min_score from score group by c_id) as l -- 最低成绩 
    on c.c_id = l.c_id
    left join (select c_id, round(avg(s_score),0) as avg_score from score group by c_id) as v -- 均值成绩
    on c.c_id = v.c_id;
    
    -- 18.2 case when 【3】
    select score.c_id, max(score.s_score) as 最高分, min(score.s_score) as 最低分, AVG(score.s_score) as 平均分,
    		count(*)as 选修人数,
    		sum(case when score.s_score>=60 then 1 else 0 end )/count(*) as 及格率,
    		sum(case when score.s_score>=70 and score.s_score<80 then 1 else 0 end )/count(*) as 中等率,
    		sum(case when score.s_score>=80 and score.s_score<90 and score.s_score<80 then 1 else 0 end )/count(*) as 优良率,
    		sum(case when score.s_score>=90 then 1 else 0 end )/count(*) as 优秀率 
    from score
    GROUP BY score.c_id
    ORDER BY count(*) desc, score.c_id asc;  
    
    -- 18.3 case when 表达式 【4】
    select c.c_id as "课程ID", c.c_name as "课程名字", max(s.s_score) as "最高分", min(s.s_score) as "最低分", 
    			 round(avg(s.s_score),1) as "平均成绩",
    			 round(sum(case when s.s_score >= 60 then 1 else 0 end)/count(s.s_score),2) as "及格率",
    			 round(sum(case when s.s_score >= 70 and s.s_score < 80 then 1 else 0 end)/count(s.s_score),2) as "中等率",
    			 round(sum(case when s.s_score >= 80 and s.s_score < 90 then 1 else 0 end)/count(s.s_score),2) as "良好率",
    			 round(sum(case when s.s_score >= 90 then 1 else 0 end)/count(s.s_score),2) as "优秀率"
    			 
    from course as c left join score as s on c.c_id = s.c_id
    group by c.c_id;
    ```

19. 按各科成绩进行排序，并显示排名，Score 重复时保留名次空缺

    这题答案有缺陷，没有对相同成绩排名不一致，第三解答可以

    ```mysql
    -- 19.2 按各科成绩进行排序，并显示排名，Score 重复时保留名次空缺
    select sc.c_id, sc.s_id,sc.s_score, 
           row_number() over (PARTITION by sc.c_id order by sc.s_score desc) as r_num
    from score as sc  
    order by sc.c_id, sc.s_score desc;	 --  此处也可以省去order by 
    
    -- 19.3 这个可以，按成绩顺序排序
    select sc.c_id,sc.s_id,sc.s_score,
    	(select count(distinct score.s_score)  
    	 from score 
    	 where score.c_id=sc.c_id and score.s_score>=sc.s_score) r_rank
    from score as sc
    order by sc.c_id,sc.s_score desc;
    
    -- 19.4 这个可以，按成绩和次序排序
    select sc.c_id,sc.s_id,sc.s_score,
    	(select count(score.s_score)+1  
    	 from score 
    	  where score.c_id=sc.c_id and score.s_score>sc.s_score) r_rank
    from score as sc
    order by sc.c_id,sc.s_score desc;
    ```

20. 查询学生的总成绩，按总成绩并进行排名

    ```mysql
    -- 20.1 
    select ss.*,
           (select count(distinct sv.sum_score) from (select st.s_id,st.s_name,if(sum(sc.s_score) is Null,0,sum(sc.s_score)) as sum_score
    from student as st left join score as sc 
    on st.s_id=sc.s_id 
    group by st.s_id
    order by sum_score desc) as sv where sv.sum_score>=ss.sum_score) as rank_num
    from (select st.s_id,st.s_name,if(sum(sc.s_score) is Null,0,sum(sc.s_score)) as sum_score
    from student as st left join score as sc 
    on st.s_id=sc.s_id 
    group by st.s_id
    order by sum_score desc) as ss;
    
    -- 20.2 这个也可以
    select m.*, count(n.sum_score) rank_num
    from (select st.s_id, st.s_name, if(sum(sc.s_score) is null,0,sum(sc.s_score)) sum_score
          from student as st left join score as sc
          on st.s_id = sc.s_id
          group by st.s_id) m,
         (select st.s_id, st.s_name, if(sum(sc.s_score) is null,0,sum(sc.s_score)) sum_score
          from student as st left join score as sc
          on st.s_id = sc.s_id
          group by st.s_id) n
    where n.sum_score>=m.sum_score
    group by m.s_id
    order by rank_num;
    ```

21. 统计各科成绩各分数段人数：课程编号，课程名称，[100-85]，[85-70]，[70-60]，[60- 0] 及所占百分比

```mysql
-- 21.1 二维表思路
select sc.c_id,
       (select score.s_score from score where sc.c_id = score.c_id order by score.s_score desc limit 1) as "第一名", -- max(sc.s_score) as "第一名"
       (select score.s_score from score where sc.c_id = score.c_id order by score.s_score desc limit 1,1) as "第二名",
			 (select score.s_score from score where sc.c_id = score.c_id order by score.s_score desc limit 2,1) as "第三名"
from score as sc
group by sc.c_id;

-- 21.2 转化为大于某成绩少于3的数据 【10】
select score.c_id, score.s_id, score.s_score 
from score 
where (select count(*) from score as sc where sc.c_id = score.c_id and score.s_score < sc.s_score) < 3
```



22. 查询各科成绩最后三名

```mysql
-- 22.1 子查询方法 自己与自己比较  【1】
select sc.c_id, min(sc.s_score) as "最后一名",
	     (select s.s_score from score as s where s.c_id = sc.c_id order by s.s_score limit 1) as "这也是最后一名",
	     (select s.s_score from score as s where s.c_id = sc.c_id order by s.s_score limit 0,1) as "这还是最后一名",
	     (select s.s_score from score as s where s.c_id = sc.c_id order by s.s_score limit 1,1) as "倒数第二名",
	     (select s.s_score from score as s where s.c_id = sc.c_id order by s.s_score limit 2,1) as "倒数的探花"

from score as sc 
group by sc.c_id; 

-- 22.2 转化为小于自己成绩的数据少于3  【2】
select sc.c_id,sc.s_score
from score as sc 
where (select count(*) from score as s where s.c_id = sc.c_id and s.s_score > sc.s_score ) < 3
order by sc.c_id, sc.s_score;
```



23. 查询每门课程被选修的学生数

```mysql
-- 23.1 /* 根据课程分组，对学生人数计数，为了防止有些课程没有人选修要用左连接 */
select c.c_id, c.c_name, count(*) as num
from course as c left join score as s on c.c_id = s.c_id
group by c.c_id;
```



24. 查询出只选修两门课程的学生学号和姓名

```mysql
/* 按学生分组，统计学生学习的课程数，选择课程数是2的学生s_id，匹配学生表，找出其姓名 */
-- 24.1 按学生分组，统计学生学习的课程数，选择课程数是2的学生s_id 【4】
select s.s_id, count(*) as num  
from score as s 
group by s.s_id 
having num = 2;

-- 24.2 匹配学生表，找出其姓名  【5】
select a.s_id, st.s_name
from student as st join 
(select s.s_id, count(*) as num
from score as s 
group by s.s_id 
having num = 2) as a
on st.s_id = a.s_id;
```



25. 查询男生、女生人数

```mysql
/* 直接对学生表按性别分组，然后各自统计人数即可 */
select st.s_sex,count(*) as  num_sex
from student as st 
group by st.s_sex; 
```



26. 统计学生中不同性别人数

```mysql
-- 26.1 这是一维表的表达方式
select  st.s_sex as "性别", count(*) "人数"
from student as st 
group by st.s_sex;

-- 26.2 二维表表达
select sum(case when st.s_sex = "男" then 1 else 0 end) as "男生人数",
       sum(case when st.s_sex = "女" then 1 else 0 end) as "女生人数"
from student as st 
group by st.s_sex;

-- 26.3 也是一种一维表
select sum(case when st.s_sex = "男" then 1 else 0 end) as "男生人数",
       sum(case when st.s_sex = "女" then 1 else 0 end) as "女生人数"
from student as st;
```



27. 查询名字中含有「风」字的学生信息

```mysql
select *
from student as st 
where st.s_name like "%风%"; 
```



28. 查询同名同性学生名单，并统计同名人数

```mysql
-- 28.1 查询同名同姓的名单，并统计同名人数  【9】
select s.s_name, s.s_sex, count(*)
from student as st, student as s
where st.s_name = s.s_name and st.s_sex = s.s_sex and st.s_id != s.s_id
group by s.s_name;

-- 28.2 查询同名同姓的名单，并统计同名人数  【10】
select s.s_name, s.s_sex, count(*)
from student as st join student as s
on st.s_name = s.s_name and st.s_sex = s.s_sex and st.s_id != s.s_id
group by s.s_name;
```



29. 查询 1990 年出生的学生名单

```mysql
-- 29.1 like 用法 【1】
select st.*
from student as st 
where st.s_birth like '1990%';

-- 29.2 date&like用法 【2】
select st.*, date(st.s_birth) as norm
from student as st 
where date(st.s_birth) like '1990%';
```



30. 查询每门课程的平均成绩，结果按平均成绩降序排列，平均成绩相同时，按课程编号升序排列

```mysql
-- 30.1 课程表和得分表做一个左连接，防止有些课程没有人选  【3】
select c.*, s.s_id, s.s_score
from course as c left join score as s
on c.c_id = s.c_id;

-- 30.2 根据上述表对课程ID分组，求均成绩，然后排序   【4】
select t.c_id, t.c_name, round(avg(t.s_score),1) as avg_score
from (select c.*, s.s_id, s.s_score
from course as c left join score as s
on c.c_id = s.c_id) as t
group by t.c_id
order by avg_score desc, t.c_id asc;
```



31. 查询平均成绩大于等于 85 的所有学生的学号、姓名和平均成绩

```mysql
-- 31.1 得分表先对学生ID分组，求出选课学生的均值  【5】
select sc.s_id, round(avg(sc.s_score),1) as avg_score
from score as sc 
group by sc.s_id
having avg_score >= 85;

-- 31.2 根据上述学生ID和学生表做连接  【6】
select st.s_id, st.s_name, t.avg_score
from student as st join 
(select sc.s_id, round(avg(sc.s_score),1) as avg_score
from score as sc 
group by sc.s_id
having avg_score >= 85) as t
on st.s_id = t.s_id;

-- 31.3 也可以先连接学生表和得分表，再分组  【7】
select st.s_id, st.s_name, round(avg(sc.s_score),1) as avg_score  -- 能先连接就先连接，不用子查询
from student as st left join score as sc 
on st.s_id = sc.s_id
group by st.s_id
having avg_score >= 85;
```



32. 查询课程名称为「数学」，且分数低于 60 的学生姓名和分数

```mysql
-- 32.1 连接课程表和得分表
select c.c_id, st.s_name, sc.s_score
from course as c 
join score as sc on c.c_id = sc.c_id 
join student as st on st.s_id = sc.s_id
where c.c_name = "数学" and sc.s_score < 60;
```



33. 查询所有学生的课程及分数情况

```mysql
-- 33.1 可能存在有些学生没有选课，需要用左连接  【9】  -- 缺陷只选了部分课程的学生，其他课程没有显示null
select st.s_id, st.s_name,st.s_sex, c.c_id,c.c_name,sc.s_score
from student as st 
left join score as sc on st.s_id = sc.s_id
left join course as c on c.c_id = sc.c_id;

-- 33.2 二维表思路
select *,
			 max(case when sc.c_id = 01 then sc.s_score else null end) as "语文", -- 聚合函数也可以是min
			 max(case when sc.c_id = 02 then sc.s_score else null end) as "数学",
			 max(case when sc.c_id = 03 then sc.s_score else null end) as "英语" 
from student as st 
left join score as sc on st.s_id = sc.s_id
group by st.s_id;


-- 33.3 二维表思路  【11】
select st.s_id, st.s_name,st.s_sex,  -- 可以先筛选这三个字段，然后就知道怎么写后面三个字段了
			 (select s.s_score from score as s where s.c_id = 01 and s.s_id = sc.s_id) as "语文",
			 (select s.s_score from score as s where s.c_id = 02 and s.s_id = sc.s_id) as "数学",
			 (select s.s_score from score as s where s.c_id = 03 and s.s_id = sc.s_id) as "英语"
from student as st 
left join score as sc on st.s_id = sc.s_id
group by st.s_id;
```



34. 查询任何一门课程成绩在 70 分以上的姓名、课程名称和分数

```mysql
/* 找出每个人的所学课程的最低成绩，与70比较，筛选s_id,c_id,s_score */
-- 34.1 找出每个人的所学课程的最低成绩，与70比较   【1】
select sc.s_id, min(sc.s_score) as min_score
from score as sc 
group by sc.s_id
having min_score > 70;

-- 34.2 根据上面查找的学生ID查找对应的数据         【2】
select st.s_name, c.c_name, sc.s_score
from (select sc.s_id, min(sc.s_score) as min_score
from score as sc 
group by sc.s_id
having min_score > 70) as t
inner join student as st on t.s_id = st.s_id  -- 这里是内连接
inner join score as sc on sc.s_id = st.s_id
inner join course as c on c.c_id = sc.c_id;
```



35. 查询存在一门成绩超过70分的姓名、课程名称和分数

```mysql
/* 只要最高分大于70份即可 */
-- 35.1 找出每个人的最高分，并筛选大于70份以上的人   【3】
select t.s_id, t.s_name, t.c_id, t.c_name, sc.s_score
from (select st.s_id, st.s_name, c.c_id, c.c_name, max(sc.s_score) as max_score
from student as st 
inner join score as sc on st.s_id = sc.s_id 
inner join course as c on sc.c_id = c.c_id
group by st.s_id
having max_score > 70) as t
inner join score as sc on t.s_id = sc.s_id;
```



36. 查询存在不及格的课程

```mysql
-- 36.1 查询存在不及格的课程   【4】
/* 根据课程分组找出其最小值，筛选小于60分的课程 */
select c.c_id, c.c_name, min(sc.s_score) as min_score
from course as c 
left join score as sc on c.c_id = sc.c_id
group by c.c_id 
having min_score < 60;

-- 36.2 查询存在不及格的课程   【5】
/* 左连接后，直接按照成绩少选，筛选不同的课程和对应的名字 */
SELECT distinct c.c_id, c.c_name
from course as c 
left join score as sc on sc.c_id = c.c_id
where sc.s_score < 60;
```



37. 查询课程编号为 01 且课程成绩在 80 分以上的学生的学号和姓名

```mysql
/* 内连接学生表和成绩表，再筛选 */
select st.s_id, st.s_name
from student as st 
inner join score as sc on st.s_id = sc.s_id
and sc.c_id = 01 and sc.s_score >= 80;
```



38. 求每门课程的学生人数

```mysql
/* 课程需要以课程白为依据，防止，有些课根本就没有人选，因此需要用左连接 */
select c.c_id,count(*) as st_num
from course as c
left join score as sc on c.c_id = sc.c_id
group by c.c_id;
```



39. 成绩不重复，查询选修「张三」老师所授课程的学生中，成绩最高的学生信息及其成绩

```mysql
/* 张三老师和学生信息，需要通过四张表相互连接在一起，这里用内连接即可 */
-- 39.1 以下用的是聚合函数                                                      【8】
select st.*, c.c_id, max(sc.s_score) as max_score
from teacher as t 
inner join course as c on t.t_id = c.t_id and t.t_name = '张三'
inner join score as sc on c.c_id = sc.c_id
inner join student as st on sc.s_id = st.s_id;

-- 39.2 以下用的是排序法                                                   【9】
select st.*, c.c_id, sc.s_score
from teacher as t 
inner join course as c on t.t_id = c.t_id and t.t_name = '张三'
inner join score as sc on c.c_id = sc.c_id
inner join student as st on sc.s_id = st.s_id
order by sc.s_score desc limit 0,1;

-- 39.3 成绩不重复，查询选修「张三」老师所授课程的学生中，成绩第三高的学生信息及其成绩 
/* 成绩不重复查询可以采用join或where连接 */
select st.*, c.c_id, sc.s_score
from teacher as t 
inner join course as c on t.t_id = c.t_id and t.t_name = '张三'
inner join score as sc on c.c_id = sc.c_id
inner join student as st on sc.s_id = st.s_id
order by sc.s_score desc limit 2,1;  
```



40. 成绩有重复的情况下，查询选修「张三」老师所授课程的学生中，成绩最高的学生信息及其成绩

```mysql
-- 40.1 筛选张三老师教授的课程中的最高成绩及其对应的课程   【1】
select c.c_id, sc.s_score
from course as c, score as sc, teacher as t
where t.t_name = "张三" and t.t_id = c.t_id and c.c_id = sc.c_id
order by sc.s_score desc limit 1;

-- 40.2 根据上述筛选的课程和最高成绩，找出那个学生的信息   【2】
select st.*, mt.c_id , mt.s_score
from score as sc,student as st,
(select c.c_id, sc.s_score
from course as c, score as sc, teacher as t
where t.t_name = "张三" and t.t_id = c.t_id and c.c_id = sc.c_id
order by sc.s_score desc limit 1) as mt
where mt.c_id = sc.c_id and mt.s_score = sc.s_score and sc.s_id = st.s_id;
```



41. 查询不同课程成绩相同的学生的学生编号、课程编号、学生成绩

```mysql
-- 41.1 学生编号、课程编号、学生成绩都反映在成绩表，只需在成绩表操作   【3】
/* 各自命名连接*/
select distinct sc.*
from score as sc, score as ss 
where sc.s_score = ss.s_score and sc.c_id <> ss.c_id and sc.s_id = ss.s_id;

--  41.2 学生编号、课程编号、学生成绩都反映在成绩表，只需在成绩表操作  【4】
/* 自连接*/
select  distinct sc.*
from score as sc inner join score as ss 
on sc.c_id != ss.c_id and sc.s_score = ss.s_score and sc.s_id = ss.s_id;
```



42. 查询每门课程成绩最好的前两名

```mysql
/* 按每门课程分组，选择一维表或二维表 */  
-- 42.1 二维表思路                          【5】
select sc.c_id,
			 max(sc.s_score) as "第一名",
			 (select ss.s_score from score as ss where sc.c_id = ss.c_id order by ss.s_score desc limit 1,1) as "第二名"  -- 注意select和order by是ss，不是sc
from score as sc 
group by sc.c_id;

-- 42.2 一维表思路                          【6】
/* 转化为大于某个成绩的数少于2 */
select  sc.c_id, sc.s_score
from score as sc 
where (select count(*) from score as ss where ss.c_id = sc.c_id and ss.s_score > sc.s_score) < 2
order by sc.c_id asc, sc.s_score desc;
```



43. 查询各科成绩前三的学生信息

```mysql
-- 43.1 查询各科成绩前三课学生ID,课程ID和成绩 【7】
/* 这里必须是一维表，转化为大于某个成绩的数少于2 */
select sc.*
from score as sc 
where (select count(*) from score as ss where ss.c_id = sc.c_id and ss.s_score > sc.s_score) < 2
order by sc.c_id asc, sc.s_score desc; 

-- 43.2 根据上述信息，查询对应学生信息和所学课程和成绩  【8】
select distinct st.*, mt.c_id, mt.s_score
from student as st 
inner join (select sc.*
from score as sc 
where (select count(*) from score as ss where ss.c_id = sc.c_id and ss.s_score > sc.s_score) < 2
order by sc.c_id asc, sc.s_score desc) as mt
on mt.s_id = st.s_id;
```



44. 统计每门课程的学生选修人数（超过5人的课程才统计）

```mysql
/* 按课程分组，统计学生人数，然后过滤 */
select c.c_id, count(distinct sc.s_id) as st_num -- 担心有人重修，这里去重
from course as c
left join score as sc on c.c_id = sc.c_id
group by c.c_id
having st_num >5;
```



45. 检索至少选修两门课程的学生学号

```mysql
/* 按学生ID分组，计算选修课程，然后过滤 */
select st.*, count(distinct sc.c_id) as st_num -- 担心有人重修，这里去重
from student as st 
left join score as sc on st.s_id = sc.s_id
group by st.s_id
having st_num >=2;
```



46. 查询选修了全部课程的学生信息

```mysql
/* 按学生分组，统计其学习的课程数，且与全部的课程数比较，过滤 */
-- 46.1 按学生分组，统计其学习的课程数，且与全部的课程数比较，过滤
select sc.s_id, count(distinct sc.c_id) as st_num  -- 避免有人重修，筛选过滤  【1】
from score as sc 
group by sc.s_id
having st_num = (select count(distinct c.c_id) from course as c); 

-- 46.2 按照上述信息与学生表联结                                              【2】
select st.*
from student as st 
inner join (
select sc.s_id, count(distinct sc.c_id) as st_num  -- 避免有人重修，筛选过滤
from score as sc 
group by sc.s_id
having st_num = (select count(distinct c.c_id) from course as c)) as mt
on st.s_id = mt.s_id;
```



47. 查询各学生的年龄，只按年份来算

```mysql
/* 按当下的年份减去出生年份，作为新字段，当前月份比出生月份小于于等于减1 */          
select st.s_name, st.s_birth, 
			 DATE_FORMAT(now(),"%Y")-DATE_FORMAT(st.s_birth,"%Y")- 
			 (case when DATE_FORMAT(now(),"%m%d")>DATE_FORMAT(st.s_birth,"%m%d") then 0 else 1 end) as age,
			 DATE_FORMAT(now(),"%Y") as now_year1, DATE_FORMAT(now(),"%y") as now_year2,
			 DATE_FORMAT(st.s_birth,"%m%d") as bir_day1,
			 DATE_FORMAT(st.s_birth,"%M%D") as bir_day2
from student as st; 

-- 47.2 检查一下DATE_FORMAT格式大小写是否有区别                                【4】
select st.s_name, st.s_birth, 
			 DATE_FORMAT(now(),"%y")-DATE_FORMAT(st.s_birth,"%y")- 
			 (case when DATE_FORMAT(now(),"%M%D")>DATE_FORMAT(st.s_birth,"%m%d") then 0 else 1 end) as age, DATE_FORMAT(now(),"%y") as now_year
from student as st;
```



48. 查询本周过生日的学生

```mysql
select st.*
from student as st 
where week(now(),1) = week(st.s_birth,1);  -- 1表示按周一开始计算周期
```



49. 查询下周过生日的学生

```mysql
-- 49.1 方法1尾巴加1表示
select st.*
from student as st 
where week(now(),1) + 1 = week(st.s_birth,1);  -- 1表示按周一开始计算周期
```



50. 查询本月过生日的学生

```mysql
select st.*
from student as st 
where month(now()) = month(st.s_birth);
```



51. 查询本季度过生日的学生

```mysql
select st.*
from student as st
where quarter(now()) = quarter(st.s_birth);
```



52. 查询下月过生日的学生

```mysql
select st.*, month(st.s_birth)+1 as next_month
from student as st 
where month(now())+1 = month(st.s_birth);
```



53. 根据学生，学科汇总单科成绩，总成绩和均值成绩

```mysql
-- 53.1 单科成绩，其实也可以直接使用成绩表
select st.s_name, c.c_name, st.s_id, sc.c_id, sc.s_score as "单科分数"
       -- max(case when sc.c_id=01 then sc.s_score else 0 end) as "语文" 
from student as st
inner join score as sc on st.s_id = sc.s_id 
inner join course as c on c.c_id = sc.c_id
group by st.s_name,c.c_name
order by c.c_name;

-- 53.2 汇总总成绩和平均成绩
select st.s_name, sum(sc.s_score) as "总成绩", round(avg(sc.s_score),1) as "平均成绩"
from student as st
inner join score as sc
on st.s_id = sc.s_id 
group by st.s_name;

-- 53.3 汇总上述表格
select m.*, n.`平均成绩`, n.`总成绩`
from (select st.s_name, c.c_name, st.s_id, sc.c_id, sc.s_score as "单科分数"
       -- max(case when sc.c_id=01 then sc.s_score else 0 end) as "语文" 
from student as st
inner join score as sc on st.s_id = sc.s_id 
inner join course as c on c.c_id = sc.c_id
group by st.s_name,c.c_name
order by c.c_name) as m
inner join (select st.s_name, sum(sc.s_score) as "总成绩", round(avg(sc.s_score),1) as "平均成绩"
from student as st
inner join score as sc
on st.s_id = sc.s_id 
group by st.s_name) as n
on m.s_name = n.s_name;
```

# 资料来自

- https://zhuanlan.zhihu.com/p/55124789