+++
title = "Spring MVC Unit Testing - Configuration"
description = "Spring MVC Unit Testing - Configuration"
date = 2016-04-09T23:06:57+08:00
draft = false
[taxonomies]
categories =  ["Spring MVC Testing"]
tags = ["Spring MVC", "testing", "java"]
+++

本文是 [Spring MVC Testing](/2016/04/09/spring-mvc-testing-content/) 单元测试系列的第1篇，原文链接：[Unit Testing of Spring MVC Controllers: Configuration](http://www.petrikainulainen.net/programming/spring-framework/unit-testing-of-spring-mvc-controllers-configuration/)。

一直以来，为Spring MVC的Controller写单元测试的工作既简单又问题多多。简单体现在单元测试可以很简单地写个测试用例调用一下目标Controller的方法；问题在于这种单元测试完全没有用（不是HTTP的请求），比如说，这种单元测试的方法没办法测试请求映射、参数验证和异常映射等。

幸运的是，从Spring 3.2开始，我们可以使用[Spring MVC Test Framework](http://docs.spring.io/spring/docs/3.2.x/spring-framework-reference/htmlsingle/#new-in-3.2-spring-mvc-test)这一强大的工具通过DispatcherServlet来仿照HTTP请求的方式来单元测试Controller的方法。

本文主要介绍如何配置Spring使得可以单元测试Spring MVC Controllers。

下面进入正题。

<!-- more -->

# 通过Maven获取依赖

本系列用到的依赖如下：

- JUnit 4.11
- Mockito Core 1.9.5
- Spring Test 3.2.3.RELEASE

生成的`pom.xml`文件的片段如下：

```xml
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.11</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>1.9.5</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-test</artifactId>
    <version>3.2.3.RELEASE</version>
    <scope>test</scope>
</dependency>
```

然后，我们进一步去看一下示例程序。

# 示例程序的结构

本教程的示例程序提供用于访问todo项的增删改查（CRUD）入口。为了更好地理解测试配置，首先看一下需要测试的controller类。

到目前为止，我们需要回答以下两个问题：

- 待测试的controller类有哪些依赖
- 这些以来是如何注入待测试的controller类

我们可以创建的`TodoController`目标类的代码中去查找答案。相关代码如下：

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
 
@Controller
public class TodoController {
    private final TodoService service;

    private final MessageSource messageSource;

    @Autowired
    public TodoController(MessageSource messageSource, TodoService service) {
    this.messageSource = messageSource;
    this.service = service;
    }

    //Other methods are omitted.
}
```

从代码中可以看出，`TodoController`类依赖于`TodoService`类和`MessageSource`类，并且使用的是构造器注入。

好了，到目前我们已经知道了需要的信息，下一步是去了解上下文配置信息。

# 程序上下文配置

为程序在生产环境和测试分别准备不同的上下文配置是不合算的，除了增加额外的工作量之外，还将导致二者配置不一致的问题，比如我们修改了生产环境的配置但是忘了修改测试配置的话。

所以我们将配置上下文按功能片段进行拆分，以使生产环境和测试环境可以做各自的自定义配置，还能最大程度的共用配置。

我们将程序配置拆分为3个部分。

1. 第一部分称之为主配置，主要是配置webapp相关的信息
2. 第二部分称之为Web配置，主要是配置Controller层的注入等信息
3. 第三部分称之为持久层配置，主要包含程序的持久层信息

注意：因为Spring同时支持Java类配置和XML配置，所以下面的配置信息都会给出两者的配置方式。

下一步我们看一下主配置的主要设置内容，以及我们如何使用Spring的方式进行配置。

# 主配置（生产环境）

本示例程序的主配置主要做如下工作：

1. 启用Spring MVC对`@Controller`注解的支持
2. 配置静态资源的路由位置
3. 配置静态资源由容器的默认servlet解析
4. 配置Bean搜索的包路径
5. 配置`ExceptionResolver` bean
6. 配置`ViewResolver` bean

我们直接看看使用Java类配置和XML配置的结果。

## Java类配置

如果使用Java类配置方式，配置类`WebAppContext`的代码如下：

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;
 
import java.util.Properties;
 
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {
        "net.petrikainulainen.spring.testmvc.common.controller",
        "net.petrikainulainen.spring.testmvc.todo.controller"
})
public class WebAppContext extends WebMvcConfigurerAdapter {
 
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(./posts/static/**").addResourceLocations("/static/");
    }
 
    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }
 
    @Bean
    public SimpleMappingExceptionResolver exceptionResolver() {
        SimpleMappingExceptionResolver exceptionResolver = new SimpleMappingExceptionResolver();
 
        Properties exceptionMappings = new Properties();
 
        exceptionMappings.put("net.petrikainulainen.spring.testmvc.todo.exception.TodoNotFoundException", "error/404");
        exceptionMappings.put("java.lang.Exception", "error/error");
        exceptionMappings.put("java.lang.RuntimeException", "error/error");
 
        exceptionResolver.setExceptionMappings(exceptionMappings);
 
        Properties statusCodes = new Properties();
 
        statusCodes.put("error/404", "404");
        statusCodes.put("error/error", "500");
 
        exceptionResolver.setStatusCodes(statusCodes);
 
        return exceptionResolver;
    }
 
    @Bean
    public ViewResolver viewResolver() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
 
        viewResolver.setViewClass(JstlView.class);
        viewResolver.setPrefix(./posts/WEB-INF/jsp/");
        viewResolver.setSuffix(".jsp");
 
        return viewResolver;
    }
}
```

## XML配置

如果使用XML配置方式，配置文件`exampleApplicationContext-web.xml`的内容如下：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:mvc="http://www.springframework.org/schema/mvc"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.springframework.org/schema/mvc http://www.springframework.org/schema/mvc/spring-mvc-3.1.xsd
       http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context-3.1.xsd">
 
    <mvc:annotation-driven/>
 
    <mvc:resources mapping="/static/**" location="/static/"/>
    <mvc:default-servlet-handler/>
 
    <context:component-scan base-package="net.petrikainulainen.spring.testmvc.common.controller"/>
    <context:component-scan base-package="net.petrikainulainen.spring.testmvc.todo.controller"/>
 
    <bean id="exceptionResolver" class="org.springframework.web.servlet.handler.SimpleMappingExceptionResolver">
        <property name="exceptionMappings">
            <props>
                <prop key="net.petrikainulainen.spring.testmvc.todo.exception.TodoNotFoundException">error/404</prop>
                <prop key="java.lang.Exception">error/error</prop>
                <prop key="java.lang.RuntimeException">error/error</prop>
            </props>
        </property>
        <property name="statusCodes">
            <props>
                <prop key="error/404">404</prop>
                <prop key="error/error">500</prop>
            </props>
        </property>
    </bean>
 
    <bean id="viewResolver" class="org.springframework.web.servlet.view.InternalResourceViewResolver">
        <property name="prefix" value="/WEB-INF/jsp/"/>
        <property name="suffix" value=".jsp"/>
        <property name="viewClass" value="org.springframework.web.servlet.view.JstlView"/>
    </bean>
</beans>
```

# 主配置（测试环境）

用于测试环境的主配置片段主要用于：

- 配置一个`MessageSource` bean用于依赖注入
- 配置一个`TodoService` bean用于依赖注入

## Java类配置

`TestContext`类的代码如下：

```java
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
 
@Configuration
public class TestContext {
 
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
 
        messageSource.setBasename("i18n/messages");
        messageSource.setUseCodeAsDefaultMessage(true);
 
        return messageSource;
    }
 
    @Bean
    public TodoService todoService() {
        return Mockito.mock(TodoService.class);
    }
}
```

## XML配置

`testContext.xml`文件的内容如下：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">
 
    <bean id="messageSource" class="org.springframework.context.support.ResourceBundleMessageSource">
        <property name="basename" value="i18n/messages"/>
        <property name="useCodeAsDefaultMessage" value="true"/>
    </bean>
 
    <bean id="todoService" name="todoService" class="org.mockito.Mockito" factory-method="mock">
        <constructor-arg value="net.petrikainulainen.spring.testmvc.todo.service.TodoService"/>
    </bean>
</beans>
```

# 测试类的编写

我们可以使用以下两种方式来编写我们的controller测试用例：

- 当待测试的controller类依赖比较简单时，可以使用所谓的`Standalone`方式显示的创建controller实例并手动地配置Spring MVC组件（指ExceptionHandler和ViewResolver等）
- 当待测试的controller类依赖比较复杂时，可以使用所谓的`WebApplicationContext`方式启动一个配置好的`WebApplicationContext`实例

接下来看一下这两种方式分别如何编写测试用例。

## `Standalone`方式

使用`Standalone`方式编写测试用例，我们可以按照如下步骤进行：

1. 对测试用例类加上`@RunWith(MockitoJUnitRunner.class)`注解
2. 测试用例类中添加一个`MockMvc`类型的成员
3. 测试用例类中添加一个`TodoService`类型的成员，并使用`@Mock`进行注解，表示该成员由`MockitoJUnitRunner`进行模拟及赋值
4. 测试用例类中添加一个`exceptionResolver()`方法，用于生成一个配置好的`SimpleMappingExceptionResolver`实例以注入
5. 测试用例类中添加一个`messageSource()`方法，用于生成一个配置好的`ResourceBundleMessageSource`实例以注入
6. 测试用例类中添加一个`validator()`方法，用于生成一个配置好的`LocalValidatorFactoryBean`实例以注入
7. 测试用例类中添加一个`viewResolver()`方法，用于生成一个配置好的`InternalResourceViewResolver`实例以注入
8. 测试用例类中添加一个`setUp()`方法并用`@Before`注解，调用[`MockMvcBuilders`](http://docs.spring.io/spring/docs/3.2.x/javadoc-api/org/springframework/test/web/servlet/setup/MockMvcBuilders.html)的静态方法`standaloneSetup()`创建并配置好`MockMvc`类型的成员

测试用例类的代码如下：

```java
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;
 
import java.util.Properties;
 
@RunWith(MockitoJUnitRunner.class)
public class StandaloneTodoControllerTest {
 
    private MockMvc mockMvc;
 
    @Mock
    private TodoService todoServiceMock;
 
    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TodoController(messageSource(), todoServiceMock))
                .setHandlerExceptionResolvers(exceptionResolver())
                .setValidator(validator())
                .setViewResolvers(viewResolver())
                .build();
    }
 
    private HandlerExceptionResolver exceptionResolver() {
        SimpleMappingExceptionResolver exceptionResolver = new SimpleMappingExceptionResolver();
 
        Properties exceptionMappings = new Properties();
 
        exceptionMappings.put("net.petrikainulainen.spring.testmvc.todo.exception.TodoNotFoundException", "error/404");
        exceptionMappings.put("java.lang.Exception", "error/error");
        exceptionMappings.put("java.lang.RuntimeException", "error/error");
 
        exceptionResolver.setExceptionMappings(exceptionMappings);
 
        Properties statusCodes = new Properties();
 
        statusCodes.put("error/404", "404");
        statusCodes.put("error/error", "500");
 
        exceptionResolver.setStatusCodes(statusCodes);
 
        return exceptionResolver;
    }
 
    private MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
 
        messageSource.setBasename("i18n/messages");
        messageSource.setUseCodeAsDefaultMessage(true);
 
        return messageSource;
    }
 
    private LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }
 
    private ViewResolver viewResolver() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
 
        viewResolver.setViewClass(JstlView.class);
        viewResolver.setPrefix(./posts/WEB-INF/jsp/");
        viewResolver.setSuffix(".jsp");
 
        return viewResolver;
    }
}
```

可以明显的发现，这种方式有两个问题：

- 尽管Spring的配置量变少了，但是测试用例类太难看了，包含了太多无用的与测试无关的代码。我们可以将这些无用代码重构到一个新的类中，这步工作可以留待读者们完成。
- 对于webapp的配置与生产环境的配置重复了，导致二者不能有效的同步。

## `WebApplicationContext`方式

使用`Standalone`方式编写测试用例，我们可以按照如下步骤进行：

1. 对测试用例类加上`@RunWith(MockitoJUnitRunner.class)`注解
2. 对测试用例类加上`@ContextConfiguration`注解，并设置要使用的配置（如果使用Java类配置，请使用`classes`属性；如果使用XML配置，请使用`locations`属性）
3. 对测试用例类加上`@WebAppConfiguration`注解，主要是使用`WebApplicationContext`实例来管理依赖注入
4. 测试用例类中添加一个`MockMvc`类型的成员
5. 测试用例类中添加一个`TodoService`类型的成员，并添加`@AutoWired`注解
6. 测试用例类中添加一个`WebApplicationContext`类型的成员，并添加`@AutoWired`注解
7. 测试用例类中添加一个`setUp()`方法并用`@Before`注解，调用[`MockMvcBuilders`](http://docs.spring.io/spring/docs/3.2.x/javadoc-api/org/springframework/test/web/servlet/setup/MockMvcBuilders.html)的静态方法`webAppContextSetup()`创建并配置好`MockMvc`类型的成员

测试用例类的代码如下：

```java
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
 
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestContext.class, WebAppContext.class})
//@ContextConfiguration(locations = {"classpath:testContext.xml", "classpath:exampleApplicationContext-web.xml"})
@WebAppConfiguration
public class WebApplicationContextTodoControllerTest {
 
    private MockMvc mockMvc;
 
    @Autowired
    private TodoService todoServiceMock;
 
    @Autowired
    private WebApplicationContext webApplicationContext;
 
    @Before
    public void setUp() {
        //We have to reset our mock between tests because the mock objects
        //are managed by the Spring container. If we would not reset them,
        //stubbing and verified behavior would "leak" from one test to another.
        Mockito.reset(todoServiceMock);
 
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }
}
```

使用这种方式使得测试用例类的代码非常干净简洁，但是缺点就是一个测试用例就要加载完整的Spring MVC框架。

# 总结

本文介绍了使用Spring MVC Test自带的支持进行单元测试的方法和配置：`Standalone`方式和`WebApplicationContext`方式，我们应该了解到：

- 进行Spring配置的时候按照功能分片段维护是很重要的，能方便配置重用
- `Standalone`方式和`WebApplicationContext`方式的区别

下一篇是介绍 [Unit Testing - Normal Controllers](/2016/04/09/spring-mvc-testing-unit-testing-normal-controllers/)

本文使用的代码已经放在了 [Github](https://github.com/pkainulainen/spring-mvc-test-examples/tree/master/controllers-unittest) 上，请自行查阅。
