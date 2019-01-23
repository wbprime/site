---
title : "Spring MVC Integration Testing - REST API"
date : 2016-04-09T23:08:39+08:00
updated: 2016-04-09 23:08:39
categories : ["Spring MVC Testing"]
tags : ["Spring MVC", "testing", "java"]

---

本文是 [Spring MVC Testing](/2016/04/09/spring-mvc-testing-content/) 集成测试系列的第4篇，原文链接：[Integration Testing of Spring MVC Applications: REST API, Part One](http://www.petrikainulainen.net/programming/spring-framework/integration-testing-of-spring-mvc-applications-rest-api-part-one/) 和 [Integration Testing of Spring MVC Applications: REST API, Part Two](http://www.petrikainulainen.net/programming/spring-framework/integration-testing-of-spring-mvc-applications-rest-api-part-two/)。

本文主要介绍如何为基于Spring MVC的REST-full的web应用程序添加集成测试。REST服务通过HTTP标准方法的语义（GET/POST/PUT/DELETE等）来隐喻常见的增删改查（CRUD）操作。

本文主要演示如何一步一步地为REST-full API服务添加集成测试用例，包括：

- 获取Todo项列表接口的集成测试
- 获取单个Todo项接口的集成测试
- 删除单个Todo项接口的集成测试
- 添加新Todo项接口的集成测试
- 更新Todo项接口的集成测试

<!-- More -->

# 示例web应用结构

## Domain 层

Domain层有一个Todo的实体类，代码如下：

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

Service层有一个`TodoService`接口，主要提供了以下个方法：

- `Todo deleteById(Long id)` 在数据库中删除指定的Todo项；如果指定的Todo项不存在，则抛出`TodoNotFoundException`异常
- `List<Todo> findAll()` 返回所有的Todo项的列表；如果没有Todo项，返回空列表
- `Todo findById(Long id)` 返回指定的Todo项；如果指定的Todo项不存在，则抛出`TodoNotFoundException`异常
- `Todo add(TodoDTO added)` 在数据库中添加指定的Todo项
- `Todo update(TodoDTO updated)` 在数据库中更新指定的Todo项；如果指定的Todo项不存在，则抛出`TodoNotFoundException`异常

代码如下：

```java
public interface TodoService {
 
    public Todo deleteById(Long id) throws TodoNotFoundException;
 
    public List<Todo> findAll();
 
    public Todo findById(Long id) throws TodoNotFoundException;

    public Todo add(TodoDTO added);
 
    public Todo update(TodoDTO updated) throws TodoNotFoundException;
}
```

## Controller 层

Controller层有一个`TodoController`类，主要提供json的REST接口以及异常映射机制。

代码如下：

```java
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.*;
 
import javax.annotation.Resource;
import java.util.List;
import java.util.Locale;

@Controller
public class TodoController {
 
    @Resource
    private TodoService service;
 
    @Resource
    private LocaleContextHolderWrapper localeHolderWrapper;
 
    @Resource
    private MessageSource messageSource;

    @RequestMapping(value = "/api/todo/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public TodoDTO deleteById(@PathVariable("id") Long id) throws TodoNotFoundException {
        Todo deleted = service.deleteById(id);
        return createDTO(deleted);
    }
 
    @RequestMapping(value = "/api/todo", method = RequestMethod.GET)
    @ResponseBody
    public List<TodoDTO> findAll() {
        List<Todo> models = service.findAll();
        return createDTOs(models);
    }
 
    private List<TodoDTO> createDTOs(List<Todo> models) {
        List<TodoDTO> dtos = new ArrayList<TodoDTO>();
 
        for (Todo model: models) {
            dtos.add(createDTO(model));
        }
 
        return dtos;
    }
 
    @RequestMapping(value = "/api/todo/{id}", method = RequestMethod.GET)
    @ResponseBody
    public TodoDTO findById(@PathVariable("id") Long id) throws TodoNotFoundException {
        Todo found = service.findById(id);
        return createDTO(found);
    }
 
    @RequestMapping(value = "/api/todo", method = RequestMethod.POST)
    @ResponseBody
    public TodoDTO add(@Valid @RequestBody TodoDTO dto) throws FormValidationError {
        validate("todo", dto);
 
        Todo added = service.add(dto);
 
        return createDTO(added);
    }
 
    @RequestMapping(value = "/api/todo/{id}", method = RequestMethod.PUT)
    @ResponseBody
    public TodoDTO update(
        @Valid @RequestBody TodoDTO dto,
        @PathVariable("id") Long todoId
    ) throws TodoNotFoundException, FormValidationError {
 
        Todo updated = service.update(dto);
 
        return createDTO(updated);
    }
 
    private TodoDTO createDTO(Todo model) {
        TodoDTO dto = new TodoDTO();
 
        dto.setId(model.getId());
        dto.setDescription(model.getDescription());
        dto.setTitle(model.getTitle());
 
        return dto;
    }
 
    @ExceptionHandler(FormValidationError.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public FormValidationErrorDTO handleFormValidationError(FormValidationError validationError) {
        Locale current = localeHolderWrapper.getCurrentLocale();
 
        List<FieldError> fieldErrors = validationError.getFieldErrors();
 
        FormValidationErrorDTO dto = new FormValidationErrorDTO();
 
        for (FieldError fieldError: fieldErrors) {
            String[] fieldErrorCodes = fieldError.getCodes();
            for (int index = 0; index < fieldErrorCodes.length; index++) {
                String fieldErrorCode = fieldErrorCodes[index];
 
                String localizedError = messageSource.getMessage(fieldErrorCode, fieldError.getArguments(), current);
                if (localizedError != null && !localizedError.equals(fieldErrorCode)) {
                    dto.addFieldError(fieldError.getField(), localizedError);
                    break;
                }
                else {
                    if (isLastFieldErrorCode(index, fieldErrorCodes)) {
                        dto.addFieldError(fieldError.getField(), localizedError);
                    }
                }
            }
        }
 
        return dto;
    }
 
    private boolean isLastFieldErrorCode(int index, String[] fieldErrorCodes) {
        return index == fieldErrorCodes.length - 1;
    }
 
    @ExceptionHandler(TodoNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleTodoNotFoundException(TodoNotFoundException ex) {
    }
}
```

## DTO

一共添加了3个DTO类，用于向api的调用者传递数据。

- `TodoDTO`类 用来传递和接收Todo项数据
- `FieldValidationErrorDTO`类 用来传递单参数错误提示
- `FormValidationErrorDTO`类 用来传递多参数错误提示

### TodoDTO

代码如下：

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

### FieldValidationErrorDTO

代码如下：

```java
public class FieldValidationErrorDTO {
 
    private String path;
    private String message;
 
    public FieldValidationErrorDTO(String path, String message) {
        this.path = path;
        this.message = message;
    }
 
    //Getters
}
```

### FormValidationErrorDTO

代码如下：

```java
public class FormValidationErrorDTO {
 
    private List<FieldValidationErrorDTO> fieldErrors = new ArrayList<FieldValidationErrorDTO>();
 
    public FormValidationErrorDTO() {
 
    }
 
    public void addFieldError(String path, String message) {
        FieldValidationErrorDTO fieldError = new FieldValidationErrorDTO(path, message);
        fieldErrors.add(fieldError);
    }
 
    //Getter
}
```

## Exceptions

一共添加了2个异常类：`TodoNotFoundException`和`FormValidationError`。

代码如下：

```java
public class TodoNotFoundException extends Exception {
    public TodoNotFoundException(final String message) {
        super(message);
    }
}

public class FormValidationError extends Exception {
 
    private List<FieldError> fieldErrors;
 
    public FormValidationError(List<FieldError> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }
 
    //Getter
}
```

# 编写测试用例

## GET 获取Todo项列表

添加测试用例的思路如下：

1. 使用@ExpectedDatabase注解来验证接口没有对数据库表状态产生变化
2. 模拟执行"/api/todo"的GET请求，并取得返回的响应结果
3. 对返回的响应结果作断言：HTTP状态码为200
4. 对返回的响应结果作断言：Content-type的值是”application/json”，并且字符集是”UTF-8”
5. 对返回的响应结果作断言：返回的结果符合预期

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
 
import static org.springframework.test.web.server.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.status;
 
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
        mockMvc.perform(get("/api/todo"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].description", is("Lorem ipsum")))
                .andExpect(jsonPath("$[0].title", is("Foo")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].description", is("Lorem ipsum")))
                .andExpect(jsonPath("$[1].title", is("Bar")));
    }
}
```

使用的`toDoData.xml`文件的内容如下：

```xml
<dataset>
    <todos id="1" creation_time="2012-10-21 11:13:28" description="Lorem ipsum" modification_time="2012-10-21 11:13:28" title="Foo" version="0"/>
    <todos id="2" creation_time="2012-10-21 11:13:28" description="Lorem ipsum" modification_time="2012-10-21 11:13:28" title="Bar" version="0"/>
</dataset>
```

## GET 获取单个Todo项

根据参数的不同，获取单个Todo项详情接口会有不同的返回结果：

- 如果指定id的Todo项存在，返回单个Todo项
- 如果指定id的Todo项不存在，返回404

下面来分别编写测试用例。

### 指定的Todo项存在

添加测试用例的思路如下：

1. 使用@ExpectedDatabase注解来验证接口没有对数据库表状态产生变化
2. 模拟执行"/api/todo/1"的GET请求，并取得返回的响应结果
3. 对返回的响应结果作断言：HTTP状态码为200
4. 对返回的响应结果作断言：Content-type的值是”application/json”，并且字符集是”UTF-8”
5. 对返回的响应结果作断言：返回的结果符合预期

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
 
import static org.springframework.test.web.server.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.status;
 
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
        mockMvc.perform(get("/api/todo/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.description", is("Lorem ipsum")))
                .andExpect(jsonPath("$.title", is("Foo")));
    }
}
```

### 指定的Todo项不存在

添加测试用例的思路如下：

1. 使用@ExpectedDatabase注解来验证接口没有对数据库表状态产生变化
2. 模拟执行"/api/todo/3"的GET请求，并取得返回的响应结果
3. 对返回的响应结果作断言：HTTP状态码为404

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
 
import static org.springframework.test.web.server.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.status;
 
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
        mockMvc.perform(get("/api/todo/{id}", 3L))
                .andExpect(status().isNotFound());
    }
}
```

## DELETE 删除指定的Todo项

根据参数的不同，删除指定Todo项接口会有不同的返回结果：

- 如果指定id的Todo项存在，则删除之并返该结果
- 如果指定id的Todo项不存在，返回404

下面来分别编写测试用例。

### 指定的Todo项存在

添加测试用例的思路如下：

1. 使用@ExpectedDatabase注解来验证接口对数据库的操作符合预期
2. 模拟执行"/api/todo/1"的DELETE请求，并取得返回的响应结果
3. 对返回的响应结果作断言：HTTP状态码为200
4. 对返回的响应结果作断言：Content-type的值是”application/json”，并且字符集是”UTF-8”
5. 对返回的响应结果作断言：返回的结果符合预期

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
 
import static org.springframework.test.web.server.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.status;
 
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = WebContextLoader.class, classes = {ExampleApplicationContext.class})
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DbUnitTestExecutionListener.class})
@DatabaseSetup("toDoData.xml")
public class ITTodoControllerTest {
 
    //Add web application context here
 
    private MockMvc mockMvc;
 
    //Add setUp() method here
 
    @Test
    @ExpectedDatabase("toDoData-delete-expected.xml")
    public void deleteById() throws Exception {
        mockMvc.perform(delete("/api/todo/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.description", is("Lorem ipsum")))
                .andExpect(jsonPath("$.title", is("Foo")));
    }
}
```

