---
title: 'Spring: XML based configuration'
date: 2015-06-02 10:12:47
updated: 2015-06-10 13:12:47
categories: ["Spring DI"]
tags: [Spring, java]
description: "The Springframework XML-based metadata configuration guide."
---

# Overview

## Spring Configuration

The Springframework supports 3 kinds of configuration methods, including XML-based configuration, annotation-based configuration and Java-based configuration.

- Xml-based configuration: traditionally supplied as a simple and intuitive XMl format metadata.
- Annotation-based configuration: introduced by Spring 2.5.
- Java-based configuration: introduced as the Spring JavaConfig project by Spring 3.0.

XML-based configuration is widely used and best supported.

Typical xml configuration file:

    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd">

        <bean id="aBean" class="MyBean">
        <!-- collaborators and configuration for this bean go here -->
        </bean>

        <!-- more bean definitions go here -->

    </beans>

## ApplicationContext

XML-based configuration provides bean metadata to Spring through a single or a set of XML file(s).  

Generally Spring read configuration metadata via a `BeanFactory` interface.  Direct use of `BeanFactory` is not supported officially in dailly application development, though you can implement `BeanFactory` interface and provide your own class.  However Spring provide a more convinient interface `ApplicationContext` inheriting `BeanFactory`.

The `org.springframework.context.ApplicationContext` interface provides more features over `BeanFactory` interface such as: integration with Spring AOP interfaces, message resource handling for internationalization, event publication, web application support and so on.

Implementations of `ApplicationContext` interface differ in ways reading configuration metadata, among which are `ClassPathXmlApplicationContext` and `FileSystemXmlApplicationContext` that providing control of XML-based configuration metadata. 

`ClassPathXmlApplicationContext` is a standalone XML application context, taking the context definition files from the class path, interpreting plain paths as class path resource names that include the package path (e.g. "mypackage/myresource.txt").  `ClassPathXmlApplicationContext` accepts one or more String and intepretes them as files in CLASSPATH, then loads configuration metadata from those files.  The leading slash in parameters, if exists, will be ignored.

Note: In case of multiple config locations, later bean definitions will override ones defined in earlier loaded files. This can be leveraged to deliberately override certain bean definitions via an extra XML file.

`FileSystemXmlApplicationContext` is a Standalone XML application context, taking the context definition files from the file system or from URLs, interpreting plain paths as relative file system locations (e.g. "mydir/myfile.txt"). Useful for test harnesses as well as for standalone environments.  Plain path(s) will always treated relative to the current VM working directory whether it has or has not leading slash.  To avoid this, add a "file:" prefix to declare an absolute file path.

Note: In case of multiple config locations, later bean definitions will override ones defined in earlier loaded files. This can be leveraged to deliberately override certain bean definitions via an extra XML file.

Typical usage of `ApplicationContext` to load xml configuration metadata:

    // create and configure beans
    ApplicationContext context =
        new ClassPathXmlApplicationContext(new String[] {"mybean.xml", "mybean2.xml"});

    // retrieve configured instance
    MyBean my_bean = context.getBean("aBean", MyBean.class);

    // use configured instance
    boolean is_succeed = myBean.doSomething();

# XML file format

## `beans` 

An xml configuration file is, of cource, a regular xml file with a top-level `<beans />` element and some springframework specific definitions.

    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd">
    </beans>

## `bean` 

An xml file consists of at least one bean definition through `<bean />` element inside a top-level `<beans />` element.  A `<bean />` element represents a bean object; a bean object can be anything whatever you want it to be: a service layer object, a data access object (DAO), a presentation object and so forth, only if it is a standard BEAN object.

    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd">

        <bean id="aSerice" class="me.wbprime.java.MySerice" />
        <bean id="aDao" class="me.wbprime.java.MyDAO" />
        <bean id="aAction" class="me.wbprime.java.MyAction" />
        <bean id="aSession" class="me.wbprime.java.MySession" />

        <!-- more bean definitions go here -->

    </beans>

## `alias` 

Spring supports alias for a bean which is valid in the `ApplicationContext` scope.

In file `a.xml`:

    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd">

        <alias name="appService" alias="b-serice"/>
    </beans>

In file `b.xml`:

    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd">

        <alias name="appService" alias="b-serice"/>
    </beans>

In file `app.xml`:

    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd">

        <alias name="appService" alias="app-serice"/>

        <bean id="appSerice" class="me.wbprime.java.MySerice" />
        <bean id="appDao" class="me.wbprime.java.MyDAO" />
        <bean id="appAction" class="me.wbprime.java.MyAction" />
        <bean id="appSession" class="me.wbprime.java.MySession" />
    </beans>

