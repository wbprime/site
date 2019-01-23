---
title: "Spring MVC Testing: Content"
date: 2016-04-09 23:03:35
updated: 2016-04-09 23:03:35
categories: ["Spring MVC Testing"]
tags: ["Spring MVC", "testing", "java"]

---

本系列翻译自[Spring MVC Test Tutorial](http://www.petrikainulainen.net/spring-mvc-test-tutorial/)。

Springframework自3.2版本以后，提供了[Spring MVC Test Framework](http://docs.spring.io/spring/docs/3.2.x/spring-framework-reference/html/testing.html#spring-mvc-test-framework)用于对Spring MVC项目进行测试。

本系列一共两个部分：单元测试和集成测试。

单元测试将一个一个的Spring MVC Controller作为一个单元，对每一个接口进行测试。Controller层对Service层的调用使用Mockito进行模拟。

集成测试对整个web服务进行测试，虽然测试的单位仍然是接口，但是测试结果更偏向于生产环境。为了保证测试的稳定性，使用了DBUnit来控制每一次测试的数据样本。

需要注意的是，虽然这个系列将Spring MVC Test Framework分为单元测试和集成测试两个部分，但是对于Spring本身来说，其内部实现都是一样的。单元测试和集成测试的区分，是从开发者的角度进行的区分。

<!-- More -->

Spring MVC 单元测试：

1. [Unit Testing - Configuration](/2016/04/09/spring-mvc-testing-unit-testing-configuration/)
2. [Unit Testing - Normal Controllers](/2016/04/09/spring-mvc-testing-unit-testing-normal-controllers/)
3. [Unit Testing - REST API](/2016/04/09/spring-mvc-testing-unit-testing-rest-api/)

Spring MVC 集成测试：

1. [Integration Testing - Configuration](/2016/04/09/spring-mvc-testing-integration-testing-configuration/)
2. [Integration Testing - Controllers](/2016/04/09/spring-mvc-testing-integration-testing-controllers/)
3. [Integration Testing - Forms](/2016/04/09/spring-mvc-testing-integration-testing-forms/)
4. [Integration Testing - REST API](/2016/04/09/spring-mvc-testing-integration-testing-rest-api/)
5. [Integration Testing - Security](/2016/04/09/spring-mvc-testing-integration-testing-security/)
6. [Integration Testing - JsonPath](/2016/04/09/spring-mvc-testing-integration-testing-jsonpath/)