作为对照，`todoData-delete-expected.xml`文件包含了预期的数据库表结果，内容如下：

```xml
<dataset>
    <todos id="2" creation_time="2012-10-21 11:13:28" description="Lorem ipsum" modification_time="2012-10-21 11:13:28" title="Bar" version="0"/>
</dataset>
```

### 指定的Todo项不存在

添加测试用例的思路如下：

1. 使用@ExpectedDatabase注解来验证接口没有对数据库表状态产生变化
2. 模拟执行"/api/todo/3"的DELETE请求，并取得返回的响应结果
3. 对返回的响应结果作断言：HTTP状态码为404

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
 
import static org.springframework.test.web.server.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.status;
 
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = WebContextLoader.class, classes = {ExampleApplicationContext.class})
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DbUnitTestExecutionListener.class})
@DatabaseSetup("toDoData.xml")
public class ITTodoControllerTest {
 
    //Add web application context here
 
    private MockMvc mockMvc;
 
    //Add setUp() method here
 
    @Test
    @ExpectedDatabase("toDoData.xml")
    public void deleteByIdWhenTodoIsNotFound() throws Exception {
        mockMvc.perform(delete("/api/todo/{id}", 3L))
                .andExpect(status().isNotFound());
    }
}
```

## POST 添加Todo项

根据参数的不同，添加指定Todo项接口会有不同的返回结果：

- Todo项为空，添加失败，返回错误提示
- Todo项的title/description字段值长度不合法，添加失败，返回错误提示
- Todo项各个字段都合法，添加成功，返回该项

下面来分别编写测试用例。

### 指定的Todo项字段为空

添加测试用例的思路如下：

1. 使用@ExpectedDatabase注解来验证接口没有对数据库表状态产生变化
2. 模拟执行"/api/todo"的POST请求，并取得返回的响应结果
3. 对返回的响应结果作断言：HTTP状态码为400
4. 对返回的响应结果作断言：Content-type的值是”application/json”，并且字符集是”UTF-8”
5. 对返回的响应结果作断言：返回了预期的错误说明

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
 
import static org.springframework.test.web.server.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.status;
 
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = WebContextLoader.class, classes = {ExampleApplicationContext.class})
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DbUnitTestExecutionListener.class})
@DatabaseSetup(toDoData.xml)
public class ITTodoControllerTest {
 
    //Add web application context here
 
    private MockMvc mockMvc;
 
    //Add setUp() method here
 
    @Test
    @ExpectedDatabase("toDoData.xml")
    public void addEmptyTodo() throws Exception {
        TodoDTO added = TodoTestUtil.createDTO(null, "", "");
        mockMvc.perform(post(/api/todo)
                .contentType(IntegrationTestUtil.APPLICATION_JSON_UTF8)
                .body(IntegrationTestUtil.convertObjectToJsonBytes(added))
        )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.fieldErrors[0]", is("title")));
    }
}
```

