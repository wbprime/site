---
title: 'Spring: Annotation based configuration'
date: 2015-06-08 10:45:49
updated: 2015-06-10 13:45:49
categories: "Spring DI"
tags: [Spring, java]
description: "The Springframework annotation-based metadata configuration guide."
---

Despite XML-based configuration, the Springframework provides full support for annotation-based metadata configuration.

XML-based configuration isolates the configuration metadata from source code using independent xml files, while annotation-based configuration mixes source code and configuration data.  Thus which is the better one to collaborate with the Springframework?  The answer is it depends.  The XML one performes good isolation between source code and configuration, however you need to bear its stupid and complex and long-but-useless xml syntax, and you must do additional work to sync youc onfiguration data and you source code when you need to update your code, which however is very frequent during development.  The annotation one is easy-understanding and simple to update/sync your configuration with your source code.  The cons is obvious that it addes more semantics to a regular Java bean leading to a mixing of configuration and source.

Fortunately you can use both styels and mix them together.  Note that annotation-based configuration is performed before XMl injection.

Note that almost all annotation-based configurations are per-class other than per-bean.

# Dependency Injection Annotations

Beans can be injected into a host bean through annotations.  Typical DI-related annotations are `@Required`, `@Autowired`, `@Resource`, `@PostConstruct`, `@PreDestroy`. 

To enable annotations `@Required`, `@Autowired`, `@Resource`, `@PostConstruct`, `@PreDestroy`, you need to add a `<context:annotation-config />` element in your beans xml file.

    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:context="http://www.springframework.org/schema/context"
        xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd
        http://www.springframework.org/schema/context
        http://www.springframework.org/schema/context/spring-context.xsd">

        <context:annotation-config />
    </beans>

Alternatively you can include explicite `RequiredAnnotationBeanPostProcessor`, `AutowiredAnnotationBeanPostProcessor`, `CommonAnnotationBeanPostProcessor` or `PersistenceAnnotationBeanPostProcessor` dependencies instead.

To make it clear, these annotations help DI procedure, while the annotation detection and bean recovery are done by XML definitions.

## `@Required`

The `@Required` annotation applies to bean property setter to specify that this property must be populated at configuration time.  This annotation is introduced from Spring 2.0.

    public class SimpleMovieLister {

        private MovieFinder movieFinder;

        @Required
        public void setMovieFinder(MovieFinder movieFinder) {
            this.movieFinder = movieFinder;
        }

        // more methods
    }

## `@Autowired`

The `@Autowired` annotation performs autowiring by type.  This annotation is introduced from Spring 2.5.

The `@Autowired` annotation can be applied to field.

    public class OuterBean {
        @Autowired
        private InnerBean inner;
    }

Applied to setter:

    public class OuterBean {
        private InnerBean inner;

        @Required
        public void setInner(InnerBean inner) {
            this.inner = inner;
        }
    }

Applied to regular (set) method:

    public class OuterBean {
        private InnerBean1 inner1;
        private InnerBean2 inner2;

        @Autowired
        public void prepare(InnerBean1 inner1, InnerBean2 inner2) {
            this.inner1 = inner1;
            this.inner2 = inner2;
        }
    }

Applied to constructor:

    public class OuterBean {
        private InnerBean inner;

        @Autowired
        public void OuterBean(InnerBean inner) {
            this.inner = inner;
        }
    }

There should only be one constructor annotated with `@Autowired` in a class.  However you can use `required` attribute to annotate multi constructor.  Typically the target constructor is `public` but not limited to.

    public class OuterBean {
        private InnerBean inner;

        @Autowired
        public void OuterBean(InnerBean inner) {
            this.inner = inner;
        }

        @Autowired(required=false)
        public void OuterBean(InnerBean inner, AnotherBean anther) {
            this.inner = inner;
        }
    }

Applied to array:

    public class OuterBean {
        @Autowired
        private InnerBean[] inners;
    }

Applied to typed collection:

    public class OuterBean {
        @Autowired
        private Set<InnerBean> inners;
    }

Specially, a `Map` can be autowired if and only if its key type is `String`.

    public class OuterBean {
        @Autowired
        private Map<String, InnerBean> innersMap;
    }

## `@Autowired` Combined with `@Qualifier`

When there are multi candidates for auotwiring, `@Qualifier` can help you control wiring details. 

Generally you can specify which bean is needed among multi beans of the same type. For example:

