---
title : "Spring MVC Unit Testing - REST API"
date : 2016-04-09T23:07:22+08:00
updated: 2016-04-09 23:07:22
categories : ["Spring MVC Testing"]
tags : ["Spring MVC", "testing", "java"]

---

本文是 [Spring MVC Testing](/2016/04/09/spring-mvc-testing-content/) 单元测试系列的第3篇，原文链接：[Unit Testing of Spring MVC Controllers: REST API](http://www.petrikainulainen.net/programming/spring-framework/unit-testing-of-spring-mvc-controllers-rest-api/)。

使用Spring MVC可以很方便第创建REST风格的接口，但是编写REST风格接口的单元测试并不是那么方便。幸运的是，Spring MVC Test极大地简化了我们为REST风格controller编写单元测试的工作。

本文将通过为`Todo`项的增删改查（CRUD）的REST风格接口操作编写单元测试的方式，一步一步地讲解如何使用Spring MVC Test来进行单元测试。OK，我们快点进入正文吧！

<!-- More -->

# 通过Maven获取依赖

本系列用到的依赖如下：

- Hamcrest 1.3 (hamcrest-all)
- Junit 4.11
- Mockito 1.9.5 (mockito-core)
- Spring Test 3.2.3.RELEASE
- JsonPath 0.8.1 (json-path and json-path-assert)

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
<dependency>
    <groupId>com.jayway.jsonpath</groupId>
    <artifactId>json-path</artifactId>
    <version>0.8.1</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.jayway.jsonpath</groupId>
    <artifactId>json-path-assert</artifactId>
    <version>0.8.1</version>
    <scope>test</scope>
</dependency>
```

# 测试用例配置

同前一篇一样，我们使用`WebApplicationContext`方式来进行单元测试，这意味着我们需要按照之前介绍的配置方法配置上下文。由于之前对这部分内容已经有了详细的介绍，这里恕不赘述。

唯一需要注意的一点是，前面我们演示了使用`SimpleMappingExceptionResolver` bean来映射异常的处理方法，这在标准的controller里面很有用；但对于REST controller而言，异常需要使用`ResponseStatusExceptionResolver` bean来处理。更进一步，我们在工程中使用了`@ControllerAdvice`来创建自定义的异常映射处理类。下文我们会详细讲解该类，在此之前，我们先看看如何实现REST controller。

# 测试用例类

要针对REST接口编写单元测试，首先要准备一些基础知识：

- Spring MVC Test如何来进行单元测试，相关内容详见 [Spring MVC Unit Testing - Normal Controllers](/2016/04/09/spring-mvc-testing-unit-testing-normal-controllers/)
- 如何对json结果作断言，我们选择的是 [JsonPath](https://github.com/jayway/JsonPath)

然后我们可以开始编写代码了。作为演示，我们将编写一下3种类型的REST接口的单元测试：

- 返回`Todo`项列表GET结果的接口
- 返回`TOdo`项GET结果的接口
- 返回`Todo`项POST结果的接口

## GET `Todo`项列表的接口

首先看一下该接口的实现代码。

### 预期的实现

预期的接口应该做以下几件事：

1. 接收到"/api/todo"上的GET请求，开始处理流程
2. 调用`TodoService`的`findAll()`方法获取到所有的`Todo`对象的列表
3. 将`Todo`列表转换为`TodoDTO`列表
4. 返回`TodoDTO`列表的json表示

`TodoController`类内的相关代码如下：

```java
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
 
import java.util.ArrayList;
import java.util.List;
 
@Controller
public class TodoController {
 
    private TodoService service;
 
    @RequestMapping(value = "/api/todo", method = RequestMethod.GET)
    @ResponseBody
    public List<TodoDTO> findAll() {
        List<Todo> models = service.findAll();
        return createDTOs(models);
    }
 
    private List<TodoDTO> createDTOs(List<Todo> models) {
        List<TodoDTO> dtos = new ArrayList<>();
 
        for (Todo model: models) {
            dtos.add(createDTO(model));
        }
 
        return dtos;
    }
 
    private TodoDTO createDTO(Todo model) {
        TodoDTO dto = new TodoDTO();
 
        dto.setId(model.getId());
        dto.setDescription(model.getDescription());
        dto.setTitle(model.getTitle());
 
        return dto;
    }
}
```

返回的json结果有可能是：

```json
[
    {
        "id":1,
        "description":"Lorem ipsum",
        "title":"Foo"
    },
    {
        "id":2,
        "description":"Lorem ipsum",
        "title":"Bar"
    }
]
```

接下来我们可以编写对应的测试用例了。

### 测试用例：`Todo`项列表GET请求

该测试用例主要工作如下：

1. 准备测试数据
2. 配置mock的`TodoService`实例在`findAll()`方法被调用的时候返回准备的数据
3. 执行一个'/api/todo'的GET请求
4. 对响应作断言：HTTP返回码是200
5. 对响应作断言：Content-type的值是"application/json"，并且字符集是"UTF-8"
6. 对响应作断言：使用jsonpath对结果作验证
7. 检查请求执行过程中mock的`TodoService`实例执行了`findAll()`方法有且仅1次
8. 检查请求执行过程中mock的`TodoService`实例未执行其他方法

相关代码如下：

```java
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
 
import java.util.Arrays;
 
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
 
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestContext.class, WebAppContext.class})
@WebAppConfiguration
public class TodoControllerTest {
 
