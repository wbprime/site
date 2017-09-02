---
title: 'Spring MVC Integration Testing - Forms'
date: 2016-04-09 23:08:24
updated: 2016-04-09 23:08:24
categories: "Spring MVC Testing"
tags: ["Spring MVC", "testing", "java"]

---

本文是 [Spring MVC Testing](/2016/04/09/spring-mvc-testing-content/) 集成测试系列的第3篇，原文链接：[Integration Testing of Spring MVC Applications: Forms](http://www.petrikainulainen.net/programming/spring-framework/integration-testing-of-spring-mvc-applications-forms/)。

本文主要介绍为处理Form表单请求的接口编写集成测试用例。

本文紧接着上一篇 [Spring MVC Integration Testing - Controllers](/2016/04/09/spring-mvc-testing-integration-testing-controllers/) 的内容，主要涉及到两个接口：创建新的Todo项和更新指定的Todo项。

<!-- More -->

# 通过Maven获取依赖

除了上一篇中介绍的依赖之外，本文添加了新的依赖：

- jackson-core-asl 1.9.9 
- jackson-mapper-asl 1.9.9

对应的pom.xml文件片段如下：

```xml
<dependency>
    <groupId>org.codehaus.jackson</groupId>
    <artifactId>jackson-core-asl</artifactId>
    <version>1.9.9</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.codehaus.jackson</groupId>
    <artifactId>jackson-mapper-asl</artifactId>
    <version>1.9.9</version>
    <scope>test</scope>
</dependency>
```

# 示例web应用结构

## DTO

本文主要处理Form表单，对应的类为`TodoDTO`。`TodoDTO`类是一个简单的Java Bean类，除了setter和getter方法外，还是用到了validator规则：

- title项不能为空
- title项的最大长度为100
- description项的最大长度为500

对应的代码如下：

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
 
    public TodoDTO() {
 
    }
 
    //Getters and setters
}
```

## Service 层

对应地，`TodoService`接口也添加了两个方法：

- `Todo add(TodoDTO added)` 创建并返回Todo项
- `Todo update(TodoDTO updated)` 更新指定的Todo项；如果指定的Todo项不存在，则抛出`TodoNotFoundException`异常

新增代码如下：

```java
public interface TodoService {
 
    public Todo add(TodoDTO added);
 
    public Todo update(TodoDTO updated) throws TodoNotFoundException;
}
```

## Controller 层

对应地，`TodoController`类也增加了4个接口方法：

- `showAddTodoForm()` GET 返回添加Todo项的表单页面
- `add()` POST 处理添加Todo项的表单请求
- `showUpdateTodoForm()` GET 返回修改Todo项的表单页面
- `update()` POST 处理修改Todo项的表单请求

对应的代码如下：

```java
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
 
import javax.annotation.Resource;
import javax.validation.Valid;
 
@Controller
@SessionAttributes("todo")
public class TodoController {
 
    @Resource
    private TodoService service;
 
    @Resource
    private MessageSource messageSource;
 
    @RequestMapping(value = "/todo/add", method = RequestMethod.GET)
    public String showAddTodoForm(Model model) {
        TodoDTO formObject = new TodoDTO();
        model.addAttribute("todo", formObject);
 
        return "todo/add";
    }
 
    @RequestMapping(value = "/todo/add", method = RequestMethod.POST)
    public String add(@Valid @ModelAttribute("todo") TodoDTO dto, BindingResult result, RedirectAttributes attributes) {
        if (result.hasErrors()) {
            return "todo/add";
        }
 
        Todo added = service.add(dto);
 
        addFeedbackMessage(attributes, "feedback.message.todo.added", added.getTitle());
        attributes.addAttribute("id", added.getId());
 
        return createRedirectViewPath("/todo/{id}");
    }
 
    @RequestMapping(value = "/todo/update/{id}", method = RequestMethod.GET)
    public String showUpdateTodoForm(@PathVariable("id") Long id, Model model) throws TodoNotFoundException {
        Todo updated = service.findById(id);
 
        TodoDTO formObject = constructFormObjectForUpdateForm(updated);
        model.addAttribute("todo", formObject);
 
        return "todo/update";
    }
 
    @RequestMapping(value = "/todo/update", method = RequestMethod.POST)
    public String update(@Valid @ModelAttribute("todo") TodoDTO dto, BindingResult result, RedirectAttributes attributes) throws TodoNotFoundException {
        if (result.hasErrors()) {
            return "todo/update";
        }
 
        Todo updated = service.update(dto);
 
        addFeedbackMessage(attributes, "feedback.message.todo.updated", updated.getTitle());
        attributes.addAttribute("id", updated.getId());
 
        return createRedirectViewPath("/todo/{id}");
    }
 
