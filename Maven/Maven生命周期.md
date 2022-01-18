### 1. clean生命周期

clean生命周期的目的是清理项目，包含下面三个阶段：

- **pre-clean**：执行一些清理前需要完成的工作
- **clean**：清理上一次构建生成的文件
- **post-clean**：执行一些清理后需要完成的工作

### 2. default生命周期

default生命周期定义了真正构建时所需要执行的所有步骤：

- **validate**
- **initialize**
- **generate-sources**
- **process-sources**：处理项目主资源文件。对src/main/resources目录内容进行变量替换后，复制到项目输出的主classpath目录中
- **generate-resources**
- **process-resources**
- **compile**：编译项目的主源码。对src/main/java目录下的Java文件至项目输出的主classpath目录中
- **process-classes**
- **generate-test-sources**
- **process-test-sources**：处理项目测试资源文件。对src/test/resources目录的内容进行变量替换后，复制到项目输出的测试classpath目录中
- **generate-test-resources**
- **process-test-resources**
- **test-compile**：编译项目的测试代码。编译src/test/java目录下的Java文件至项目输出的测试classpath目录中
- **process-test-classes**
- **test**：使用单元测试框架运行测试，测试代码不会被打包或部署
- **prepare-package**
- **package**：接收编译好的代码，打包成可发布的格式，如JAR
- **pre-integration-test**
- **integration-test**
- **post-integration-test**
- **verify**
- **install**：将包安装到Maven本地仓库，供本地其它Maven项目使用
- **deploy**：将最终的包复制到远程仓库，供其它开发人员和Maven项目使用

### 3. site生命周期

site生命周期的目的是建立和发布项目站点，Maven能基于POM所包含的信息，自动生成一个友好的站点，方便团队交流和发布项目信息

- **pre-site**：执行一些在生成项目站点之前需要完成的工作
- **site**：生成项目站点文档
- **post-site**：执行一些在生成项目站点之后需要完成的工作
- **site-deploy**：将生成的项目站点发布到服务器上

### 4. 命令行与生命周期

各个生命周期是相互独立的，一个生命周期的阶段是前后有依赖关系的。

- **mvn clean**：该命令调用clean生命周期的clean阶段。实际执行的阶段是clean生命周期的**pre-clean**和**clean**阶段
- **mvn test**：该命令调用default生命周期的test阶段。实际执行的阶段是default生命周期的**validate**到**test**之前的所有阶段
- **mvn clean install**：该命令调用clean生命周期的**clean**阶段，和default生命周期的**install**阶段。实际执行的阶段为**clean**生命周期的**pre-clean**、**clean**阶段，default生命周期从**validate**到**install**的所有阶段

### 5. 插件绑定的生命周期和任务

![image-20220118155558712](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/202201181556041.png)