## `import` 

It can be useful to have bean definitions span multiple XML files. Often each individual XML configuration file represents a logical layer or module in your architecture.  

In file `app.xml`:

    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd">

        <import resource="a.xml"/>
        <import resource="resources/b.xml"/>
        <import resource="/resources/c.xml"/>
    </beans>

Thus loading `app.xml` means loading configuration data from three files: `a.xml`, `b.xml` and `c.xml`.  As a implicit constraint, pathes of all files being imported must be given relative to the file doing the import.  So `a.xml` must be in the same directory or classpath location as `app.xml`, while `b.xml` and `c.xml` must be in `resources` sub-directory of the importing file.  Also a leading slash is ignored.

Note that the contents of the files being imported, including the top level <beans/> element, must be valid XML bean definitions according to the Spring Schema.

# Bean Configuration

Spring container manages one or more beans by `<bean />` definitions.  A `<bean />` element can provide the following metadata for a target `Bean` object:

- a full-qualified class name
- object scope
- object lifecycle behavior
- properties being injected, including plain values and references, also called *collaborators* or *dependencies*
- other configuration

## Name

In Spring, every bean should have one or more identifiers.  These identifiers must be unique within the container that hosts the bean.  

In a `<bean />` element, an identifier can be specified by an `id` attribute.  Note that `id` uniqueness is enforced by the container.

    <bean id="unique id" ... />

Also you can use a `name` attribute.  Interestingly, you can specify more than one identifiers in `name` attribute, treated as aliases, separated by a comma (`,`), a semicolon (`;`) or white space(s).

    <beans name="id1,id2,id3" ... />
    <beans name="id1;id2;id3" ... />
    <beans name="id1 id2 id3" ... />

If neither `id` nor `name` sttribute are explicitly given, Spring would generate a unique identifier for that bean.  However this bean can not be referenced by other beans.

## Class

The object type is specified by `class` attribute for a bean.  The `class` attribute can be used in 2 ways:

- when specifying a static factory method, `class` attribute must match the calss type containning the static factory method.
- when not specified, `class` attribute must match the return type of `new` operator.

        <bean class="ClassA" ... />

## Instantiating a Bean

Spring provides 3 ways to instantiating a bean.

1. using a constructor.

    If you got a class with default constructor which has no parameters, you can simply specify the `class` attribute.

    If you got a class without a default constructor, you can add a `class` attribute and then provide additional `constructor-arg` sub-elements.
    
        <bean id="exampleBean" class="me.wbprime.java.ExampleBean" >
            <constructor-arg value="some value" />
        </bean>

    If your constructor have more than one parameters, for example, class `ExampleBean`:

        package me.wbprime.java

        public class ExampleBean {
            private int _age;
            private String _name;

            public ExampleBean(int age, String name) {
                this._age = age;
                this._name = name;
            }
        }

    You can specify the parameter type:

        <bean id="exampleBean" class="me.wbprime.java.ExampleBean" >
            <constructor-arg type="int" value="26" />
            <constructor-arg type="java.lang.String" value="Elvis Wang" />
        </bean>
 
    Or you can specify the parameter position:

        <bean id="exampleBean" class="me.wbprime.java.ExampleBean" >
            <constructor-arg index="0" value="26" />
            <constructor-arg index="1" value="Elvis Wang" />
        </bean>

    Or you can use parameter names:

        <bean id="exampleBean" class="me.wbprime.java.ExampleBean" >
            <constructor-arg name="age" value="26" />
            <constructor-arg name="name" value="Elvis Wang" />
        </bean>

    Keep in mind that to make this work out of the box your code must be compiled with the debug flag enabled so that Spring can look up the parameter name from the constructor.

2. using a static factory method

    You may want to implement your `Factory method` pattern, by using a static factory method.  Of cource you can make your work together with Spring.  In such case, you need a `class` attribute which contains the static factory method as is discussed right before.  Then you need a new attribute named `factory-method`.

    For example: 

        package me.wbprime.java

        public class ExampleFactory {
            private static ExampleBean aBean = new ExampleBean();
            private ExampleFactory() {}

            public static ExampleBean createBean() {
                return aBean;
            }
        }

    You can add a `<bean />` like this:

        <bean id="aaBean"
            class="me.wbprime.java.ExampleFactory"
            factory-method="createBean"/>

    The definition does not specify the return type.

