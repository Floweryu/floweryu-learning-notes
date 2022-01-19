### 可继承的POM元素

- `groupId`：项目组ID，项目坐标核心元素
- `version`：项目版本，项目坐标核心元素
- `description`：项目描述信息
- `organization`：项目的组织信息
- `inceptionYear`：项目的创始年份
- `url`：项目的URL地址
- `developers`：项目开发者信息
- `contributors`：项目贡献者信息
- `distributionManagement`：项目部署配置
- `issueManagement`：项目缺陷跟踪系统信息
- `ciManagement`：项目持续集成系统信息
- `scm`：项目版本控制系统信息
- `mailingLists`：项目邮件列表信息
- `properties`：自定义的Maven属性
- `dependencies`：项目的依赖配置
- `dependencyManagement`：项目的依赖管理配置
- `repositories`：项目仓库配置
- `build`：包括项目的源码目录配置、输出目录配置、插件配置、插件管理配置等
- `reporting`：包括项目的报告输出目录配置、报告插件配置等

### 依赖管理

在父模块中使用`dependencyManagement`元素既能让子模块继承到父模块的依赖配置，又能保证子模块依赖使用的灵活性。在`dependencyManagement`元素下的依赖声明不会引入实际的依赖，但是能够约束`dependencies`下依赖的使用。

可以在父模块中使用`dependencyManagement`依赖，在子模块中只用再声明依赖的`groupId`和`artifactId`即可，不需要再声明`version`及其它已经再父模块设置的变量。

### 插件管理

`pluginManagement`元素可以帮助管理插件，使用方法和`dependencyManagement`一致。

### 反应堆及构建顺序

假如一个项目的结构如下：

![image-20220119114523858](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/202201191145378.png)

进行构建可以得到下面输出：

![image-20220119141028102](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/202201191410668.png)



输出的顺序就是构建的顺序。但是这个顺序和POM中写的不一致。

**实际的构建顺序：Maven按序读取POM，如果POM中没有依赖的模块，就构建该模块。否则就构建其依赖的模块，如果该依赖还依赖于其它模块，则构建依赖的依赖。**

**因此，如果出现模块A依赖于B，而B又依赖于A的情况时，Maven就会报错。**

#### 裁剪反应堆

可以使用下面命令选择性构建模块，输入`mvn -h`可以看到：

- `-am,--also-make`：同时构建所列出模块的依赖模块
- `-amd,--also-make-dependents`：同时构建依赖于所列出模块的模块
- `-pl,--projects <arg>`：构建指定的模块，模块见用逗号分隔
- `-rf,--resume-from <arg>`：从指定的模块回溯反应堆