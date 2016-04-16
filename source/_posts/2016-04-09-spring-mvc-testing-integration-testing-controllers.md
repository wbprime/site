title: 'Spring MVC Integration Testing - Controllers'
date: 2016-04-09 23:08:01
updated: 2016-04-09 23:08:01
categories: "Spring MVC Testing"
tags: ["Spring MVC", "Test", "Java"]

---

本文是[Spring MVC Testing](/2016/04/09/spring-mvc-testing-content/) 集成测试系列的第2篇，原文链接：[Integration Testing of Spring MVC Applications: Controllers](http://www.petrikainulainen.net/programming/spring-framework/integration-testing-of-spring-mvc-applications-controllers/)。

本文主要介绍如何为“标准”Controller编写集成测试。在这里“标准”的含义延续前一个序列 [Spring MVC Testing](/2016/04/09/spring-mvc-testing-content/) 中的含义，表示不使用Ajax的请求或者处理Form结果的请求。

同样地，本文还是一步一步地为我们的TodoApplication编写集成测试。该程序提供Todo项的增删改查（CRUD）接口，本文主要关注其中的3个接口：获取Todo项列表；查看单个Todo项的详情；以及删除某个Todo项。

<!-- More -->

# 通过Maven获取依赖

本文用到的依赖如下：

- Hamcrest 1.3
- JUnit 4.10
- Spring Test 3.2.3.RELEASE
- Spring Test DBUnit 1.0.0
- DBUnit 2.4.8

生成的pom.xml文件的片段如下：

```xml
<dependency>
    <groupId>org.hamcrest</groupId>
    <artifactId>hamcrest-all</artifactId>
    <version>1.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.10</version>
    <scope>test</scope>
    <exclusions>
        <exclusion>
            <artifactId>hamcrest-core</artifactId>
            <groupId>org.hamcrest</groupId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-test</artifactId>
    <version>3.1.2.RELEASE</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-test</artifactId>
    <version>3.2.3.RELEASE</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.github.springtestdbunit</groupId>
    <artifactId>spring-test-dbunit</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.dbunit</groupId>
    <artifactId>dbunit</artifactId>
    <version>2.4.8</version>
    <scope>test</scope>            
</dependency>
```

# Spring Test DBUnit 快速入门

我们在集成测试中使用了 [Spring Test DBUnit](http://springtestdbunit.github.com/spring-test-dbunit/) 库和 [DBUnit](http://www.dbunit.org/) 库。我们快速地熟悉一下如何使用它们。

## 配置

### 创建Spring上下文配置

首先需要创建Spring上下文配置，让Spring来管理依赖。

1. 创建一个Java类，并用`@Configuration`注解
2. 新建`application.properties`属性文件，并使用`@PropertySource`注解导入类配置上下文中
3. 添加一个`Environment`类型的成员用于获取属性文件中的配置信息
4. 使用`@Bean`注解创建一个`DataSource`类型的Bean

最终的结果如下：

```java
import com.jolbox.bonecp.BoneCPDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
 
import javax.annotation.Resource;
import javax.sql.DataSource;
 
@Configuration
@PropertySource("classpath:application.properties")
public class ExampleApplicationContext {
 
    @Resource
    private Environment environment;
 
    @Bean
    public DataSource dataSource() {
        BoneCPDataSource dataSource = new BoneCPDataSource();
 
        dataSource.setDriverClass(environment.getRequiredProperty("db.driver"));
        dataSource.setJdbcUrl(environment.getRequiredProperty("db.url"));
        dataSource.setUsername(environment.getRequiredProperty("db.username"));
        dataSource.setPassword(environment.getRequiredProperty("db.password"));
 
        return dataSource;
    }
}
```

### 配置测试用例类

我们可以通过如下步骤在测试用例类中使用DBUnit：

1. 对测试用例类使用`@RunWith(SpringJUnit4ClassRunner.class)`注解
2. 对测试用例类使用`@ContextConfiguratiON`注解并引入上一步创建的`ExampleApplicationContext`类作为Spring配置上下文
3. 对测试用例类使用`@TestExecutionListeners`注解并使用`DbUnitTestExecutionListener`来处理DBUnit的相关注解

最终代码如下：

```java
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
 
import javax.annotation.Resource;
 
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ExampleApplicationContext.class})
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DbUnitTestExecutionListener.class })
public class Test {
    //Add test methods here
}
```

## 使用

使用DBUnit，我们可以用注解来初始化数据库、初始化数据表以及在测试结束后验证数据库的状态。详细使用请参见 [Spring Test DBUnit](http://springtestdbunit.github.com/spring-test-dbunit/)。本文将会使用如下两个注解：

- @DatabaseSetup 用于在测试开始之前将数据库初始化到指定状态
- @ExpectedDatabase 用于在测试结束之后验证数据库状态

# Spring MVC Test 快速入门

## 创建并执行请求

`MockMvc`类的`perform(RequestBuilder requestBuilder)`方法可以用来模拟执行HTTP请求。`MockMvcRequestBuilders`类提供了几个静态方法用来模拟HTTP请求实体，具体是：

- `get()` 用于创建一个模拟的HTTP GET请求实体
- `delete()` 用于创建一个模拟的HTTP DELETE请求实体
- `fileUpload()` 用于创建一个模拟的HTTP文件上传请求实体（multipart request）
- `post()` 用于创建一个模拟的HTTP POST请求实体
- `put()` 用于创建一个模拟的HTTP PUT请求实体

详情可以查看`MockHttpServletRequestBuilder`类的说明文档。

## 验证请求返回结果

`ResultActions`类提供了3个方法来提供对模拟的HTTP请求返回结果的处理：

- `void andExpect(ResultMatcher matcher)` 用于对返回结果作断言验证
- `void andDo(ResultHandler handler)` 用于对返回结果进行二次操作
- `MvcResult andReturn()` 用于直接返回结果

为了提高代码效率，`MockMvcResultMatchers`类和`MockMvcResultHandlers`类提供了许多静态方法：

- `MockMvcResultMatchers`类提供了很多包装好的`Matcher`实例
- `MockMvcResultHandlers`类目前只提供了`print()`方法返回一个`Handler`实例可以输出返回结果到控制台

# 示例web应用结构

## Domain 层

Domain层提供了`Todo`实体类，表示一个一个的Todo项。

`Todo`类代码如下：

```java
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import javax.persistence.*;
 
@Entity
@Table(name="todos")
public class Todo {
 
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
 
    @Column(name = "creation_time", nullable = false)
    @Type(type="org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    private DateTime creationTime;
 
    @Column(name = "description", nullable = true, length = 500)
    private String description;
 
    @Column(name = "modification_time", nullable = false)
    @Type(type="org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    private DateTime modificationTime;
 
    @Column(name = "title", nullable = false, length = 100)
    private String title;
 
    @Version
    private long version;
 
    public Todo() {
 
    }
 
    //Getters and other methods
}
```

## Service 层

Service层提供了`TodoService`接口，用来连接Controller层和Domain层的通信。该接口提供了3个方法：

- `Todo deleteById(Long id)` 删除指定id的Todo项；如果不存在该Todo项，则抛出`TodoNotFoundException`异常
- `List<Todo> findAll()` 返回所有Todo项的列表；如果没有Todo项，则返回空列表
- `Todo findById(Long id)` 返回指定id的Todo项；如果不存在该Todo项，则抛出`TodoNotFoundException`异常

`TodoService`接口代码如下：

```java
public interface TodoService {
 
    public Todo deleteById(Long id) throws TodoNotFoundException;
 
    public List<Todo> findAll();
 
    public Todo findById(Long id) throws TodoNotFoundException;
}
```

## Controller 层

Controller层提供了`TodoController`类，用于创建视图、处理请求。

`TodoController`类代码如下：

```java
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
 
import javax.annotation.Resource;
import javax.validation.Valid;
 
@Controller
public class TodoController {
 
    @Resource
    private TodoService service;
 
    @Resource
    private MessageSource messageSource;
 
    @RequestMapping(value = "/todo/delete/{id}", method = RequestMethod.GET)
    public String deleteById(@PathVariable("id") Long id, RedirectAttributes attributes) throws TodoNotFoundException {
        Todo deleted = service.deleteById(id);
 
        addFeedbackMessage(attributes, "feedback.message.todo.deleted", deleted.getTitle());
 
        return createRedirectViewPath("/");
    }
 
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String findAll(Model model) {
        List<Todo> models = service.findAll();
 
        model.addAttribute("todos", models);
 
        return "todo/list";
    }
 
    @RequestMapping(value = "/todo/{id}", method = RequestMethod.GET)
    public String findById(@PathVariable("id") Long id, Model model) throws TodoNotFoundException {
        Todo found = service.findById(id);
 
        model.addAttribute("todo", found);
 
        return "todo/view";
    }
 
    private void addFeedbackMessage(RedirectAttributes attributes, String messageCode, Object... messageParameters) {
        String localizedFeedbackMessage = getMessage(messageCode, messageParameters);
        attributes.addFlashAttribute("feedbackMessage", localizedFeedbackMessage);
    }
 
    private String getMessage(String messageCode, Object... messageParameters) {
        Locale current = LocaleContextHolder.getLocale();
        return messageSource.getMessage(messageCode, messageParameters, current);
    }
 
    private String createRedirectViewPath(String requestMapping) {
        StringBuilder redirectViewPath = new StringBuilder();
        redirectViewPath.append("redirect:");
        redirectViewPath.append(requestMapping);
        return redirectViewPath.toString();
    }
}
```

# 测试用例

## 创建测试用例类框架

要搭建起测试框架，需要以下步骤：

1. 搭建Spring MVC Test环境
2. 搭建DbUnit环境
3. 配置DBUnit，设定测试开始和结束时的状态

没有添加任何测试方法前的框架代码如下：

```java
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.server.MockMvc;
import org.springframework.test.web.server.samples.context.WebContextLoader;
import org.springframework.test.web.server.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
 
import javax.annotation.Resource;
 
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = WebContextLoader.class, classes = {ExampleApplicationContext.class})
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DbUnitTestExecutionListener.class })
@DatabaseSetup("toDoData.xml")
public class ITTodoControllerTest {
 
    @Resource
    private WebApplicationContext webApplicationContext;
 
    private MockMvc mockMvc;
 
    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.webApplicationContextSetup(webApplicationContext)
                .build();
    }
     
    //Add tests here
}
```

注意代码中的`@DatabaseSetup("toDoData.xml")`注解，该注解的作用在于使用`todoData.xml`文件中的数据去初始化数据库表，该注解由`DbUnitTestExecutionListener`进行解析。

`todoData.xml`文件内容如下：

```xml
<dataset>
    <todos id="1" creation_time="2012-10-21 11:13:28" description="Lorem ipsum" modification_time="2012-10-21 11:13:28" title="Foo" version="0"/>
    <todos id="2" creation_time="2012-10-21 11:13:28" description="Lorem ipsum" modification_time="2012-10-21 11:13:28" title="Bar" version="0"/>
</dataset>
```

## 编写集成测试用例

### 获取Todo项列表接口的测试用例

编写该测试用例的思路如下：

1. 使用`@ExpectedDatabase`注解来验证接口没有对数据库表状态产生变化
2. 模拟执行"/"的GET请求，并取得返回的响应结果
3. 对返回的响应结果作断言：HTTP状态码为200
4. 对返回的响应结果作断言：view的名字是'todo/list'
5. 对返回的响应结果作断言：view的路径为"/WEB-INF/jsp/todo/list.jsp"
6. 对返回的响应结果作断言：model中Todo项的个数是2
7. 对返回的响应结果作断言：model中的Todo项符合预期

最终的代码如下：

```java
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.server.MockMvc;
import org.springframework.test.web.server.samples.context.WebContextLoader;
 
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.server.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.*;
 
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = WebContextLoader.class, classes = {ExampleApplicationContext.class})
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DbUnitTestExecutionListener.class })
@DatabaseSetup("toDoData.xml")
public class ITTodoControllerTest {
 
    //Add web application context here
 
    private MockMvc mockMvc;
 
    //Add setUp() method here
 
    @Test
    @ExpectedDatabase("toDoData.xml")
    public void findAll() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("todo/list"))
                .andExpect(forwardedUrl("/WEB-INF/jsp/todo/list.jsp"))
                .andExpect(model().attribute("todos", hasSize(2)))
                .andExpect(model().attribute("todos", hasItem(
                        allOf(
                                hasProperty("id", is(1L)),
                                hasProperty("description", is("Lorem ipsum")),
                                hasProperty("title", is("Foo"))
                        )
                )))
                .andExpect(model().attribute("todos", hasItem(
                        allOf(
                                hasProperty("id", is(2L)),
                                hasProperty("description", is("Lorem ipsum")),
                                hasProperty("title", is("Bar"))
                        )
                )));
    }
}
```

### 获取单个Todo项详情接口的测试用例

根据参数的不同，获取单个Todo项详情接口会有两种不同的返回结果：

- 如果指定id的Todo项存在，返回单个Todo项的详情页
- 如果指定id的Todo项不存在，返回404页

对于指定Todo项存在的情况，我们可以按照以下步骤来编写测试用例：

1. 使用`@ExpectedDatabase`注解来验证接口没有对数据库表状态产生变化
2. 模拟执行"/todo/1"的GET请求，并取得返回的响应结果
3. 对返回的响应结果作断言：HTTP状态码为200
4. 对返回的响应结果作断言：view的名字是'todo/view'
5. 对返回的响应结果作断言：view的路径为"/WEB-INF/jsp/todo/view.jsp"
6. 对返回的响应结果作断言：model中的Todo项符合预期

最终的代码如下：

```java
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.server.MockMvc;
import org.springframework.test.web.server.samples.context.WebContextLoader;
 
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.server.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.*;
 
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = WebContextLoader.class, classes = {ExampleApplicationContext.class})
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DbUnitTestExecutionListener.class })
@DatabaseSetup("toDoData.xml")
public class ITTodoControllerTest {
 
    //Add web application context here
 
    private MockMvc mockMvc;
 
    //Add setUp() method here
 
    @Test
    @ExpectedDatabase("toDoData.xml")
    public void findById() throws Exception {
        mockMvc.perform(get("/todo/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(view().name("todo/view"))
                .andExpect(forwardedUrl("/WEB-INF/jsp/todo/view.jsp"))
                .andExpect(model().attribute("todo", hasProperty("id", is(1L))))
                .andExpect(model().attribute("todo", hasProperty("description", is("Lorem ipsum"))))
                .andExpect(model().attribute("todo", hasProperty("title", is("Foo"))));
    }
}
```

对于指定Todo项不存在的情况，我们可以按照以下步骤来编写测试用例：

1. 使用`@ExpectedDatabase`注解来验证接口没有对数据库表状态产生变化
2. 模拟执行"/todo/3"的GET请求，并取得返回的响应结果
3. 对返回的响应结果作断言：HTTP状态码为404
4. 对返回的响应结果作断言：view的名字是'error/404'
5. 对返回的响应结果作断言：view的路径为"/WEB-INF/jsp/error/404.jsp"

最终的代码如下：

```java
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.server.MockMvc;
import org.springframework.test.web.server.samples.context.WebContextLoader;
 
import static org.springframework.test.web.server.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.*;
 
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = WebContextLoader.class, classes = {ExampleApplicationContext.class})
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DbUnitTestExecutionListener.class })
@DatabaseSetup("toDoData.xml")
public class ITTodoControllerTest {
 
    //Add web application context here
 
    private MockMvc mockMvc;
 
    //Add setUp() method here
 
    @Test
    @ExpectedDatabase("toDoData.xml")
    public void findByIdWhenTodoIsNotFound() throws Exception {
        mockMvc.perform(get("/todo/{id}", 3L))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(forwardedUrl("/WEB-INF/jsp/error/404.jsp"));
    }
}
```

### 删除指定Todo项接口的测试用例

根据参数的不同，删除指定Todo项接口会有两种不同的返回结果：

- 如果指定id的Todo项存在，数据库中的Todo会被删除
- 如果指定id的Todo项不存在，返回404页

对于指定Todo项存在的情况，我们可以按照以下步骤来编写测试用例：

1. 使用`@ExpectedDatabase`注解来验证接口对数据库的操作符合预期
2. 模拟执行"/todo/delete/1"的GET请求，并取得返回的响应结果
3. 对返回的响应结果作断言：HTTP状态码为200
4. 对返回的响应结果作断言：view的名字是'redirect:/'
5. 对返回的响应结果作断言：view的路径为"/WEB-INF/jsp/todo/view.jsp"
6. 对返回的响应结果作断言：flash中返回了预期的提示信息

最终的代码如下：

```java
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.server.MockMvc;
import org.springframework.test.web.server.samples.context.WebContextLoader;
 
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.server.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.*;
 
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = WebContextLoader.class, classes = {ExampleApplicationContext.class})
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DbUnitTestExecutionListener.class })
@DatabaseSetup("toDoData.xml")
public class ITTodoControllerTest {
 
    //Add web application context here
 
    private MockMvc mockMvc;
 
    //Add setUp() method here
 
    @Test
    @ExpectedDatabase("todoData-delete-expected.xml")
    public void deleteById() throws Exception {
        mockMvc.perform(get("/todo/delete/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(view().name("redirect:/"))
                .andExpect(flash().attribute("feedbackMessage", is("Todo entry: Foo was deleted.")));
    }
}
```

作为对照，`todoData-delete-expected.xml`文件包含了预期的数据库表结果，内容如下：

```xml
<dataset>
    <todos id="2" creation_time="2012-10-21 11:13:28" description="Lorem ipsum" modification_time="2012-10-21 11:13:28" title="Bar" version="0"/>
</dataset>
```

对于指定Todo项不存在的情况，我们可以按照以下步骤来编写测试用例：

1. 使用`@ExpectedDatabase`注解来验证接口没有对数据库表状态产生变化
2. 模拟执行"/todo/delete/3"的GET请求，并取得返回的响应结果
3. 对返回的响应结果作断言：HTTP状态码为404
4. 对返回的响应结果作断言：view的名字是'error/404'
5. 对返回的响应结果作断言：view的路径为"/WEB-INF/jsp/error/404.jsp"

最终的代码如下：

```java
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.server.MockMvc;
import org.springframework.test.web.server.samples.context.WebContextLoader;
 
import static org.springframework.test.web.server.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.*;
 
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = WebContextLoader.class, classes = {ExampleApplicationContext.class})
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DbUnitTestExecutionListener.class })
@DatabaseSetup("toDoData.xml")
public class ITTodoControllerTest {
 
    //Add web application context here
 
    private MockMvc mockMvc;
 
    //Add setUp() method here
 
    @Test
    @ExpectedDatabase("toDoData.xml")
    public void deleteByIdWhenTodoIsNotFound() throws Exception {
        mockMvc.perform(get("/todo/delete/{id}", 3L))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(forwardedUrl("/WEB-INF/jsp/error/404.jsp"));
    }
}
```

# 总结

到此为止，我们就写完了所有的测试用例代码。总结一下，本文主要内容如下：

- 如何生成并执行模拟的HTTP请求，以及如何对响应结果作断言
- 使用Spring MVC Test编写的集成测试用例可读性非常好，可以作为接口文档的补充
- Spring MVC Test没法验证view是否正确绘制，但可以验证是否使用了预期的view模板

下一篇是 [Spring MVC Integration Testing - Forms](/2016/04/09/spring-mvc-testing-integration-testing-forms/)。