    private MockMvc mockMvc;
 
    @Autowired
    private TodoService todoServiceMock;
 
    //Add WebApplicationContext field here.
 
    //The setUp() method is omitted.
 
    @Test
    public void findAll_TodosFound_ShouldReturnFoundTodoEntries() throws Exception {
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
 
        verify(todoServiceMock, times(1)).findAll();
        verifyNoMoreInteractions(todoServiceMock);
    }
}
```

代码中使用的`TestUtil.APPLICATION_JSON_UTF8`的定义如下：

```java
public class TestUtil {
 
    public static final MediaType APPLICATION_JSON_UTF8 = 
			new MediaType(
				MediaType.APPLICATION_JSON.getType(),
				MediaType.APPLICATION_JSON.getSubtype, 
				Charset.forName("utf8")
			);
}
```

## GET `Todo`项的接口

首先看一下该接口的实现代码。

### 预期的实现

预期的接口应该做以下几件事：

1. 接收到"/api/todo/{id}"上的GET请求，开始处理流程
2. 调用`TodoService`的`findById()`方法获取到目标Todo对象
3. 将`Todo`项转换为`TodoDTO`项
4. 返回`TodoDTO`项的json表示

`TodoController`类内的相关代码如下：

```java
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
 
@Controller
public class TodoController {
 
    private TodoService service;
 
    @RequestMapping(value = "/api/todo/{id}", method = RequestMethod.GET)
    @ResponseBody
    public TodoDTO findById(@PathVariable("id") Long id) throws TodoNotFoundException {
        Todo found = service.findById(id);
        return createDTO(found);
    }
 
    private TodoDTO createDTO(Todo model) {
        TodoDTO dto = new TodoDTO();
 
        dto.setId(model.getId());
        dto.setDescription(model.getDescription());
        dto.setTitle(model.getTitle());
 
        return dto;
    }
}
```

> 如果抛出`TodoNotFoundException`，程序会怎么处理？

如前所述，我们编写了一个异常处理类来处理异常与HTTP返回码的映射。当程序抛出`TodoNotFoundException`异常时，异常处理类会将该异常转换为404的状态码，并写一条日志。

`RestErrorHandler`类的代码如下：

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
 
@ControllerAdvice
public class RestErrorHandler {
 
    private static final Logger LOGGER = LoggerFactory.getLogger(RestErrorHandler.class);
 
    @ExceptionHandler(TodoNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleTodoNotFoundException(TodoNotFoundException ex) {
        LOGGER.debug("handling 404 error on a todo entry");
    }
}
```

所以，我们的单元测试需要同时测试`Todo`项未找到和已找到的情况。

### 测试用例：`Todo`项GET请求返回404

该测试用例主要工作如下：

1. 配置mock的`TodoService`实例在`findById()`方法被调用的时候抛出`TodoNotFoundException`
2. 执行一个'/api/todo/1'的GET请求
3. 对响应作断言：HTTP返回码是404
4. 检查请求执行过程中mock的`TodoService`实例执行了`findById()`方法有且仅1次
5. 检查请求执行过程中mock的`TodoService`实例未执行其他方法

相关代码如下：