    private TodoDTO constructFormObjectForUpdateForm(Todo updated) {
        TodoDTO dto = new TodoDTO();
 
        dto.setId(updated.getId());
        dto.setDescription(updated.getDescription());
        dto.setTitle(updated.getTitle());
 
        return dto;
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

# 添加测试用例

## GET 添加Todo项表单页面接口

添加测试用例的思路如下：

1. 使用@ExpectedDatabase注解来验证接口没有对数据库表状态产生变化
2. 模拟执行"/todo/add"的GET请求，并取得返回的响应结果
3. 对返回的响应结果作断言：HTTP状态码为200
4. 对返回的响应结果作断言：view的名字是’todo/add’
5. 对返回的响应结果作断言：view的路径为”/WEB-INF/jsp/todo/add.jsp”
6. 对返回的响应结果作断言：model中Todo项各个字段均为空

最终代码如下：

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
    public void showAddTodoForm() throws Exception {
        mockMvc.perform(get("/todo/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("todo/add"))
                .andExpect(forwardedUrl("/WEB-INF/jsp/todo/add.jsp"))
                .andExpect(model().attribute("todo", hasProperty("id", nullValue())))
                .andExpect(model().attribute("todo", hasProperty("description", isEmptyOrNullString())))
                .andExpect(model().attribute("todo", hasProperty("title", isEmptyOrNullString())));
    }
}
```

代码中使用到的`todoData.xml`文件内容如下：

```xml
<dataset>
    <todos id="1" creation_time="2012-10-21 11:13:28" description="Lorem ipsum" modification_time="2012-10-21 11:13:28" title="Foo" version="0"/>
    <todos id="2" creation_time="2012-10-21 11:13:28" description="Lorem ipsum" modification_time="2012-10-21 11:13:28" title="Bar" version="0"/>
</dataset>
```

## POST 添加Todo项表单处理接口

处理一个添加Todo项的表单请求，可能会有3种处理结果：

- 表单提交的Todo项为空，添加失败，返回错误提示
- 表单提交的Todo项的title/description字段值长度不合法，添加失败，返回错误提示
- 表单提交的Todo项各个字段合法，添加成功

下面来分别编写测试用例。

### 提交空表单

添加测试用例的思路如下：

1. 使用@ExpectedDatabase注解来验证接口没有对数据库表状态产生变化
2. 模拟执行"/todo/add"的POST请求，并取得返回的响应结果：Content-type 设置为"application/x-www-form-urlencoded"
3. 对返回的响应结果作断言：HTTP状态码为200
4. 对返回的响应结果作断言：view的名字是’todo/add’
5. 对返回的响应结果作断言：view的路径为”/WEB-INF/jsp/todo/add.jsp”
6. 对返回的响应结果作断言：model中存在预期的错误提示信息

最终代码如下：

```java
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.server.MockMvc;
import org.springframework.test.web.server.samples.context.WebContextLoader;
 
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.server.request.MockMvcRequestBuilders.post;
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
    public void addEmptyTodo() throws Exception {
        mockMvc.perform(post("/todo/add")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .sessionAttr("todo", new TodoDTO())
        )
                .andExpect(status().isOk())
                .andExpect(view().name("todo/add"))
                .andExpect(forwardedUrl("/WEB-INF/jsp/todo/add.jsp"))
                .andExpect(model().attributeHasFieldErrors("todo", "title"))
                .andExpect(model().attribute("todo", hasProperty("id", nullValue())))
                .andExpect(model().attribute("todo", hasProperty("description", isEmptyOrNullString())))
                .andExpect(model().attribute("todo", hasProperty("title", isEmptyOrNullString())));
    }
}
```

### 表单验证失败

添加测试用例的思路如下：

1. 使用@ExpectedDatabase注解来验证接口没有对数据库表状态产生变化
2. 模拟执行"/todo/add"的POST请求，并取得返回的响应结果：Content-type 设置为"application/x-www-form-urlencoded"
3. 对返回的响应结果作断言：HTTP状态码为200
4. 对返回的响应结果作断言：view的名字是’todo/add’
5. 对返回的响应结果作断言：view的路径为”/WEB-INF/jsp/todo/add.jsp”
6. 对返回的响应结果作断言：model中存在预期的错误提示信息

最终代码如下：

```java
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.server.MockMvc;
import org.springframework.test.web.server.samples.context.WebContextLoader;
 
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.server.request.MockMvcRequestBuilders.post;
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
    public void addTodoWhenTitleAndDescriptionAreTooLong() throws Exception {
        String title = TodoTestUtil.createStringWithLength(101);
        String description = TodoTestUtil.createStringWithLength(501);
 
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
    }
}
```

### 表单被正确处理

添加测试用例的思路如下：

1. 使用@ExpectedDatabase注解来验证接口往数据库中写入了一条记录
2. 模拟执行"/todo/add"的POST请求，并取得返回的响应结果：Content-type 设置为"application/x-www-form-urlencoded"
3. 对返回的响应结果作断言：HTTP状态码为200
4. 对返回的响应结果作断言：view的名字是’redirect:/todo/view/{id}’
6. 对返回的响应结果作断言：model中返回的Todo项的id值为3

最终代码如下：

```java
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.server.MockMvc;
import org.springframework.test.web.server.samples.context.WebContextLoader;
 
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.server.request.MockMvcRequestBuilders.post;
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
    @ExpectedDatabase(value="toDoData-add-expected.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void addTodo() throws Exception {
        mockMvc.perform(post("/todo/add")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("description", "description")
                .param("title", "title")
                .sessionAttr("todo", new TodoDTO())
        )
                .andExpect(status().isOk())
                .andExpect(view().name("redirect:/todo/view/{id}"))
                .andExpect(model().attribute("id", is("3")))
                .andExpect(flash().attribute("feedbackMessage", is("Todo entry: title was added.")));
    }
}
```

## GET 修改Todo项表单页面接口

根据参数的不同，该请求会返回不同的结果：

- 如果指定的Todo项被找到，返回修改页面
- 如果指定的Todo项未找到，返回404页面

下面来分别编写测试用例。

### 指定Todo项被找到

添加测试用例的思路如下：

1. 使用@ExpectedDatabase注解来验证接口没有对数据库表状态产生变化
2. 模拟执行"/todo/update/1"的GET请求，并取得返回的响应结果
3. 对返回的响应结果作断言：HTTP状态码为200
4. 对返回的响应结果作断言：view的名字是’todo/update’
5. 对返回的响应结果作断言：view的路径为”/WEB-INF/jsp/todo/update.jsp”
6. 对返回的响应结果作断言：model中存在预期的错误提示信息

最终代码如下：

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
    public void showUpdateTodoForm() throws Exception {
        mockMvc.perform(get("/todo/update/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(view().name("todo/update"))
                .andExpect(forwardedUrl("/WEB-INF/jsp/todo/update.jsp"))
                .andExpect(model().attribute("todo", hasProperty("id", is(1L))))
                .andExpect(model().attribute("todo", hasProperty("description", is("Lorem ipsum"))))
                .andExpect(model().attribute("todo", hasProperty("title", is("Foo"))));
    }
}
```