### 指定的Todo项包含不合法字段

添加测试用例的思路如下：

1. 使用@ExpectedDatabase注解来验证接口没有对数据库表状态产生变化
2. 模拟执行"/api/todo"的POST请求，并取得返回的响应结果
3. 对返回的响应结果作断言：HTTP状态码为400
4. 对返回的响应结果作断言：Content-type的值是”application/json”，并且字符集是”UTF-8”
5. 对返回的响应结果作断言：返回了预期的错误说明

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
import static org.springframework.test.web.server.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.status;
 
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = WebContextLoader.class, classes = {ExampleApplicationContext.class})
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DbUnitTestExecutionListener.class})
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
        TodoDTO added = TodoTestUtil.createDTO(null, description, title);
 
        mockMvc.perform(post("/api/todo")
                .contentType(IntegrationTestUtil.APPLICATION_JSON_UTF8)
                .body(IntegrationTestUtil.convertObjectToJsonBytes(added))
        )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.fieldErrors", hasSize(2)))
                .andExpect(
                    jsonPath(
                        "$.fieldErrors[*].path", containsInAnyOrder("title", "description")
                    )
                )
                .andExpect(jsonPath("$.fieldErrors[*].message", containsInAnyOrder(
                        "The maximum length of the description is 500 characters.",
                        "The maximum length of the title is 100 characters."
                )));
    }
}
```

### 添加Todo项成功

添加测试用例的思路如下：

1. 使用@ExpectedDatabase注解来验证接口往数据库中写入了一条记录
2. 模拟执行"/api/todo"的POST请求，并取得返回的响应结果
3. 对返回的响应结果作断言：HTTP状态码为200
4. 对返回的响应结果作断言：Content-type的值是”application/json”，并且字符集是”UTF-8”
5. 对返回的响应结果作断言：返回的结果符合预期

最终代码如下：

```java
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
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
 