```java
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
 
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
 
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestContext.class, WebAppContext.class})
@WebAppConfiguration
public class TodoControllerTest {
 
    private MockMvc mockMvc;
 
    @Autowired
    private TodoService todoServiceMock;
 
    //Add WebApplicationContext field here.
 
    //The setUp() method is omitted.
 
    @Test
    public void findById_TodoEntryNotFound_ShouldReturnHttpStatusCode404() throws Exception {
        when(todoServiceMock.findById(1L)).thenThrow(new TodoNotFoundException(""));
 
        mockMvc.perform(get("/api/todo/{id}", 1L))
                .andExpect(status().isNotFound());
 
        verify(todoServiceMock, times(1)).findById(1L);
        verifyNoMoreInteractions(todoServiceMock);
    }
}
```

### 测试用例：`Todo`项GET请求返回成功

该测试用例主要工作如下：

1. 准备测试数据
2. 配置mock的`TodoService`实例在`findById()`方法被调用的时候返回准备的数据
3. 执行一个'/api/todo/1'的GET请求
4. 对响应作断言：HTTP返回码是200
5. 对响应作断言：Content-type的值是"application/json"，并且字符集是"UTF-8"
6. 对响应作断言：使用jsonpath对结果作验证
7. 检查请求执行过程中mock的`TodoService`实例执行了`findById()`方法有且仅1次
8. 检查请求执行过程中mock的`TodoService`实例未执行其他方法

代码如下：

```java
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
 
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
 
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestContext.class, WebAppContext.class})
@WebAppConfiguration
public class TodoControllerTest {
 
    private MockMvc mockMvc;
 
    @Autowired
    private TodoService todoServiceMock;
 
    //Add WebApplicationContext field here.
 
    //The setUp() method is omitted.
 
    @Test
    public void findById_TodoEntryFound_ShouldReturnFoundTodoEntry() throws Exception {
        Todo found = new TodoBuilder()
                .id(1L)
                .description("Lorem ipsum")
                .title("Foo")
                .build();
 
        when(todoServiceMock.findById(1L)).thenReturn(found);
 
        mockMvc.perform(get("/api/todo/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.description", is("Lorem ipsum")))
                .andExpect(jsonPath("$.title", is("Foo")));
 
        verify(todoServiceMock, times(1)).findById(1L);
        verifyNoMoreInteractions(todoServiceMock);
    }
}
```

## POST `Todo`项的接口

首先看一下该接口的实现代码。

### 预期的实现

预期的接口应该做以下几件事：

1. 接收到"/api/todo"上的POST请求，开始处理流程
2. 校验参数向`TodoDTO`的转换
3. 调用`TodoService`的`add()`方法添加指定的`Todo`项
4. 将`Todo`项转换为`TodoDTO`项
5. 返回`TodoDTO`项的json表示

`TodoController`类内的相关代码如下：

```java
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
 
import javax.validation.Valid;
 
@Controller
public class TodoController {
 
    private TodoService service;
 
    @RequestMapping(value = "/api/todo", method = RequestMethod.POST)
    @ResponseBody
    public TodoDTO add(@Valid @RequestBody TodoDTO dto) {
        Todo added = service.add(dto);
        return createDTO(added);
    }
 
    private TodoDTO createDTO(Todo model) {
        TodoDTO dto = new TodoDTO();
 
        dto.setId(model.getId());
        dto.setDescription(model.getDescription());
        dto.setTitle(model.getTitle());
 
        return dto;
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

- 参数校验通过，接口返回json
- 参数校验没有通过，异常被映射为状态码400

也就是说，如果参数正确，返回结果的状态码为200，返回json形如：

```json
{
    "fieldErrors":[
        {
            "path":"description",
            "message":"The maximum length of the description is 500 characters."
        },
        {
            "path":"title",
            "message":"The maximum length of the title is 100 characters."
        }
    ]
}
```

如果参数不正确，返回结果的状态码为400，返回json形如：

```json
{
    "id":1,
    "description":"description",
    "title":"todo"
}
```

接下来可以开始编写测试用例了。

### 测试用例：`Todo`项POST请求失败

该测试用例主要工作如下：

1. 创建一个不符合验证规则的title
2. 创建一个不符合验证规则的description
3. 执行一个'/api/todo'的POST请求
4. 对响应作断言：HTTP返回码是400
5. 对响应作断言：Content-type的值是"application/json"，并且字符集是"UTF-8"
6. 检查请求执行过程中mock的`TodoService`实例未执行任何方法

代码如下：

```java
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
 
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
 
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestContext.class, WebAppContext.class})
@WebAppConfiguration
public class TodoControllerTest {
 