3. using a non-static factory method

    Similar to a static factory method, instance factory method is also supported by Spring.  All you need to do is to leave the `class` attribute empty, set the `factory-bean` attribute to the bean reference containning the factory method, set the `factory-method` attribute to the name of the factory method.

    Example:

            package me.wbprime.java

            public class ExampleFactory {
                public ExampleFactory() {}

                public ExampleBean createBeanA() {
                    return new BeanA;
                }

                public ExampleBean createBeanB() {
                    return new BeanB;
                }
            }

    Configuration in xml file:

        ...

        <bean id="aFactory" class="ExampleFactory" />
        <bean id="aBean" factory-bean="aFactory" factory-method="createBeanA" />
        <bean id="bBean" factory-bean="aFactory" factory-method="createBeanB" />

        ...

    Note that a `factory-bean` can have more than one factory method, and the return type is not specified.

## Scope

You can control the bean scope by specifying an additional `scope` attribute to your `<bean />` element.  The Spring Framework supports six scopes out of box, four of which are web-aware.

| Scope          | Description                                                                                     |
| -----          | -----                                                                                           |
| singleton      | (Default) Scopes a single bean definition to a single object instance per Spring IoC container. |
| prototype      | Scopes a single bean definition to any number of object instances.                              |
| request        | Scopes a single bean definition to the lifecycle of a single HTTP request. Web-aware.           |
| session        | Scopes a single bean definition to the lifecycle of an HTTP Session. Web-aware.                 |
| global session | Scopes a single bean definition to the lifecycle of a global HTTP Session. Web-aware.           |
| application    | Scopes a single bean definition to the lifecycle of a ServletContext. Web-aware.                |

### `singleton`

The Spring container creates only one instance of that object if you specify singleton scope to a bean by adding a `scope` attribute.

    <bean id="demo1" class="Demo1" scope="singleton" />

Note that this is the default value for `scope` attribute, if not specified explicitly.

### `prototype`

In contrast to singleton, prototype scope means that the Spring container creates an instance each time a request for that bean is made. As a rule, use the prototype scope for all stateful beans and the singleton scope for stateless beans.

    <bean id="demo2" class="Demo2" scope="prototype" />

If you use a singleton-scoped bean which referenced a prototype-scoped bean, all dependencies are injected at instantiation time and injected once.

### `request`, `session` and `global session`

The request, session, and global session scopes are web-aware (using `ApplicationContext` such as `XmlWebApplicationContext`). If you use these scopes with regular `ApplicationContext` such as the `ClassPathXmlApplicationContext`, you get an `IllegalStateException` complaining about an unknown bean scope.

### `application`

The application scope is similar to singleton for a entire web application, but differs in 2 ways: It is a singleton per `ServletContext`, not per Spring `ApplicationContext`, and it is actually exposed and therefore visible as a `ServletContext` attribute.

### Custom Scope

To do.

## Lazy-initializing

By default, the Spring container creates and configures all singleton dependencies when initializing a bean.  If this behavior is not desirable, you can set lazy initializing mode for a bean.

    <bean id="lazyBean" class="LazyBean" lazy-init="true" />

Now `lazyBean` bean will not be pre-instantiated when the Spring container is starting up.  However, if a lazy-initialized bean is referenced by another singleton bean that is not lazy-initialized, the lazy-initialized bean will still be created and initialized when starting up.

## Lifecycle Callbacks

### `init-method`

The Spring provides `init-method` attribute to allow a bean performing initialization work after instantiating and all necessary properties been set.

    <bean id="exampleBean" class="examples.ExampleBean" init-method="init"/>

`init` is a name of method in `examples.ExampleBean` with a `public void init() {..}` like implementation.

### `destroy-method`

The Spring provides `destroy-method` attribute to allow a bean performing cleaning up work before destroyed.

    <bean id="exampleBean" class="examples.ExampleBean" destroy-method="cleanup"/>

`cleanup` is a name of method in `examples.ExampleBean` with a `public void cleanup() {...}` like implementation.

### Default `init/destroy` Method

The fact is that init methods in your project keep the same name such as `init()`, `initialize()` and so on.  Adding it to each bean seems stupid.  To avoid this, you can set `default-init-method` attribute for `beans` element.

    <beans default-init-method="init"/>

Similarly, you can set `default-destroy-method` attribute for destroy methods.

    <beans default-destroy-method="destroy"/>

The `init-method`/`destroy-method` attribute for `bean` element will override the default values set to `beans`.

