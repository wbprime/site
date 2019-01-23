---
title: "Spring MVC Unit Testing - Normal Controllers"
date: 2016-04-09 23:07:12
updated: 2016-04-09 23:07:12
categories: ["Spring MVC Testing"]
tags: ["Spring MVC", "testing", "java"]

---

本文是 [Spring MVC Testing](/2016/04/09/spring-mvc-testing-content/) 单元测试系列的第2篇，原文链接：[Unit Testing of Spring MVC Controllers: "Normal" Controllers](http://www.petrikainulainen.net/programming/spring-framework/unit-testing-of-spring-mvc-controllers-normal-controllers/)。

本系列的第1部分讲述了使用Spring MVC Test应如何进行单元测试的[配置](/2016/04/09/spring-mvc-testing-unit-testing-configuration/)，现在可以开始实战一下如何对标准controller编写单元测试。

首先需要明确一下。

> 何为标准controller？

注意：原文标准是加了双引号的（"normal"）

我们称之为标准controller的Controller，是渲染view或者处理form提交请求的Controller。（与之相对的是Rest Controller）。

OK，现在我们进入正文。

<!-- More -->

# 通过Maven获取依赖

本系列用到的依赖如下：

- Jackson 2.2.1 (core and databind modules)
- Hamcrest 1.3
- JUnit 4.11
- Mockito 1.9.5
- Spring Test 3.2.3.RELEASE

生成的pom.xml文件的片段如下：

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-core</artifactId>
    <version>2.2.1</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.2.1</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.hamcrest</groupId>
    <artifactId>hamcrest-all</artifactId>
    <version>1.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.11</version>
    <scope>test</scope>
    <exclusions>
        <exclusion>
            <artifactId>hamcrest-core</artifactId>
            <groupId>org.hamcrest</groupId>
        </exclusion>
    </exclusions>
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

然后可以开始编写测试用例了。

# 测试用例类

对controller方法进行单元测试，原则上有以下两个步骤：

1. 首先向目标controller发送一个请求
2. 然后检验收到的响应是否符合预期

Spring MVC Test模块提供了一些工具简化我们的工作，这些类主要是：

- [`MockMvcRequestBuilders`](http://docs.spring.io/spring/docs/3.2.x/javadoc-api/org/springframework/test/web/servlet/request/MockMvcRequestBuilders.html) 类可以用来简化创建请求的工作
- [`MockMvc`](http://docs.spring.io/spring/docs/3.2.x/javadoc-api/org/springframework/test/web/servlet/MockMvc.html) 类可以用来执行请求并获取响应
- [`MockMvcResultMatchers`](http://docs.spring.io/spring/docs/3.2.x/javadoc-api/org/springframework/test/web/servlet/result/MockMvcResultMatchers.html) 类可以用来辅助对响应作校验

为了演示完整的流程，我们将编写单元测试测试3个controller方法：

1. 第一个主要是渲染显示`Todo`项列表页面的接口
2. 第二个主要是渲染显示单个`Todo`项详情的接口
3. 第三个主要是处理添加`Todo`项的表单请求的接口

## `Todo`项列表页接口

首先看一下该接口的实现代码。

### 预期的实现

预期的接口应该做以下几件事：

1. 接收到"/"上的GET请求，开始处理流程
2. 调用`TodoService`的`findAll()`方法获取到所有的`Todo`对象的列表
3. 将获取到的列表加入到model中
4. 返回对应的view名称

`TodoController`类内的相关代码如下：

```java
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;
 
@Controller
public class TodoController {
    private final TodoService service;
     
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String findAll(Model model) {
        List<Todo> models = service.findAll();
        model.addAttribute("todos", models);
        return "todo/list";
    }
}
```

接下来可以开始编写测试用例了。

### 测试用例：`Todo`列表页接口

该测试用例主要工作如下：

1. 准备测试数据
2. 配置mock的`TodoService`实例在`findAll()`方法被调用的时候返回准备的数据
3. 执行一个'/'的GET请求
4. 对响应作断言：HTTP返回码是200
5. 对响应作断言：view的名称是"todo/list"
6. 对响应作断言：请求拿到的是'/WEB-INF/jsp/todo/list.jsp'页面
7. 对响应作断言：model里面的元素个数是2
8. 对响应作断言：model里面的元素是正确的
9. 检查请求执行过程中mock的`TodoService`实例执行了`findAll()`方法有且仅1次
10. 检查请求执行过程中mock的`TodoService`实例未执行其他方法

相关代码如下：

```java 
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
 
import java.util.Arrays;
 
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
 
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestContext.class, WebAppContext.class})
@WebAppConfiguration
public class TodoControllerTest {
 
    private MockMvc mockMvc;
 
    @Autowired
    private TodoService todoServiceMock;
 
    //Add WebApplicationContext field here
 
    //The setUp() method is omitted.
 
    @Test
    public void 
    findAll_ShouldAddTodoEntriesToModelAndRenderTodoListView() throws Exception {
        Todo first = new TodoBuilder()
                .id(1L)
                .description("Lorem ipsum")
                .title("Foo")
                .build();
 
        Todo second = new TodoBuilder()
                .id(2L)
                .description("Lorem ipsum")
                .title("Bar")
                .build();
 
        when(todoServiceMock.findAll()).thenReturn(Arrays.asList(first, second));
 
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
 
        verify(todoServiceMock, times(1)).findAll();
        verifyNoMoreInteractions(todoServiceMock);
    }
}
```

## `Todo`项详情页接口

首先看一下该接口的实现代码。

### 预期的实现

预期的接口应该做以下几件事：

1. 接收到"/todo/{id}"上的GET请求，{id}是`Todo`的id值，开始处理流程
2. 调用`TodoService`的`findById()`方法获取到目标`Todo`对象
3. 将获取到的`Todo`项加入到model中
4. 返回对应的view名称

`TodoController`类内的相关代码如下：

```java
@RequestMapping(value = "/todo/{id}", method = RequestMethod.GET)
public String findById(@PathVariable("id") Long id, Model model) throws TodoNotFoundException {
    Todo found = service.findById(id);
    model.addAttribute("todo", found);
    return "todo/view";
}
```

> 如果抛出了`TodoNotFoundException`，Spring Mvc是怎么处理的呢？

在本系列的前一篇中，我们在webapp的配置中注册了一个`exceptionResolver()`：

```java
@Bean
public SimpleMappingExceptionResolver exceptionResolver() {
    SimpleMappingExceptionResolver exceptionResolver = new SimpleMappingExceptionResolver();
 
    Properties exceptionMappings = new Properties();
 
    exceptionMappings.put(
        "net.petrikainulainen.spring.testmvc.todo.exception.TodoNotFoundException",
        "error/404"
    );
    exceptionMappings.put("java.lang.Exception", "error/error");
    exceptionMappings.put("java.lang.RuntimeException", "error/error");
 
    exceptionResolver.setExceptionMappings(exceptionMappings);
 
    Properties statusCodes = new Properties();
 
    statusCodes.put("error/404", "404");
    statusCodes.put("error/error", "500");
 
    exceptionResolver.setStatusCodes(statusCodes);
 
    return exceptionResolver;
}
```

所以，当抛出`TodoNotFoundException`异常时，会返回'error/404'的页面。

所以我们的测试用例要测试两种情况：

- 接口找到了指定的`Todo`项
- 接口没有找到指定的`Todo`项

接下来可以开始编写测试用例了。

### 测试用例：`Todo`项未找到

该测试用例主要工作如下：

1. 配置mock的`TodoService`实例在`findById()`方法被调用的时候抛出`TodoNotFoundException`
2. 执行一个'/todo/1'的GET请求
3. 对响应作断言：HTTP返回码是404
4. 对响应作断言：view的名称是"error/404"
5. 对响应作断言：请求拿到的是'/WEB-INF/jsp/error/404.jsp'页面
6. 检查请求执行过程中mock的`TodoService`实例执行了`findById()`方法有且仅1次
7. 检查请求执行过程中mock的`TodoService`实例未执行其他方法

代码如下：

```java
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
 
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
 
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestContext.class, WebAppContext.class})
@WebAppConfiguration
public class TodoControllerTest {
 
    private MockMvc mockMvc;
 
    @Autowired
    private TodoService todoServiceMock;
 
    //Add WebApplicationContext field here
 
    //The setUp() method is omitted.
 
    @Test
    public void findById_TodoEntryNotFound_ShouldRender404View() throws Exception {
        when(todoServiceMock.findById(1L)).thenThrow(new TodoNotFoundException(""));
 
        mockMvc.perform(get("/todo/{id}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(forwardedUrl("/WEB-INF/jsp/error/404.jsp"));
 
        verify(todoServiceMock, times(1)).findById(1L);
        verifyZeroInteractions(todoServiceMock);
    }
}
```

### 测试用例：`Todo`项被找到

该测试用例主要工作如下：

1. 准备测试数据
2. 配置mock的`TodoService`实例在`findById()`方法被调用的时候返回准备的数据
3. 执行一个'/todo/1'的GET请求
4. 对响应作断言：HTTP返回码是200
5. 对响应作断言：view的名称是"todo/view"
6. 对响应作断言：请求拿到的是'/WEB-INF/jsp/todo/view.jsp'页面
7. 对响应作断言：model里面的元素是正确的
8. 检查请求执行过程中mock的`TodoService`实例执行了`findById()`方法有且仅1次
9. 检查请求执行过程中mock的`TodoService`实例未执行其他方法

代码如下：

```java
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
 
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
 
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestContext.class, WebAppContext.class})
@WebAppConfiguration
public class TodoControllerTest {
 
    private MockMvc mockMvc;
 
    @Autowired
    private TodoService todoServiceMock;
 
    //Add WebApplicationContext field here
 
    //The setUp() method is omitted.
 
    @Test
    public void 
    findById_TodoEntryFound_ShouldAddTodoEntryToModelAndRenderViewTodoEntryView() throws Exception {
        Todo found = new TodoBuilder()
                .id(1L)
                .description("Lorem ipsum")
                .title("Foo")
                .build();
 
        when(todoServiceMock.findById(1L)).thenReturn(found);
 
        mockMvc.perform(get("/todo/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(view().name("todo/view"))
                .andExpect(forwardedUrl("/WEB-INF/jsp/todo/view.jsp"))
                .andExpect(model().attribute("todo", hasProperty("id", is(1L))))
                .andExpect(model().attribute("todo", hasProperty("description", is("Lorem ipsum"))))
                .andExpect(model().attribute("todo", hasProperty("title", is("Foo"))));
 
        verify(todoServiceMock, times(1)).findById(1L);
        verifyNoMoreInteractions(todoServiceMock);
    }
}
```

## `Todo`项创建表单请求接口

首先看一下该接口的实现代码。

### 预期的实现

预期的接口应该做以下几件事：

1. 接收到"/todo/add"上的POST请求，开始处理流程
2. 检测表单是否有错误
3. 调用`TodoService`的`add()`方法添加指定的`Todo`项
3. 将需要的信息加入到model中
4. 返回重定向的view名称

`TodoController`类内的相关代码如下：

```java
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
 
import javax.validation.Valid;
import java.util.Locale;
 
@Controller
@SessionAttributes("todo")
public class TodoController {
 
    private final TodoService service;
 
    private final MessageSource messageSource;
 
    @RequestMapping(value = "/todo/add", method = RequestMethod.POST)
    public String add(
        @Valid @ModelAttribute("todo") TodoDTO dto,
        BindingResult result,
        RedirectAttributes attributes
    ) {
        if (result.hasErrors()) {
            return "todo/add";
        }
 
        Todo added = service.add(dto);
 
        addFeedbackMessage(attributes, "feedback.message.todo.added", added.getTitle());
        attributes.addAttribute("id", added.getId());
 
        return createRedirectViewPath("todo/view");
    }
 
    private void addFeedbackMessage(
        RedirectAttributes attributes,
        String messageCode,
        Object... messageParameters
    ) {
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

我们使用了`TodoDTO`类来封装`Todo`项的创建信息，代码如下：

```java
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;
 
public class TodoDTO {
 
    private Long id;
 
    @Length(max = 500)
    private String description;
 
    @NotEmpty
    @Length(max = 100)
    private String title;
 
    //Constructor and other methods are omitted.
}
```

`TodoDTO`类里面有一些校验规则，如果不满足规则，Spring在接口的`BindingResult`参数里面会显示错误。

所以，我们的测试用例需要考虑两种情况：

- 参数校验通过
- 参数校验没有通过

接下来可以开始编写测试用例了。

### 测试用例：`TodoDTO`参数校验未通过

该测试用例主要工作如下：

1. 创建一个不符合验证规则的title
2. 创建一个不符合验证规则的description
3. 执行一个'/todo/add'的POST请求
4. 对响应作断言：HTTP返回码是200
5. 对响应作断言：view的名称是"todo/add"
6. 对响应作断言：请求拿到的是'/WEB-INF/jsp/todo/add.jsp'页面

代码如下：

```java
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
 
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
 
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestContext.class, WebAppContext.class})
@WebAppConfiguration
public class TodoControllerTest {
 
    private MockMvc mockMvc;
 
    @Autowired
    private TodoService todoServiceMock;
 
    //Add WebApplicationContext field here
 
    //The setUp() method is omitted.
 
    @Test
    public void 
    add_DescriptionAndTitleAreTooLong_ShouldRenderFormViewAndReturnValidationErrorsForTitleAndDescription()
    throws Exception {
        String title = TestUtil.createStringWithLength(101);
        String description = TestUtil.createStringWithLength(501);
 
        mockMvc.perform(post("/todo/add")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("description", description)
                .param("title", title)
                .sessionAttr("todo", new TodoDTO())
        )
                .andExpect(status().isOk())
                .andExpect(view().name("todo/add"))
                .andExpect(forwardedUrl("/WEB-INF/jsp/todo/add.jsp"))
                .andExpect(model().attributeHasFieldErrors("todo", "title"))
                .andExpect(model().attributeHasFieldErrors("todo", "description"))
                .andExpect(model().attribute("todo", hasProperty("id", nullValue())))
                .andExpect(model().attribute("todo", hasProperty("description", is(description))))
                .andExpect(model().attribute("todo", hasProperty("title", is(title))));
 
        verifyZeroInteractions(todoServiceMock);
    }
}
```

为了简化代码，我们新建了一个新的类`TestUtil`，用来生成固定长度的字符串。`TestUtil`类代码如下：

```java
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
 
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
 
public class TestUtil {
 
    public static String createStringWithLength(int length) {
        StringBuilder builder = new StringBuilder();
 
        for (int index = 0; index < length; index++) {
            builder.append("a");
        }
 
        return builder.toString();
    }
}
```

### 测试用例：`TodoDTO`参数校验通过

该测试用例主要工作如下：

1. 准备测试数据
2. 配置mock的`TodoService`实例在`add()`方法被调用的时候返回一个`Todo`项
3. 执行一个'/todo/add'的POST请求
4. 对响应作断言：HTTP返回码是302
5. 对响应作断言：view的名称是"redirect:todo/{id}"
6. 对响应作断言：请求被重定向到"todo/1"
7. 检查请求执行过程中mock的`TodoService`实例执行了`add()`方法有且仅1次
8. 检查请求执行过程中mock的`TodoService`实例未执行其他方法

代码如下：

```java
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
 
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
 
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestContext.class, WebAppContext.class})
@WebAppConfiguration
public class TodoControllerTest {
 
