---
title: "Spring Mvc Note"
date: 2018-02-06T12:51:13+08:00
categories: ["Notes"]
tags: ["spring mvc"]
description: "Note on Spring MVC"
draft: false
---

# Scenario 1

In client application (application is not web application, e.g may be swing app)

```
private static ApplicationContext context = new  ClassPathXmlApplicationContext("test-client.xml");

context.getBean(name);
```

No need of web.xml. ApplicationContext as container for getting bean service. No need for web server container. In test-client.xml there can be Simple bean with no remoting, bean with remoting.

Conclusion: In Scenario 1 applicationContext and DispatcherServlet are not related.

# Scenario 2

In a server application (application deployed in server e.g Tomcat). Accessed service via remoting from client program (e.g swing app)

Define listener in web.xml

```
<listener>
    <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
</listener>
```

At server startup ContextLoaderListener instantiates beans defined in applicationcontext.xml.

Assuming you have defined the following in applicationcontext.xml:

```
<import resource="test1.xml" />
<import resource="test2.xml" />
<import resource="test3.xml" />
<import resource="test4.xml" />
```

The beans are instantiated from all four configuration files test1.xml, test2.xml, test3.xml, test4.xml.

Conclusion: In Scenario 2 applicationContext and DispatcherServlet are not related.

# Scenario 3

In a web application with spring MVC.

In web.xml define:

```
<servlet>
    <servlet-name>springweb</servlet-name>
    <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>    
</servlet>

<servlet-mapping>
    <servlet-name>springweb</servlet-name>
    <url-pattern>*.action</url-pattern>
</servlet-mapping>
```

When tomcat starts, beans defined in springweb-servlet.xml are instantiated. DispatcherServlet extends FrameworkServlet. In FrameworkServlet bean instantiation takes place for springweb . In our case springweb is FrameworkServlet.

Conclusion: In Scenario 3 applicationContext and DispatcherServlet are not related.

# Scenario 4

In web application with spring MVC. springweb-servlet.xml for servlet and applicationcontext.xml for accessing the business service within the server program or for accessing DB service in another server program.

In web.xml the following are defined:

```
<listener>
    <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
</listener>

<servlet>
    <servlet-name>springweb</servlet-name>
    <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>

</servlet>

<servlet-mapping>
    <servlet-name>springweb</servlet-name>
    <url-pattern>*.action</url-pattern>
</servlet-mapping>
```

At server startup, ContextLoaderListener instantiates beans defined in applicationcontext.xml; assuming you have declared herein:

```
<import resource="test1.xml" />
<import resource="test2.xml" />
<import resource="test3.xml" />
<import resource="test4.xml" />
```

The beans are all instantiated from all four test1.xml, test2.xml, test3.xml, test4.xml. After the completion of bean instantiation defined in applicationcontext.xml then beans defined in springweb-servlet.xml are instantiated.

So instantiation order is root is application context, then FrameworkServlet.

Now it makes clear why they are important in which scenario.
