---
title: "Spring MVC Integration Testing - Configuration"
date: 2016-04-09 23:07:53
updated: 2016-04-16 11:07:53
categories: ["Spring MVC Testing"]
tags: ["Spring MVC", "testing", "java"]

---

本文是 [Spring MVC Testing](/2016/04/09/spring-mvc-testing-content/) 集成测试系列的第1篇，原文链接：[Integration Testing of Spring MVC Applications: Configuration](http://www.petrikainulainen.net/programming/spring-framework/integration-testing-of-spring-mvc-applications-configuration/)。

没有人会否认集成测试的重要性，它是验证我们开发的组件能够正常协同工作的重要手段。不幸的是，对使用Spring MVC开发的web应用程序作集成测试有一点麻烦。

过去我们一直用 [Selenium](http://docs.seleniumhq.org) 和 [JWebUnit](https://jwebunit.github.io/jwebunit/) 来对web应用接口作集成测试，然后效果不是很好。这种方法有以下三个缺点：

- 对于开发中的web接口，编写和维护测试的工作量比较大
- 对于使用Javascript，尤其是Ajax的web应用，可用性不高
- 必须在web容器中启动运行，导致速度慢而且很没有效率

经常就是开发者在后续开发过程中觉得维护之前的集成测试用例太过耗时而且效果不大，所以废弃了这种形式的集成测试。幸运的是，我们找到了一种新型的集成测试框架Spring MVC Test可以用来简化测试工作。

本文主要介绍如何配置Spring MVC Test框架来进行web应用的测试。本系列使用的工具包括：

- Spring Framework 3.2
- JUnit 4.10
- Maven 3.0.3

我们一起来开始进入Spring MVC Test的世界吧！

<!-- More -->

# 通过Maven获取依赖

译者注：原文写作的时候是基于Spring Framework 3.1.2，当时Spring-test-mvc还是作为一个独立的项目进行开发和发布。在Spring Framework 3.2以后，该项目被合并到Spring Framework中去了。现在Spring Framework已经发布了4.X系列，很少有人在使用3.2以下的版本，为了减少混淆，直接将原文的pom文件加以修改。特此说明。

生成的pom.xml文件如下：

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-test</artifactId>
    <version>3.2</version>
    <scope>test</scope>
</dependency>
```

# MockMvc 配置

Spring MVC Test框架提供了`MockMvc`类体系来辅助编写基于Spring MVC开发的web应用的测试代码。我们需要做的就是使用`MockMvcBuilder`接口的实际实现来生成`MockMvc`实例。`MockMvcBuilders`工厂类提供了两个工厂方法创建`MockMvcBuilder`实例：

- `StandaloneMockMvcBuilder standaloneSetup(Object… controllers)` 主要用来对单个Controller进行测试，需要手动地配置各种Bean
- `DefaultMockMvcBuilder webAppContextSetup(WebApplicationContext context)` 主要使用配置好的Spring上下文来配置Bean

下面来详细看一下这两种方式分别如何使用。

## standaloneSetup

如果要测试的类是`HomeController`，我们可以用如下的方式创建一个`MockMvc`实例：

```java
MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HomeController()).build();
```

## webAppContextSetup

这种方式我们要先初始化一个配置好的`WebApplicationContext`实例，然后通过如下代码创建一个`MockMvc`实例：

```java
WebApplicationContext wac ;
MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
```

# 测试用例类的配置

在集成测试中我们应该使用webAppContextSetup方式的`MockMvc`配置方式，这样可以最大化地共用Spring配置代码。

我们可以按照如下的步骤来配置集成测试用例类：

1. 对测试用例类加上`@RunWith(SpringJUnit4ClassRunner.class)`注解
2. 对测试用例类加上`@ContextConfiguration`注解，并指定Spring配置文件（XML）或者配置类（Java）
3. 对测试用例类加上`@WebAppConfiguration`注解，表明这是一个web应用的测试用例
4. 测试用例类中添加一个`MockMvc`类型的成员
5. 测试用例类中添加一个`setUp()`方法并用`@Before`注解，调用`MockMvcBuilders`的静态方法`webAppContextSetup()`创建并配置好`MockMvc`类型的成员

完成后的测试用例类大概长成这个样子：

```java
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:testContext.xml", "classpath:webMvcContext.xml"})
//@ContextConfiguration(classes = {TestContext.class, WebMvcContext.class})
@WebAppConfiguration
public class TodoControllerTest_WebAppContext {
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Before
    public void setUp() throws Exception {
        Mockito.reset(mockedTodoService);

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    // Add tests here
}
```

本文使用的代码被放在了 [Github](https://github.com/pkainulainen/spring-mvc-test-examples/tree/master/configuration) 上。使用Maven进行集成测试的方法可以参见我的另外一篇文章 [Integration Testing with Maven](http://www.petrikainulainen.net/programming/maven/integration-testing-with-maven/)。

# 总结

本文主要介绍了如何来配置基于Spring MVC Test的集成测试，主要内容有：

- 如何使用不同的Spring配置方式（XML/Java）来配置测试用例
- 应该使用`webAppContextSetup`的方式而不是`standaloneSetup`方式
- 如何使用`WebApplicationContext`注入来完成Spring配置的加载

下一篇是 [Spring MVC Integration Testing - Controllers](/2016/04/09/spring-mvc-testing-integration-testing-controllers/)。
