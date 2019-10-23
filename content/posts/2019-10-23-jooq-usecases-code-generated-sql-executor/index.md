+++
title = "JOOQ 的使用 - 从生成代码执行 SQL 命令 (SQL Executor)"
description = "jOOQ 是基于 JDBC 之上的一个抽象层，提供了多种多样的模型来与关系型数据库进行互操作；其使用与 mybatis 和 Hibernate ORM 不同的思路来实现对象关系映射(ORM) 。本篇主要介绍基于 jOOQ 代码生成的 SQL 命令执行 (SQL Executor) 。"
date = 2019-10-23T16:27:38+08:00
draft = false
template = "page.html"
[taxonomies]
categories =  ["Java"]
tags = ["jdbc", "sql", "jooq"]
+++

[jOOQ][jooq] 是基于 JDBC 之上的一个抽象层，提供了多种多样的模型来与关系型数据库进行互操作；其使用与 [mybatis](https://mybatis.org/mybatis-3/) 和 [Hibernate ORM](http://hibernate.org/orm/) 不同的思路来实现 [对象关系映射 ORM](https://en.wikipedia.org/wiki/Object-relational_mapping) 。

[JOOQ 的使用 - 代码生成配置 (PostgreSQL & DDL Driven)](@/posts/2019-10-21-jooq-usecases-code-generating/index.md) 介绍了使用 [jOOQ][jooq] 为数据库表生成实体类代码；[JOOQ 的使用 - 执行 SQL 语句 (SQL Executor)](@/posts/2019-10-17-jooq-usecases-sql-executor/index.md) 介绍了基于 [jOOQ][jooq] 的 SQL 命令执行；作为对比，本篇主要介绍基于生成代码的 SQL 命令执行 (SQL Executor) 。

<!-- more -->

# Generated Classes

本文使用 [代码生成](@/posts/2019-10-21-jooq-usecases-code-generating/index.md) 里面的基于 [PostgreSQL][postgresql] 实例驱动生成的代码，代码结构如下：

```
├── tables
│   ├── daos
│   │   ├── AuthorDao.java
│   │   └── BookDao.java
│   ├── interfaces
│   │   ├── IAuthor.java
│   │   └── IBook.java
│   ├── pojos
│   │   ├── Author.java
│   │   └── Book.java
│   ├── records
│   │   ├── AuthorRecord.java
│   │   └── BookRecord.java
│   ├── Author.java
│   └── Book.java
├── DefaultCatalog.java
├── Indexes.java
├── Keys.java
├── Public.java
├── Sequences.java
└── Tables.java
```

# SQL Building

## Common

公共代码：

```java
private DSLContext dsl;

@BeforeEach
void setUp(@TempDir final Path dir) throws Exception {
    dsl = DSL.using(SQLDialect.POSTGRES);
}
```

## select

```java
final Result<Record4<Long, String, String, String>> fetched =
    dsl.select(Tables.AUTHOR.ID, Tables.AUTHOR.FIRST_NAME,
            Tables.AUTHOR.LAST_NAME, Tables.BOOK.TITLE)
        .from(Tables.AUTHOR.as("u"))
        .join(Tables.BOOK.as("b"))
        .on(Tables.AUTHOR.ID.eq(Tables.BOOK.AUTHOR_ID))
        .where(Tables.AUTHOR.FIRST_NAME.eq("Elvis"))
        .and(Tables.AUTHOR.LAST_NAME.eq("Wang"))
        .fetch();

fetched.forEach(
    r -> System.out.println(
        String.format("Author ID: %d, name %s %s, book title %s",
        r.value1(), r.value2(), r.value3(), r.value4())
    )
);
```

## insert

```java
final Optional<AuthorRecord> inserted = dsl.insertInto(Tables.AUTHOR)
    .columns(Tables.AUTHOR.FIRST_NAME, Tables.AUTHOR.LAST_NAME)
    .values("Elvis", "Wang")
    .returning(Tables.AUTHOR.ID)
    .fetchOptional();

inserted.ifPresent(r -> System.out.println(r.getId()));
```

## update

```java
final int n = dsl.update(Tables.AUTHOR)
    .set(Tables.AUTHOR.FIRST_NAME, "Elvis")
    .set(Tables.AUTHOR.LAST_NAME, "Wang")
    .where(Tables.AUTHOR.ID.eq(1993L))
    .execute();

System.out.println("Update " + n + " records");
```

## delete

```java
final int n = dsl
    .delete(Tables.AUTHOR)
    .where(Tables.AUTHOR.ID.eq(1993L))
    .execute();

System.out.println("Delete " + n + " records");
```

---

完整的示例代码可以参见 [jOOQ Usecases](https://github.com/wbprime/java-mods/tree/master/jooq-usecases) 。

想了解更多的 [jOOQ][jooq] 用法，可以阅读 [官方文档][documentation] 。

---

可以明显感觉到，使用 [jOOQ][jooq] 生成的代码为 SQL 执行提供了更强的类型安全保证。

以上。

[jooq]: https://www.jooq.org/ "jOOQ generates Java code from your database and lets you build type safe SQL queries through its fluent API."
[documentation]: https://www.jooq.org/learn/ "jOOQ Documentation"
[sql99]: https://en.wikipedia.org/wiki/SQL:1999 "SQL:1999"
[h2]: http://www.h2database.com/html/main.html "H2 Database Engine"
[hsqldb]: http://hsqldb.org "HSQLDB - 100% Java Database"
[mysql]: http://www.mysql.com "The world's most popular open source database"
[postgresql]: https://www.postgresql.org "PostgreSQL: The World's Most Advanced Open Source Relational Database"