## Dependency Injection

Dependency injection (DI) is a process that bean objects define their dependencies, while the dependencies are handled by the container.  DI can be fulfilled by 1) interface, 2) constructor and 3) setters.

The Spring provides support for constructor-based DI and setter-based DI.

### Constructor-based DI

This applies to constructor and factory method based bean instantiation process.

    <bean id="bBean" ... >
        <constructor-arg value="lalala" /> <!-- auto detecting -->
        <constructor-arg type="java.lang.String" value="lalala" /> <!-- using type detecting -->
        <constructor-arg index="0" value="lalala" /> <!-- using index detecting -->
        <constructor-arg name="info" value="lalala" /> <!-- using name detecting -->
    </bean>

### Setter-based DI

Setter-based DI applies to beans being instantiated. 

Setter-based DI can be done via `property` sub-element.

    <bean id="exampleBean" class="examples.ExampleBean">
        <property name="prop1">
            <ref bean="anotherExampleBean"/>
        </property>

        <property name="prop2" ref="yetAnotherBean"/>
        <property name="prop3" value="1"/>
    </bean>

### DI Values

The Spring supports constructor-based DI via `<constructor-arg />` element and setter-based DI via `<property />` element.  Values for these two elements can be specified inline (via attribute) or via sub element.  And values can be Java primitives (int, boolean and so on), String (java.lang.String) and reference to other beans.

1. straight values

    Java primitives and String values can be setted using `value` attribute or `value` element.

        <bean id="exampleBean" class="examples.ExampleBean">
            <property name="prop1" value="1"/>
            <property name="prop2">
                <value>1</value>
            </property>
            <property name="prop3" value="My name"/>
            <property name="prop4">
                <value>My name</value>
            </property>
        </bean>

2. bean reference

    References to other beans can be setted using `ref` attribute or `ref` element.  You can specify each of `bean`, `local` and `parent` attributes to `ref` element.
    
    Specifying the target bean through the `bean` attribute of the `<ref/>` tag is the most general form, and allows creation of a reference to any bean in the same container or parent container, regardless of whether it is in the same XML file. The value of the `bean` attribute may be the same as the `id` attribute of the target bean, or as one of the values in the `name` attribute of the target bean.

    Specifying the target bean through the `parent` attribute creates a reference to a bean that is in a parent container of the current container. The value of the `parent` attribute may be the same as either the `id` attribute of the target bean, or one of the values in the `name` attribute of the target bean, and the target bean must be in a parent container of the current one. You use this bean reference variant mainly when you have a hierarchy of containers and you want to wrap an existing bean in a parent container with a proxy that will have the same name as the parent bean.

    The `local` attribute on the `ref` element is no longer supported in the 4.0 beans xsd since it does not provide value over a regular bean reference any more.

        <bean id="exampleBean" class="examples.ExampleBean">
            <property name="prop1" ref="aBean"/>
            <property name="prop2">
                <ref bean="aBean" />
            </property>
        </bean>

3. inner beans

    A `<bean/>` element inside the `<property/>` or `<constructor-arg/>` elements defines a inner bean. An inner bean definition does not require a defined id or name; the container ignores these values. It also ignores the scope flag. Inner beans are always anonymous and they are always created with the outer bean. It is not possible to inject inner beans into collaborating beans other than into the enclosing bean.

        <bean id="outer" class="...">
            <!-- instead of using a reference to a target bean, simply define the target bean inline -->
            <property name="target">
                <bean class="com.example.Person"> <!-- this is the inner bean -->
                    <property name="name" value="Fiona Apple"/>
                    <property name="age" value="25"/>
                </bean>
            </property>
        </bean>

4. collections

    You can set values of Java collection types `List`, `Set`, `Map` and `Properties` via `<list />`, `<set />`, `<map />` and `<props />` elements respectively.

        <bean id="moreComplexObject" class="example.ComplexObject">
            <!-- results in a setAdminEmails(java.util.Properties) call -->
            <property name="adminEmails">
                <props>
                    <prop key="administrator">administrator@example.org</prop>
                    <prop key="support">support@example.org</prop>
                    <prop key="development">development@example.org</prop>
                </props>
            </property>
            <!-- results in a setSomeList(java.util.List) call -->
            <property name="someList">
                <list>
                    <value>a list element followed by a reference</value>
                    <ref bean="myDataSource" />
                </list>
            </property>
            <!-- results in a setSomeMap(java.util.Map) call -->
            <property name="someMap">
                <map>
                    <entry key="an entry" value="just some string"/>
                    <entry key ="a ref" value-ref="myDataSource"/>
                </map>
            </property>
            <!-- results in a setSomeSet(java.util.Set) call -->
            <property name="someSet">
                <set>
                    <value>just some string</value>
                    <ref bean="myDataSource" />
                </set>
            </property>
        </bean>