In your annotation source file:

    public class OuterBean {
        @Autowired
        @Qualifier("main")
        private InnerBean inner;
    }

In your xml file:

    <bean id="inner1" class="InnerBean">
        <qualifier value="main"/>
    </bean>

    <bean id="inner2" class="InnerBean">
        <qualifier value="action"/>
    </bean>

    <bean id="outer" class="OuterBean"/>

Thus bean `inner1` will be autowired to bean `outer`.

The `@Qualifier` can also be applied to a construtor argument.

    public class OuterBean {
        @Autowired
        public void OuterBean(@Qualifier("main") InnerBean inner) {
        }
    }

If the value of `@Qualifier` is not given, the bean id/name will be used as a fallback qualifier value.

## Custom `@Qualifier`

Dispite value based `@Qualifier` identification, you can custom your own qualifier annotation and use type based `@Qualifier`.

Given:

    @Target({ElementType.FIELD, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Qualifier
    public @interface Genre {
        String value();
    }

    public class OuterBean {
        @Autowired
        @Genre("Action")
        private InnerBean inner;
    }

Thus following configuration works:

    <context:annotation-config/>

    <bean class="OuterBean">
        <qualifier type="Genre" value="Action"/>
    </bean>

## `@Resource`

The Spring introduces support for JSR-250 annotations like `@Resource` from version 2.5.  `@Resource` works on fields or setters, taking a name attribute indicating the bean id/name to be injected.  One of the differences between `@Resource` and `@Autowired` is that the former performs by-name injection while the latter by-type.

    public class OuterBean {

        @Resource(name="inner")
        private InnerBean inner;

        @Resource
        private InnerBean inner1;

        private InnerBean inner2;

        @Resource
        public void setInner2(InnerBean inner2) { }
    }

The bean `inner` will be injected as it is specified explicitely.  However, if not specified, a fallback injection will be done by setting the field name (for field annotation, i.e., `inner1`) or the arguement name (for setter method, e.g., `inner2`).

## `@PostConstruct` and `@PreDestroy`

Similar to XML based configuration, the Spring annotation based configuration also provides annotations to support bean lifecycle management.

The `@PostConstruct` annotation specifies the callback after bean instantiation and dependency injection.

The `@PreDestroy` annotation specifies the callback before bean destruction.

    public class OuterBean {
        @PostConstruct
        public void myCustomInit() {
        }

        @PreDestroy
        public void myCustomDestroy() {
        }
    }

# Classpath Scanning 

Last annotations demonstrate ways to help dependency injection, however dependency beans themself need to be specified using traditional XML definitions.

More or less, the Springframework also provides annotations to help bean registration and dependecy detection, removing the need of xml files.

## `@Component`, `@Repository`, `@Service` and `@Controller`

The `@Component` annotation is a generic marker of any conponents that are designing to managed by the Spring container.  That is to say, that the any class decorated by `@Component` would be instatiated as a bean, and the beans lifecycle is managed by the Spring container, and the beans will be scanned and filtered in DI step. 

The `@Component` annotation provides three specializtions concentrating on semantics.  The `@Repository` annotation marks, say, a repository type (DAO).  The `@Service` annotation marks a service layer type.  The `@Controller` annotation marks a presentation layer type.  These annotations differ in semantics and may be used differently by the Spring framework in future releases.

    @Component
    public class MyCustomBean {}

    @Repository
    public class MyCustomDAO {}

    @Service
    public class MyCustomService {}

    @Controller
    public class MyCustomController {}

To enable classpath scanning, you need additional work.  Adding following lines in your xml configuration file:

    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:context="http://www.springframework.org/schema/context"
        xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd
        http://www.springframework.org/schema/context
        http://www.springframework.org/schema/context/spring-context.xsd">

        <context:component-scan base-package="org.example"/>

    </beans>

The `<context:component-scan />` calles classpath auto scanning, specifying the qualified package name `org.example`.  This implicit enables the `<context:annotation-config />` which enables `AutowiredAnnotationBeanPostProcessor`, `CommonAnnotationBeanPostProcessor` and `RequiredAnnotationBeanPostProcessor` at the same time.  So the `@Required`, `@Autowired`, `@PostConstruct` and `@PreDestroy` annotations are also out of box.

Also you can implement your own annotations based on `@Component` meta annotation.

    // Spring will treat @MyComponent in the same way as @Component
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Component 
    public @interface MyComponent { }

The custom `@MyComponent` annotation will be treated like the `@Component` besides more costom semantics.

The bean name for a annotated class is generated by the Spring following a default rule: returning the uncapitalized non-qualified class name.

i.g., 

    @Repository
    public class MyCustomDAO {}

will auto introduce a bean named *myCustomDAO".

However, if you don't like this name, you can set `name` using `@Repository("myBestName")`.  The `name` property applies to `@Component`, `@Repository`, `@Service` and `@Controller`.

If you don't like the default naming rule, you can use `BeanNameGenerator` interface.

## `@Scope`

Specifying bean scope using the `@Scope` annotation.

    @Scope("prototype")
    @Repository
    public class MyCustomBean { }

## `@Lazy`

Specifying bean lazy instantiating using the `@Lazy` annotation.

    @Lazy
    @Repository
    public class MyCustomBean { }

## Beans Filtering

You may want to cusotm the beans are to be scanned by setting some filters.  You can achieve this by adding `include-filter` and `exclude-filter` sub-element to `component-scan` element.

    <beans>
        <context:component-scan base-package="org.example">
            <context:include-filter type="regex"
                    expression=".*Stub.*Repository"/>
            <context:exclude-filter type="annotation"
                    expression="org.springframework.stereotype.Repository"/>
        </context:component-scan>
    </beans>

This implies the configuration will ignore all `@Repository` annotations and using "stub Repository" instead.

You must set `type` and `expression` attributes for a `include-filter` or a `exclude-filter` element.  Types are *annotation*, *assignable*, *aspectj*, *regex* and *custom*.

Filter Type          | Example Expression         | Description
-----                | -----                      | -----
annotation (default) | org.example.SomeAnnotation | An annotation to be present at the type level in target components.
assignable           | org.example.SomeClass      | A class (or interface) that the target components are assignable to (extend/implement).
aspectj              | org.example..*Service+     | An AspectJ type expression to be matched by the target components.
regex                | org\.example\.Default.*    | A regex expression to be matched by the target components class names.
custom               | org.example.MyTypeFilter   | A custom implementation of the org.springframework.core.type .TypeFilter interface.

# JSR 3.0 Annotations Alternatives

You can use also JSR 3.0 standard annotations for dependency injection.  These annotations are scanned in the same way as the Spring annotations.

To use JSR 3.0 annotations, you need to add relevant jars to your classpath.

For maven, adding following lines:

    <dependency>
        <groupId>javax.inject</groupId>
        <artifactId>javax.inject</artifactId>
        <version>1</version>
    </dependency>

## `@Inject`

Think `@Inject` as `@Autowired`.

The `@Inject` annotation can be applied at the class-level, field-level, method-level and constructor-argument level.

    import javax.inject.Inject;

    public class OuterBean {
        private InnerBean inner;

        @Inject
        public void setInner(InnerBean inner) {
            this.inner = inner;
        }
    }

## `@Named`

Think `@Named` as `@Component`.

    import javax.inject.Named;

    @Named
    public class OuterBean { }

You can specify a *name* to the `@Named`.

    @Named("myOuterBean")
    public class OuterBean { }

Specially, you can use `@Named` combined with `@Inject`.

    @Inject
    public void setInner(@Named("main") InnerBean inner) {
        this.inner = inner;
    }

## *JSR 3.0 Annotations* VS *Spring Annotations*

The comparison between *JSR 3.0 annotations* VS *Spring annotations* is shown in following table.

Spring              | javax.inject.* | javax.inject restrictions / comments
-----               | -----          | -----
@Autowired          | @Inject        | @Inject has no required attribute
@Component          | @Named         | -
@Scope("singleton") | @Singleton     | The JSR-330 default scope is like Spring’s prototype. However, in order to keep it consistent with Spring’s general defaults, a JSR-330 bean declared in the Spring container is a singleton by default. In order to use a scope other than singleton, you should use Spring’s @Scope annotation.
@Qualifier          | @Named         | -
-@Value             | -              | -no equivalent
@Required           | -              | -no equivalent
@Lazy               | -              | -no equivalent

# Summary

The Spring annotation-based metadata configuration method is a full-featured and easy to use alternative to traditional XML-based metadata configuration.

Note that annotation injection is performed before XML injection, thus the latter configuration will override the former for properties wired through both approaches.