    private MockMvc mockMvc;
 
    @Autowired
    private TodoService todoServiceMock;
 
    //Add WebApplicationContext field here.
 
    //The setUp() method is omitted.
 
    @Test
    public void 
    add_TitleAndDescriptionAreTooLong_ShouldReturnValidationErrorsForTitleAndDescription()
    throws Exception {
        String title = TestUtil.createStringWithLength(101);
        String description = TestUtil.createStringWithLength(501);
 
        TodoDTO dto = new TodoDTOBuilder()
                .description(description)
                .title(title)
                .build();
 
        mockMvc.perform(post("/api/todo")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(dto))
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
 
        verifyZeroInteractions(todoServiceMock);
    }
}
```

我们使用到了`TestUtil`类，再一次地将该类的代码贴出来以供参考。

```java
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
 
import java.io.IOException;
import java.nio.charset.Charset;
 
public class TestUtil {
 
    public static final MediaType APPLICATION_JSON_UTF8 = 
        new MediaType(
            MediaType.APPLICATION_JSON.getType(), 
            MediaType.APPLICATION_JSON.getSubtype(),
            Charset.forName("utf8")
        );
 
    public static byte[] convertObjectToJsonBytes(Object object) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper.writeValueAsBytes(object);
    }
 
    public static String createStringWithLength(int length) {
        StringBuilder builder = new StringBuilder();
 
        for (int index = 0; index < length; index++) {
            builder.append("a");
        }
 
        return builder.toString();
    }
}
```

### 测试用例：`Todo`项POST请求成功

该测试用例主要工作如下：

1. 准备测试数据
2. 配置mock的`TodoService`实例在`add()`方法被调用的时候返回一个`Todo`项
3. 执行一个'/todo/add'的POST请求
4. 对响应作断言：HTTP返回码是200
5. 对响应作断言：Content-type的值是"application/json"，并且字符集是"UTF-8"
6. 对响应作断言：使用jsonpath对结果作验证

代码如下：

```java
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
 
import static junit.framework.Assert.assertNull;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
 
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestContext.class, WebAppContext.class})
@WebAppConfiguration
public class TodoControllerTest {
 
    private MockMvc mockMvc;
 
    @Autowired
    private TodoService todoServiceMock;
 
    //Add WebApplicationContext field here.
 
    //The setUp() method is omitted.
 
    @Test
    public void 
    add_NewTodoEntry_ShouldAddTodoEntryAndReturnAddedEntry() throws Exception {
        TodoDTO dto = new TodoDTOBuilder()
                .description("description")
                .title("title")
                .build();
 
        Todo added = new TodoBuilder()
                .id(1L)
                .description("description")
                .title("title")
                .build();
 
        when(todoServiceMock.add(any(TodoDTO.class))).thenReturn(added);
 
        mockMvc.perform(post("/api/todo")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(dto))
        )
                .andExpect(status().isOk())
                .andExpect(content().contentType(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.description", is("description")))
                .andExpect(jsonPath("$.title", is("title")));
 
        ArgumentCaptor<TodoDTO> dtoCaptor = ArgumentCaptor.forClass(TodoDTO.class);
        verify(todoServiceMock, times(1)).add(dtoCaptor.capture());
        verifyNoMoreInteractions(todoServiceMock);
 
        TodoDTO dtoArgument = dtoCaptor.getValue();
        assertNull(dtoArgument.getId());
        assertThat(dtoArgument.getDescription(), is("description"));
        assertThat(dtoArgument.getTitle(), is("title"));
    }
}
```

# 总结

本文主要介绍了：

- 如何为REST接口的GET请求编写单元测试
- 如何为REST接口的POST请求编写单元测试
- 如何将对象序列化为json并用POST请求发送出去
- 如何使用JsonPath对返回的结果作断言

到此，整个使用Spring MVC Test来进行单元测试的系列就结束了，希望所有人能有所收获。

本文使用的代码已经放在了 [Github](https://github.com/pkainulainen/spring-mvc-test-examples/tree/master/controllers-unittest) 上，请自行查阅。

翻译完了单元测试的三篇文章，感觉心好累。不得不佩服国外的同行，研究一个东西就研究得很透，分析的很细。本系列不仅仅是一个测试的教程，而且可以作为Spring MVC的开发入门系列。

对作者致以崇高的敬意，和感谢！
