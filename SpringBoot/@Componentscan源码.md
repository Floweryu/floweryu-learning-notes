在`org.springframework.context.annotation.ConfigurationClassParser#doProcessConfigurationClass`函数中，实现了很多注解。

截取一段@Component注解的实现方法：

```java
Set<AnnotationAttributes> componentScans = AnnotationConfigUtils.attributesForRepeatable(
    sourceClass.getMetadata(), ComponentScans.class, ComponentScan.class);
if (!componentScans.isEmpty() &&
    !this.conditionEvaluator.shouldSkip(sourceClass.getMetadata(), ConfigurationPhase.REGISTER_BEAN)) {
    for (AnnotationAttributes componentScan : componentScans) {
        // The config class is annotated with @ComponentScan -> perform the scan immediately
        Set<BeanDefinitionHolder> scannedBeanDefinitions =
            this.componentScanParser.parse(componentScan, sourceClass.getMetadata().getClassName());
        // Check the set of scanned definitions for any further config classes and parse recursively if needed
        for (BeanDefinitionHolder holder : scannedBeanDefinitions) {
            BeanDefinition bdCand = holder.getBeanDefinition().getOriginatingBeanDefinition();
            if (bdCand == null) {
                bdCand = holder.getBeanDefinition();
            }
            if (ConfigurationClassUtils.checkConfigurationClassCandidate(bdCand, this.metadataReaderFactory)) {
                parse(bdCand.getBeanClassName(), holder.getBeanName());
            }
        }
    }
}
```

获取对应注解的配置信息，即对应的`@ComponentScan({"com.spring.study.module"})`中主要的扫描路径信息，然后传递给ComponentScanAnnotationParser的parser进一步扫描：

```java
Set<BeanDefinitionHolder> scannedBeanDefinitions =
    this.componentScanParser.parse(componentScan, sourceClass.getMetadata().getClassName());
```

在上述parse方法方法内部，有一个核心函数：

```java
ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(this.registry,
				componentScan.getBoolean("useDefaultFilters"), this.environment, this.resourceLoader);
```

是Spring的核心解析类，通过字节码扫描的方式，效率比其他工具高出很多，**这段代码可以在日常工作中直接使用**。