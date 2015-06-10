title: 'Spring: At First Glance'
date: 2015-05-28 19:11:56
updated: 2015-06-10 13:11:56
categories: tech
tags: [Java, Spring]
description: The Springframework is famous for IOC and DI features.  What exactly does a Spring based Java application look like? And how to produce a Spring based app without learning a lot of teches and apps like maven and gradle.  Ok, let's start step by step.
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

# Make it run

At the very first, I would like to direct into spring without any dependency handling apps.  Maven and its counterparts are useful and convienent for development, but may not be helpful for studying at the very beginning.

To make it clear, I will start my Spring journey in a special directory `/home/wb/projects/`.  Now you can guess my user name is wb, hahaha.

To handle dependency manually, I need to download [Spring libraries](http://repo.spring.io/release/org/springframework/spring).

Now let's start by creating a project using Intellij Idea named `Spring`.

Now the filesystem hierachy may look like (produced by `tree -F`):

    /home/wb/projects/Spring/
    ├── Spring.iml
    └── src/

Add spring library dependencies for `Spring` project.

1. Open `Project structure` setting dialog by clicking `File|Project Structure...`.  Then add library in `Library` tab.

    Following jar files need be added:

        spring-core-4.1.6.RELEASE.jar
        spring-beans-4.1.6.RELEASE.jar
        spring-context-4.1.6.RELEASE.jar

2. Create a test class named `First`

    Now the filesystem hierachy may look like (produced by `tree -F`):

        /home/wb/projects/Spring/
        ├── Spring.iml
        └── src/
            └── First.java

3. Add a private field named `prop` in `First`, and add setter and getter.

    Here is what `First.java` contains:

        public class First {

            private String prop;

            public First() {}

            public String getProp() {
                return prop;
            }

            public void setProp(String prop) {
                this.prop = prop;
            }

            public void print() {
                System.out.println(prop);
            }

            public static void main(String [] _args) {
                First aFirst = new First();

                aFirst.setProp("Hello world");
                aFirst.print();
            }
        }

    Run it.

        Hello world
        
4. Now create a new file named `first.xml`.

        /home/wb/projects/Spring/
        ├── Spring.iml
        └── src/
            ├── First.java
            └── first.xml

    In `first.xml`:

        <?xml version="1.0" encoding="UTF-8"?>
        <beans xmlns="http://www.springframework.org/schema/beans"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://www.springframework.org/schema/beans
                http://www.springframework.org/schema/beans/spring-beans.xsd">

            <bean id="aFirst" class="First">
                <property name="prop" value="I'm first" />
            </bean>

        </beans>

5. Add Spring specific code in `First.java`.

    In `First.java`:

        import org.springframework.context.ApplicationContext;
        import org.springframework.context.support.ClassPathXmlApplicationContext;

        public class First {

            private String prop;

            public First() {}

            public String getProp() {
                return prop;
            }

            public void setProp(String prop) {
                this.prop = prop;
            }

            public void print() {
                System.out.println(prop);
            }

            public static void main(String [] _args) {
                ApplicationContext con = new ClassPathXmlApplicationContext(new String [] {"first.xml"});

                First aFirst = con.getBean("aFirst", First.class);
                aFirst.print();
            }
        }

    Run it:

        Exception in thread "main" java.lang.NoClassDefFoundError: org/apache/commons/logging/LogFactory
            at org.springframework.context.support.AbstractApplicationContext.<init>(AbstractApplicationContext.java:154)
            at org.springframework.context.support.AbstractApplicationContext.<init>(AbstractApplicationContext.java:215)
            at org.springframework.context.support.AbstractRefreshableApplicationContext.<init>(AbstractRefreshableApplicationContext.java:88)
            at org.springframework.context.support.AbstractRefreshableConfigApplicationContext.<init>(AbstractRefreshableConfigApplicationContext.java:58)
            at org.springframework.context.support.AbstractXmlApplicationContext.<init>(AbstractXmlApplicationContext.java:61)
            at org.springframework.context.support.ClassPathXmlApplicationContext.<init>(ClassPathXmlApplicationContext.java:136)
            at org.springframework.context.support.ClassPathXmlApplicationContext.<init>(ClassPathXmlApplicationContext.java:93)
            ...

    Oh, exceptions!

6. Add `common-logging` dependency.

    According to [Spring guide](http://docs.spring.io/spring/docs/current/spring-framework-reference/htmlsingle/index.html#overview-logging), Spring (explicitly spring-core) depends on [Apache Commons Logging](http://commons.apache.org/proper/commons-logging/).  So I need to download [it](http://commons.apache.org/proper/commons-logging/download_logging.cgi) and add it to project library path.

    Now run it:

        Jun 01, 2015 7:27:19 PM org.springframework.context.support.ClassPathXmlApplicationContext prepareRefresh
        INFO: Refreshing org.springframework.context.support.ClassPathXmlApplicationContext@71933d6c: startup date [Mon Jun 01 19:27:19 CST 2015]; root of context hierarchy
        Jun 01, 2015 7:27:19 PM org.springframework.beans.factory.xml.XmlBeanDefinitionReader loadBeanDefinitions
        INFO: Loading XML bean definitions from class path resource [first.xml]
        Exception in thread "main" java.lang.NoClassDefFoundError: org/springframework/expression/ParserContext
            at org.springframework.context.support.AbstractApplicationContext.prepareBeanFactory(AbstractApplicationContext.java:553)
            at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:455)
            at org.springframework.context.support.ClassPathXmlApplicationContext.<init>(ClassPathXmlApplicationContext.java:139)
            at org.springframework.context.support.ClassPathXmlApplicationContext.<init>(ClassPathXmlApplicationContext.java:93)
            at First.main(First.java:23)
            at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
            at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
            at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
            at java.lang.reflect.Method.invoke(Method.java:606)
            at com.intellij.rt.execution.application.AppMain.main(AppMain.java:140)
            Caused by: java.lang.ClassNotFoundException: org.springframework.expression.ParserContext
            at java.net.URLClassLoader$1.run(URLClassLoader.java:366)
            at java.net.URLClassLoader$1.run(URLClassLoader.java:355)
            at java.security.AccessController.doPrivileged(Native Method)
            at java.net.URLClassLoader.findClass(URLClassLoader.java:354)
            at java.lang.ClassLoader.loadClass(ClassLoader.java:425)
            at sun.misc.Launcher$AppClassLoader.loadClass(Launcher.java:308)
            at java.lang.ClassLoader.loadClass(ClassLoader.java:358)
            ... 10 more

    Wow, another exception!

7. Indeed, I surpress this exception by adding another dependency `spring-expression-4.1.6.RELEASE.jar`.  I dont know why, it just works.

    Run it:

        Jun 01, 2015 7:30:16 PM org.springframework.context.support.ClassPathXmlApplicationContext prepareRefresh
        INFO: Refreshing org.springframework.context.support.ClassPathXmlApplicationContext@6ebfc8d0: startup date [Mon Jun 01 19:30:16 CST 2015]; root of context hierarchy
        Jun 01, 2015 7:30:16 PM org.springframework.beans.factory.xml.XmlBeanDefinitionReader loadBeanDefinitions
        INFO: Loading XML bean definitions from class path resource [first.xml]
        I'm first

8. Finally it works.