import static org.springframework.test.web.server.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.status;
 
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = WebContextLoader.class, classes = {ExampleApplicationContext.class})
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DbUnitTestExecutionListener.class})
@DatabaseSetup("toDoData.xml")
public class ITTodoControllerTest {
 
    //Add web application context here
 
    private MockMvc mockMvc;
 
    //Add setUp() method here
 
    @Test
    @ExpectedDatabase(value="toDoData-add-expected.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void add() throws Exception {
        TodoDTO added = TodoTestUtil.createDTO(null, "description", "title");
        mockMvc.perform(post("/api/todo")
                .contentType(IntegrationTestUtil.APPLICATION_JSON_UTF8)
                .body(IntegrationTestUtil.convertObjectToJsonBytes(added))
        )
                .andExpect(status().isOk())
                .andExpect(content().contentType(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.description", is("description")))
                .andExpect(jsonPath("$.title", is("title")));
    }
}
```

使用到的`toDoData-add-expected.xml`文件的内容如下：

```xml
<dataset>
    <todos id="1" description="Lorem ipsum" title="Foo" version="0"/>
    <todos id="2" description="Lorem ipsum" title="Bar" version="0"/>
    <todos id="3" description="description" title="title" version="0"/>
</dataset>
```

## PUT 修改Todo项

根据参数的不同，添加指定Todo项接口会有不同的返回结果：

- 指定的Todo项为空，修改失败，返回错误信息
- 指定的Todo项参数不合法，修改失败，返回错误信息
- 指定的Todo项被正确修改，返回修改后的结果
- 指定的Todo项不存在，修改失败

下面来分别编写测试用例。

### 指定的Todo项字段为空

添加测试用例的思路如下：

1. 使用@ExpectedDatabase注解来验证接口没有对数据库表状态产生变化
2. 模拟执行"/api/todo/1"的PUT请求，并取得返回的响应结果
3. 对返回的响应结果作断言：HTTP状态码为400
4. 对返回的响应结果作断言：Content-type的值是”application/json”，并且字符集是”UTF-8”
5. 对返回的响应结果作断言：返回了预期的错误说明

最终代码如下：

```java
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
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
 
import static org.springframework.test.web.server.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.status;
 
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = WebContextLoader.class, classes = {ExampleApplicationContext.class})
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DbUnitTestExecutionListener.class})
@DatabaseSetup("toDoData.xml")
public class ITTodoControllerTest {
 
