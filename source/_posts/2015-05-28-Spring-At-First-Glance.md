title: Spring - At First Glance
date: 2015-05-28 19:11:56
updated: 2015-05-28 19:11:56
categories: Tech
tags: [Java, Spring]
description:
---

[Spring](http://spring.io) is a world famous Java developing framework.

> The Spring Framework is a lightweight solution and a potential one-stop-shop for building your enterprise-ready applications. However, Spring is modular, allowing you to use only those parts that you need, without having to bring in the rest. You can use the IoC container, with any web framework on top, but you can also use only the Hibernate integration code or the JDBC abstraction layer. The Spring Framework supports declarative transaction management, remote access to your logic through RMI or web services, and various options for persisting your data. It offers a full-featured MVC framework, and enables you to integrate AOP transparently into your software.
>
> Spring is designed to be non-intrusive, meaning that your domain logic code generally has no dependencies on the framework itself. In your integration layer (such as the data access layer), some dependencies on the data access technology and the Spring libraries will exist. However, it should be easy to isolate these dependencies from the rest of your code base.

Now I would like to dive into Spring, starting from the most beginning.

# Getting start with Spring

Spring framework provides a tool to help start your application using [Spring Boot](http://projects.spring.io/spring-boot) as soon as possible. [Spring Initializr](http://start.spring.io) is a new feature in Spring framwork 4.x.

> You can use start.spring.io to generate a basic project or follow one of the "Getting Started" guides like the Getting Started Building a RESTful Web Service one. As well as being easier to digest, these guides are very task focused, and most of them are based on Spring Boot. They also cover other projects from the Spring portfolio that you might want to consider when solving a particular problem.

# Installation and usage

1. For [maven](http://maven.apache.org), add Spring modules needed in your pom.xml.

        <dependencies>
            <dependency>
                <groupId>org.springframework</groupId>
                <artifactId>spring-context</artifactId>
                <version>4.1.6.RELEASE</version>
                <scope>runtime</scope>
            </dependency>
        </dependencies>

2. For [Gradle](http://www.gradle.org), include the appropriate URL in the repositories section.

repositories {
    mavenCentral()
        // and optionally...
            maven { url "http://repo.spring.io/release" }
            }

3. For [Ivy](http://ant.apache.org/ivy), the following resolver to your ivysettings.xml.

<resolvers>
    <ibiblio name="io.spring.repo.maven.release"
                m2compatible="true"
                            root="http://repo.spring.io/release/"/>
                            </resolvers>

4. Last but most important, for regular use without each of the three, you can download Spring distribution zip file from <http://repo.spring.io/release/org/springframework/spring>, and add separated jar files into you CLASSPATH.

# Try it out


