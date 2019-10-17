+++
title = "JOOQ 的使用 - 执行 SQL 命令语句"
description = "jOOQ 是基于 JDBC 之上的一个抽象层，提供了多种多样的模型来与关系型数据库进行互操作；其使用与 mybatis 和 Hibernate ORM 不同的思路来实现对象关系映射(ORM) 。本篇主要介绍基于 jOOQ 的 SQL 命令语句执行。"
date = 2019-10-17T19:09:40+08:00
draft = false
template = "page.html"
[taxonomies]
categories =  ["Java"]
tags = ["jdbc", "sql", "jooq"]
+++

[jOOQ][jooq] 是基于 JDBC 之上的一个抽象层，提供了多种多样的模型来与关系型数据库进行互操作；其使用与 [mybatis](https://mybatis.org/mybatis-3/) 和 [Hibernate ORM](http://hibernate.org/orm/) 不同的思路来实现 [对象关系映射 ORM](https://en.wikipedia.org/wiki/Object-relational_mapping) 。

本篇主要介绍基于 [jOOQ][jooq] 的 SQL 命令语句执行。

<!-- more -->

[jOOQ][jooq] 可以用来执行裸 SQL 命令语句，也可以用来执行带参数绑定的 SQL 命令语句（只要你的 SQL 正确的话）。

以下所有的用例都基于 [JUnit 5](https://junit.org/junit5/) 框架，测试用的数据库提供方为 [H2][h2] 。

公共代码：

```java
private DSLContext dsl;

@BeforeEach
void setUp(@TempDir final Path dir) throws Exception {
    final String url = "jdbc:h2:" + dir.toString() + "/test.db";
    final Connection connection = DriverManager.getConnection(url, "sa", "");

    dsl = DSL.using(connection, SQLDialect.H2);
}
```

# create table

```java
dsl.execute("create table \"user\"(\n"
            + "\"id\" bigint not null auto_increment, "
            + "\"name\" varchar(100) not null, "
            + "\"created_at\" timestamp not null, "
            + "primary key (\"id\")"
            + ");");
```

# select

```java
final Result<Record> fetch = dsl.fetch("select * from \"user\"");
```

# insert

```java
dsl.execute("insert into \"user\" (\"name\", \"created_at\") "
            + "values (cast(? as varchar), cast(? as timestamp))",
        n, OffsetDateTime.now())
```

# update

```java
dsl.execute("update \"user\" "
            + "set \"name\" = 'Elvis Wang', "
            + "    \"created_at\" = timestamp '2019-10-17 19:46:58.456' "
            + "where \"id\" = ?", 2);
```
# delete

```java
dsl.execute("delete from \"user\" where \"id\" = ?", 2);
```

---

完整的示例代码可以参见 [jOOQ Usecases](https://github.com/wbprime/java-mods/tree/master/jooq-usecases) 。

想了解更多的 [jOOQ][jooq] 用法，可以阅读 [官方文档][documentation] 。

---

以上。

[jooq]: https://www.jooq.org/ "jOOQ generates Java code from your database and lets you build type safe SQL queries through its fluent API."
[documentation]: https://www.jooq.org/learn/ "jOOQ Documentation"
[sql99]: https://en.wikipedia.org/wiki/SQL:1999 "SQL:1999"
[h2]: http://www.h2database.com/html/main.html "H2 Database Engine"
[hsqldb]: http://hsqldb.org "HSQLDB - 100% Java Database"
[mysql]: http://www.mysql.com "The world's most popular open source database"
[postgresql]: https://www.postgresql.org "PostgreSQL: The World's Most Advanced Open Source Relational Database"
