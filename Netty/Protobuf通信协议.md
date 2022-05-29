**Protobuf的编码过程为**：使用预先定义的Message数据结构将实际数据进行打包，然后编码成二进制的码流进行传输或者存储。

**Protobuf的解码过程为：**将二进制码流解码成Protobuf自己定义的Message数据结构的POJO实例。

使用方法：

```protobuf
// 定义proto协议版本
syntax = "proto3";

package com.floweryu.netty.protobuf.bean;

// 生成POJO类将代码放入指定包中
option java_package = "com.floweryu.netty.protobuf.bean";
// 生成POJO类时的名称 
option java_outer_classname = "MsgProtos";

message Msg {
  uint32 id = 1;
  string conetent = 2;
}

```

引入依赖：

```xml
<!-- https://mvnrepository.com/artifact/com.google.protobuf/protobuf-java -->
<dependency>
    <groupId>com.google.protobuf</groupId>
    <artifactId>protobuf-java</artifactId>
    <version>3.21.1</version>
</dependency>
```

这里使用maven进行编译：

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.xolstice.maven.plugins</groupId>
            <artifactId>protobuf-maven-plugin</artifactId>
            <version>0.5.0</version>
            <extensions>true</extensions>
            <configuration>
                <!--需要编译的proto文件位置-->
                <protoSourceRoot>${project.basedir}/proto</protoSourceRoot>
                <!--默认值-->
                <!--<outputDirectory>${project.build.directory}/generated-sources/protobuf/java</outputDirectory>-->
                <outputDirectory>${project.build.sourceDirectory}</outputDirectory>
                <!--设置是否在生成java文件之前清空outputDirectory的文件，默认值为true，设置为false时也会覆盖同名文件-->
                <clearOutputDirectory>false</clearOutputDirectory>
                <!--编译输出文件位置-->
                <temporaryProtoFileDirectory>${project.build.directory}/protoc-temp</temporaryProtoFileDirectory>
                <!--更多配置信息可以查看https://www.xolstice.org/protobuf-maven-plugin/compile-mojo.html-->
                <!--protoc编译器位置-->
                <protocExecutable>${project.basedir}/proto/protobin/protoc.exe</protocExecutable>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>compile</goal>
                        <goal>test-compile</goal>
                    </goals>

                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

整个目录结构：

![image-20220529193436412](https://raw.githubusercontent.com/Floweryu/typora-img/main/img/202205291934639.png)

测试类：

```java
public static MsgProtos.Msg buildMsg() {
    MsgProtos.Msg.Builder builder = MsgProtos.Msg.newBuilder();
    builder.setConetent("zzzzzzfffff");
    builder.setId(999);
    MsgProtos.Msg message = builder.build();
    return message;
}
```