### 指定Todo项未找到

添加测试用例的思路如下：

1. 使用@ExpectedDatabase注解来验证接口没有对数据库表状态产生变化
2. 模拟执行"/todo/update/3"的GET请求，并取得返回的响应结果
3. 对返回的响应结果作断言：HTTP状态码为404
4. 对返回的响应结果作断言：view的名字是’error/404’
5. 对返回的响应结果作断言：view的路径为”/WEB-INF/jsp/error/404.jsp”

最终代码如下：

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
    public void showUpdateTodoFormWhenTodoIsNotFound() throws Exception {
        mockMvc.perform(get("/todo/update/{id}", 3L))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(forwardedUrl("/WEB-INF/jsp/error/404.jsp"));
    }
}
```

## POST 修改Todo项表单处理接口

处理一个修改Todo项的表单请求，可能会有以下4个结果：

- 表单里的Todo项为空，修改失败，返回错误信息
- 表单里的Todo项参数不合法，修改失败，返回错误信息
- 表单里指定的Todo项被正确修改
- 表单里指定的Todo项不存在，修改失败

下面来分别编写测试用例。

### 提交空表单

添加测试用例的思路如下：

1. 使用@ExpectedDatabase注解来验证接口没有对数据库表状态产生变化
2. 模拟执行"/todo/update"的POST请求，并取得返回的响应结果：Content-type 设置为"application/x-www-form-urlencoded"
3. 对返回的响应结果作断言：HTTP状态码为200
4. 对返回的响应结果作断言：view的名字是’todo/update’
5. 对返回的响应结果作断言：view的路径为”/WEB-INF/jsp/todo/update.jsp”
6. 对返回的响应结果作断言：model中存在预期的错误提示信息

最终代码如下：

```java
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.server.MockMvc;
import org.springframework.test.web.server.samples.context.WebContextLoader;
 
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.server.request.MockMvcRequestBuilders.post;
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
    public void updateEmptyTodo() throws Exception {
        mockMvc.perform(post("/todo/update")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("id", "1")
                .sessionAttr("todo", new TodoDTO())
        )
                .andExpect(status().isOk())
                .andExpect(view().name("todo/update"))
                .andExpect(forwardedUrl("/WEB-INF/jsp/todo/update.jsp"))
                .andExpect(model().attributeHasFieldErrors("todo", "title"))
                .andExpect(model().attribute("todo", hasProperty("id", is(1L))))
                .andExpect(model().attribute("todo", hasProperty("description", isEmptyOrNullString())))
                .andExpect(model().attribute("todo", hasProperty("title", isEmptyOrNullString())));
    }
}
```

### 表单验证失败

添加测试用例的思路如下：

1. 使用@ExpectedDatabase注解来验证接口没有对数据库表状态产生变化
2. 模拟执行"/todo/update"的POST请求，并取得返回的响应结果：Content-type 设置为"application/x-www-form-urlencoded"
3. 对返回的响应结果作断言：HTTP状态码为200
4. 对返回的响应结果作断言：view的名字是’todo/update’
5. 对返回的响应结果作断言：view的路径为”/WEB-INF/jsp/todo/update.jsp”
6. 对返回的响应结果作断言：model中存在预期的错误提示信息

最终代码如下：

```java
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.server.MockMvc;
import org.springframework.test.web.server.samples.context.WebContextLoader;
 
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.server.request.MockMvcRequestBuilders.post;
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
    public void updateTodoWhenTitleAndDescriptionAreTooLong() throws Exception {
        String title = TodoTestUtil.createStringWithLength(101);
        String description = TodoTestUtil.createStringWithLength(501);
 
        mockMvc.perform(post("/todo/update")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("description", description)
                .param("id", "1")
                .param("title", title)
                .sessionAttr("todo", new TodoDTO())
        )
                .andExpect(status().isOk())
                .andExpect(view().name("todo/update"))
                .andExpect(forwardedUrl("/WEB-INF/jsp/todo/update.jsp"))
                .andExpect(model().attributeHasFieldErrors("todo", "title"))
                .andExpect(model().attributeHasFieldErrors("todo", "description"))
                .andExpect(model().attribute("todo", hasProperty("id", is(1L))))
                .andExpect(model().attribute("todo", hasProperty("description", is(description))))
                .andExpect(model().attribute("todo", hasProperty("title", is(title))));
    }
}
```

### 表单被正确处理

添加测试用例的思路如下：

1. 使用@ExpectedDatabase注解来验证接口修改了数据库的一条数据
2. 模拟执行"/todo/update"的POST请求，并取得返回的响应结果：Content-type 设置为"application/x-www-form-urlencoded"
3. 对返回的响应结果作断言：HTTP状态码为200
4. 对返回的响应结果作断言：view的名字是’redirect:/todo/view/{id}’
6. 对返回的响应结果作断言：model中返回的Todo项的id值为1

最终代码如下：

```java
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.server.MockMvc;
import org.springframework.test.web.server.samples.context.WebContextLoader;
 
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.server.request.MockMvcRequestBuilders.post;
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
    @ExpectedDatabase(value="toDoData-update-expected.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void updateTodo() throws Exception {
        mockMvc.perform(post("/todo/update")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("description", "description")
                .param("id", "1")
                .param("title", "title")
                .sessionAttr("todo", new TodoDTO())
        )
                .andExpect(status().isOk())
                .andExpect(view().name("redirect:/todo/view/{id}"))
                .andExpect(model().attribute("id", is("1")))
                .andExpect(flash().attribute("feedbackMessage", is("Todo entry: title was updated.")));
    }
}
```

进行数据库验证的`toDoData-update-expected.xml`文件的内容如下：

```xml
<dataset>
    <todos id="1" description="description" title="title" version="1"/>
    <todos id="2" description="Lorem ipsum" title="Bar" version="0"/>