    //Add web application context here
 
    private MockMvc mockMvc;
 
    //Add setUp() method here
 
    @Test
    @ExpectedDatabase("toDoData.xml")
    public void updateEmptyTodo() throws Exception {
        TodoDTO updated = TodoTestUtil.createDTO(1L, "", "");
 
        mockMvc.perform(put("/api/todo/{id}", 1L)
                .contentType(IntegrationTestUtil.APPLICATION_JSON_UTF8)
                .body(IntegrationTestUtil.convertObjectToJsonBytes(updated))
        )
                .andExpect(status().isBadRequest())
                .andExpect(content().mimeType(IntegrationTestUtil.APPLICATION_JSON_UTF8))
                .andExpect(content().string("{\"fieldErrors\":[{\"path\":\"title\",\"message\":\"The title cannot be empty.\"}]}"));
    }
}
```

### 指定的Todo项包含不合法字段

添加测试用例的思路如下：

1. 使用@ExpectedDatabase注解来验证接口没有对数据库表状态产生变化
2. 模拟执行"/api/todo/1"的PUT请求，并取得返回的响应结果
3. 对返回的响应结果作断言：HTTP状态码为400
4. 对返回的响应结果作断言：Content-type的值是”application/json”，并且字符集是”UTF-8”
5. 对返回的响应结果作断言：返回了预期的错误说明

最终代码如下：

```java
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
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
import static org.springframework.test.web.server.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.status;
 
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = WebContextLoader.class, classes = {ExampleApplicationContext.class})
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DbUnitTestExecutionListener.class})
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
 
        TodoDTO updated = TodoTestUtil.createDTO(1L, description, title);
 
        mockMvc.perform(put("/api/todo/{id}", 1L)
                .contentType(IntegrationTestUtil.APPLICATION_JSON_UTF8)
                .body(IntegrationTestUtil.convertObjectToJsonBytes(updated))
        )
                .andExpect(status().isBadRequest())
                .andExpect(content().mimeType(IntegrationTestUtil.APPLICATION_JSON_UTF8))
                .andExpect(content().string(startsWith("{\"fieldErrors\":[")))
                .andExpect(content().string(allOf(
                        containsString("{\"path\":\"description\",\"message\":\"The maximum length of the description is 500 characters.\"}"),
                        containsString("{\"path\":\"title\",\"message\":\"The maximum length of the title is 100 characters.\"}")
                )))
                .andExpect(content().string(endsWith("]}")));
    }
}
```

### 指定的Todo项不存在

添加测试用例的思路如下：

1. 使用@ExpectedDatabase注解来验证接口没有对数据库表状态产生变化
2. 模拟执行"/api/todo/3"的PUT请求，并取得返回的响应结果
3. 对返回的响应结果作断言：HTTP状态码为404

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
 
import static org.springframework.test.web.server.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.status;
 
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = WebContextLoader.class, classes = {ExampleApplicationContext.class})
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DbUnitTestExecutionListener.class})
@DatabaseSetup("toDoData.xml")
public class ITTodoControllerTest {
 
    //Add web application context here
 
    private MockMvc mockMvc;
 
    //Add setUp() method here
 
    @Test
    @ExpectedDatabase("toDoData.xml")
    public void updateTodoWhenTodoIsNotFound() throws Exception {
        TodoDTO updated = TodoTestUtil.createDTO(3L, "description", "title");
 
        mockMvc.perform(put("/api/todo/{id}", 3L)
                .contentType(IntegrationTestUtil.APPLICATION_JSON_UTF8)
                .body(IntegrationTestUtil.convertObjectToJsonBytes(updated))
        )
                .andExpect(status().isNotFound());
    }
}
```

