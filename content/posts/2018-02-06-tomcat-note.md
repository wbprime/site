---
title : "Tomcat Note"
date : 2018-02-06T15:28:53+08:00
categories : ["Notes"]
tags : ["tomcat"]
description : "Note of apache tomcat architecture"
draft : false
---

Notes of
[Tomcat 系统架构与设计模式，第 1
部分](https://www.ibm.com/developerworks/cn/java/j-lo-tomcat1/index.html) and 
[Tomcat 系统架构与设计模式，第 2
部分](https://www.ibm.com/developerworks/cn/java/j-lo-tomcat2/).

# Tomcat Architecture

    ---------------------------------------------
    | Server                                    |
    | - Service                                 |
    |   - Connector Coyote                      |
    |   - Connector Coyote                      |
    |   - Connector Coyote                      |
    |   - Connector Coyote                      |
    |   - ...                                   |
    |   - Container Catalina                    |
    |   - Session                               |
    |   - Jasper                                |
    |   - Naming                                |
    |   - Logging                               |
    |   - JMX                                   |
    ---------------------------------------------

# Structures

## LifeCycle

```
void addLifecycleListner(...)
void removeLifecycleListener(...)
LifecycleListeners findLifecycleListeners()
void start()
void stop()
```

## Server

A "Server" is a manager of a collection of "Service".

## Service

A "Service" is a combination of a "Container" and multi "Connector" together with several utils.

## Connector

A "Connector" receive http request and post http response. 

When a http connection comes, a "Connector" passed it to a "Processor" via
`Processor::assign()`.

## Processor

A "Processor" constructs a http request and response and passed it to a
"Container".

## Container

A "Container" is a role, including "Wrapper", "Context", "Host" and "Engine".

    Engine
        |- Host 0
            |- Context
                |- Wrapper
        |- Host 1
            |- Context
                |- Wrapper

## Engine

An "Engine" has no parent.

```
Engine interface
    - String get DefaultHost()
    - void setDefaultHost(String)
    - String getJvmRoute()
    - void setJvmRoute(String)
    - Service getService()
    - void setService(Service)
    - void addDefaultContext(DefaultContext)
    - DefaultContext getDefaultContext()
    - void importDefaultContext()
```

StandardEngine implements Engine.

- Cluster: load balance
- Realm: security management
- Pipeline: logic
- Valve: operations

## Host

A "Host" is a deployer.

- Cluster: load balance
- Realm: security management
- Pipeline: logic
- Valve: operations

## Context

A "Context" is the place where to run a servlet.

- Cluster: load balance
- Realm: security management
- Pipeline: logic
- Valve: operations
- Manager: session management org.apache.catalina.session.StandardManager
- Resources
- Loader: class loader
- Mapper: URL map to wrapper

## Wrapper

A "Wrapper" is a servlet.

- Pipeline: logic
- Valve: operations
- Servlet
- Servelet stack

Following classes implementing "Wrapper".

- `org.apache.catalina.servlets.DefaultServlet`
- `org.apache.jasper.servlet.JspServlet`

## Pipeline and Valve

- `org.apache.catalina.core.ContainerBase`
- `org.apache.catalina.core.StandardEngine`
- `org.apache.catalina.core.StandardHost`
- `org.apache.catalina.core.StandardContext`
- `org.apache.catalina.core.StandardWrapper`
- `org.apache.catalina.core.StandardPipeline`
- `org.apache.catalina.valves.ValveBase`
- `org.apache.catalina.core.StandardEngineValve`
- `org.apache.catalina.core.StandardHostValve`
- `org.apache.catalina.core.StandardContextValve`
- `org.apache.catalina.core.StandardWrapperValve`