    private MockMvc mockMvc;
 
    @Autowired
    private TodoService todoServiceMock;
 
    //Add WebApplicationContext field here
 
    //The setUp() method is omitted.
 
    @Test
    public void add_NewTodoEntry_ShouldAddTodoEntryAndRenderViewTodoEntryView() throws Exception {
        Todo added = new TodoBuilder()
                .id(1L)
                .description("description")
                .title("title")
                .build();
 
        when(todoServiceMock.add(isA(TodoDTO.class))).thenReturn(added);
 
        mockMvc.perform(post("/todo/add")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("description", "description")
                .param("title", "title")
                .sessionAttr("todo", new TodoDTO())
        )
                .andExpect(status().isMovedTemporarily())
                .andExpect(view().name("redirect:todo/{id}"))
                .andExpect(redirectedUrl("/todo/1"))
                .andExpect(model().attribute("id", is("1")))
                .andExpect(flash().attribute("feedbackMessage", is("Todo entry: title was added.")));
 
        ArgumentCaptor<TodoDTO> formObjectArgument = ArgumentCaptor.forClass(TodoDTO.class);
        verify(todoServiceMock, times(1)).add(formObjectArgument.capture());
        verifyNoMoreInteractions(todoServiceMock);
 
        TodoDTO formObject = formObjectArgument.getValue();
 
        assertThat(formObject.getDescription(), is("description"));
        assertNull(formObject.getId());
        assertThat(formObject.getTitle(), is("title"));
    }
}
```

# 总结

本文主要介绍了如何使用Spring MVC Test来对标准controller进行单元测试，主要内容如下：

- 如何创建一个请求
- 如何对请求的响应作断言
- 如何单元测试一个渲染view的接口
- 如何单元测试一个处理表单请求的接口

下一篇是介绍[Spring MVC Unit Testing - REST API](/2016/04/09/spring-mvc-testing-unit-testing-rest-api/)。

本文使用的代码已经放在了 [Github](https://github.com/pkainulainen/spring-mvc-test-examples/tree/master/controllers-unittest) 上，请自行查阅。