</dataset>
```

### 指定Todo项未找到

添加测试用例的思路如下：

1. 使用@ExpectedDatabase注解来验证接口没有对数据库表状态产生变化
2. 模拟执行"/todo/update"的POST请求，并取得返回的响应结果：Content-type 设置为"application/x-www-form-urlencoded"
3. 对返回的响应结果作断言：HTTP状态码为404
4. 对返回的响应结果作断言：view的名字是’error/404’
5. 对返回的响应结果作断言：view的路径为”/WEB-INF/jsp/error/404.jsp”

最终代码如下：

```java
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.server.MockMvc;
import org.springframework.test.web.server.samples.context.WebContextLoader;
 
import static org.springframework.test.web.server.request.MockMvcRequestBuilders.post;
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
    public void updateTodoWhenTodoIsNotFound() throws Exception {
        mockMvc.perform(post("/todo/update")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("description", "description")
                .param("id", "3")
                .param("title", "title")
                .sessionAttr("todo", new TodoDTO())
        )
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(forwardedUrl("/WEB-INF/jsp/error/404.jsp"));
    }
}
```

# 总结

本文主要介绍了如何编写集成测试用例测试表单处理接口，要点如下：

- 如何指定请求的content type
- 如何模拟表单请求
- 如何在Session中添加数据
- 如何检测响应数据中包含了错误提示信息

下一篇是 [Spring MVC Integration Testing - REST API](/2016/04/09/spring-mvc-testing-integration-testing-rest-api/)。
