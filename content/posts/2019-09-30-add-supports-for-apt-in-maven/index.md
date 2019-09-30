+++
title = "在 Maven 中支持 Java 的注解处理器 APT"
description = "Java 中有很多基于 APT 代码生成技术的类库。在 Maven 中启用对 APT 的支持，需要配置 annotationProcessorPaths 或添加 optional 的依赖。"
date = 2019-09-30T12:36:55+08:00
draft = false
template = "page.html"
[taxonomies]
categories =  ["Java"]
tags = ["java", "apt", "maven", "annotationProcessorPaths"]
+++

Java 中有很多基于 [注解处理器 APT (Annotation Processing Tool)][apt] 技术的类库，如 [AutoValue][autovalue] 和 [FreeBuilder][freebuilder] 等。

在 [Maven](https://maven.apache.org/) 中支持 [APT](apt) ，需要在 [Apache Maven Compiler Plugin](https://maven.apache.org/plugins/maven-compiler-plugin/) 的配置部分添加 [annotationProcessorPaths](https://maven.apache.org/plugins/maven-compiler-plugin/compile-mojo.html#annotationProcessorPaths) 的配置，如下：

```xml
<plugin>
	<groupId>org.apache.maven.plugins</groupId>
	<artifactId>maven-compiler-plugin</artifactId>
	<version>3.6.1</version>
	<configuration>
		<source>1.8</source>
		<target>1.8</target>
		<testSource>1.8</testSource>
		<testTarget>1.8</testTarget>
		<encoding>UTF-8</encoding>
		<optimize>true</optimize>
		<!-- Slightly faster builds, see https://issues.apache.org/jira/browse/MCOMPILER-209 -->
		<useIncrementalCompilation>false</useIncrementalCompilation>
		<annotationProcessorPaths>
			<path>
				<groupId>com.google.auto.value</groupId>
				<artifactId>auto-value</artifactId>
				<version>${auto-value.version}</version>
			</path>
		</annotationProcessorPaths>
	</configuration>
</plugin>
```

上述配置对于 [Maven](https://maven.apache.org/) `3.5` 以上版本有效。

对于低于 `3.5` 的版本，可以在 `dependencies` 块中添加依赖项，并设置 `optional` 属性。

```xml
<dependency>
	<groupId>org.inferred</groupId>
	<artifactId>freebuilder</artifactId>
	<version>${freebuilder_version}</version>
	<optional>true</optional>
</dependency>
```

如果是可执行的工程，也可以设置 `scope` 为 `provided` 。

```xml
<dependency>
	<groupId>org.inferred</groupId>
	<artifactId>freebuilder</artifactId>
	<version>${freebuilder_version}</version>
	<scope>provided</scope>
</dependency>
```

<!-- more -->

> If specified, the compiler will detect annotation processors only in those classpath elements. If
> omitted, the default classpath is used to detect annotation processors.

根据 [官方文档](https://maven.apache.org/plugins/maven-compiler-plugin/compile-mojo.html#annotationProcessorPaths) 的说明，如果同时配置了 `annotationProcessorPaths` 和 `dependencies`，只有 `annotationProcessorPaths`
中的注解处理器会被加载使用。

---

以上。

[autovalue]: https://github.com/google/auto/tree/master/value "AutoValue - Immutable value-type code generation for Java 1.6+."
[apt]: https://docs.oracle.com/javase/7/docs/technotes/guides/apt/ "Annotation Processing Tool (apt)"
[freebuilder]: https://freebuilder.inferred.org/ "Automatic generation of the Builder pattern for Java"