### 指定的Todo项存在

添加测试用例的思路如下：

1. 使用@ExpectedDatabase注解来验证接口对数据库的修改符合预期
2. 模拟执行"/api/todo/1"的PUT请求，并取得返回的响应结果
3. 对返回的响应结果作断言：HTTP状态码为200
4. 对返回的响应结果作断言：Content-type的值是”application/json”，并且字符集是”UTF-8”
5. 对返回的响应结果作断言：返回的结果符合预期

最终代码如下：

```java
import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
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
 
import static org.springframework.test.web.server.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.status;
 
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = WebContextLoader.class, classes = {ExampleApplicationContext.class})
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DbUnitTestExecutionListener.class})
@DatabaseSetup("toDoData.xml")
public class ITTodoControllerTest {
 
    //Add web application context here
 
    private MockMvc mockMvc;
 
    //Add setUp() method here
 
    @Test
    @ExpectedDatabase(value="toDoData-update-expected.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void update() throws Exception {
        TodoDTO updated = TodoTestUtil.createDTO(1L, "description", "title");
 
        mockMvc.perform(put("/api/todo/{id}", 1L)
                .contentType(IntegrationTestUtil.APPLICATION_JSON_UTF8)
                .body(IntegrationTestUtil.convertObjectToJsonBytes(updated))
        )
                .andExpect(status().isOk())
                .andExpect(content().mimeType(IntegrationTestUtil.APPLICATION_JSON_UTF8))
                .andExpect(content().string("{\"id\":1,\"description\":\"description\",\"title\":\"title\"}"));
    }
}
```

代码中使用到的`toDoData-update-expected.xml`文件内容如下：

```xml
<dataset>
    <todos id="1" description="description" title="title" version="1"/>
    <todos id="2" description="Lorem ipsum" title="Bar" version="0"/>
</dataset>
```

# 总结

本文的要点如下：

- 如何对GET接口进行集成测试
- 如何对POST接口进行集成测试
- 如何对PUT接口进行集成测试
- 如何对DELETE接口进行集成测试

下一篇是 [Spring MVC Integration Testing - Security](/2016/04/09/spring-mvc-testing-integration-testing-security/)。