5. null

    Using `<null />` element to set Java `null` value.

        <bean class="ExampleBean">
            <property name="email">
                <null/>
            </property>
        </bean>

### `p-namespace`

The Spring p-namespace (property namespace ?) is introduced to describe straight values and bean references as a shorcut/replacement for `<property />` element.

    <beans xmlns="http://www.springframework.org/schema/beans"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:p="http://www.springframework.org/schema/p"
        xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd">

Typically 

    <bean name="pnamespaceBean" class="com.example.ExampleBean"
        p:email="foo@bar.com"/>

is the same as 

    <bean name="classicBean" class="com.example.ExampleBean">
        <property name="email" value="foo@bar.com"/>
    </bean>

And also 

    <bean name="pnamespaceStudent"
        class="com.example.Student"
        p:teacher-ref="jane"/>

is the same as 

    <bean name="classicStudent" class="com.example.Student">
        <property name="teacher" ref="jane"/>
    </bean>

Spring p-namespace provide inline attribute for `<property />` element, using `p:PROPERRY` to set straight values and `p:PROPERTY-ref` to set bean references, of which `PROPERTY` is the actual property name.

### `c-namespace`

The Spring c-namespace (constructor namespace ?) is introduced to describe straight values and bean references as a shorcut/replacement for `<constructor-arg />` element.

    <beans xmlns="http://www.springframework.org/schema/beans"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:c="http://www.springframework.org/schema/c"
        xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd">

Typically 

    <bean name="cnamespaceBean" class="com.example.ExampleBean"
        c:email="foo@bar.com"/>

is the same as 

    <bean name="classicBean" class="com.example.ExampleBean">
        <constraint-arg name="email" value="foo@bar.com"/>
    </bean>

And also 

    <bean name="cnamespaceStudent"
        class="com.example.Student"
        c:teacher-ref="jane"/>

is the same as 

    <bean name="classicStudent" class="com.example.Student">
        <constructor-arg name="teacher" ref="jane"/>
    </bean>

Of course the name of constructor parameters should be available (compiled with debug information).  If this is not the case, you can use index position based version.

    <bean id="foo" class="Foo" c:_0="foo@bar.com" c:_1-ref="jane"/>

### Compound Property

You can use compound property to simplify property injection.

    <bean id="foo" class="foo.Bar">
        <property name="fred.bob.sammy" value="123" />
    </bean>

will call `foo.getFred().getBob().setSammy("123")` equally.  In order for this to work, the fred property of foo, and the bob property of fred must not be null after the bean is constructed, or a NullPointerException is thrown.

##  Autowiring

Autowiring is such a feature that the container manages the relationship between a bean and its implicit collaborating beans and resolves the dependencies automatically.  Within the Spring autowiring decrease your work significantly specifying properties/constructor-args and helps when your project evolves and bean dependencies changes slightly.  To be brief, autowiring moves additional work to the Spring container to detect bean dependencies which needs explicit to be done by you programmers. 

The Spring supports 4 kinds of autowiring modes:

- `no` autowiring.  (Default) No autowiring means all dependencies must be specified explicitely.
- `byName` autowiring.  Autowiring is done by property name.
- `byType` autowiring.  Autowiring is done by property type.  Note that if more than one beans for target type exists, a fatal exception is thrown.
- `constructor` autowiring.  Autowiring is done by constructor-arg type, similar to `byType` for property.

```
<bean ... autowire="byName" />
```

If you want to exclude a bean from the autowiring of another bean, you can:

- set `default-autowire-candidates` attribute for top level `<beans />` element.  For example, `<beans ... default-autowire-candidates="*DAO" />` will exclude all beans in this element not matching `"*DAO` pattern for autowiring.
- set `autowire-candidates` attribute for target `<bean />` element to `false`.

The latter takes precedence.

Typically it is best practice to keep consistent across your project when using autowiring.

# Summary

XML based configuration is the traditional method to fulfill ths Spring IoC/DI and other features, and it is the best supported and documented.

Ths Spring also supports Annotation based configuration and Java based configuration.  Annotation based configuration is more convenient and simply in syntax; Java based configuration is implemented using Java classes.

