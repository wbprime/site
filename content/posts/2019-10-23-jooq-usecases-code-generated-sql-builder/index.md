+++
title = "JOOQ 的使用 - 从生成代码拼接 SQL 语句 (SQL Builder)"
description = "jOOQ 是基于 JDBC 之上的一个抽象层，提供了多种多样的模型来与关系型数据库进行互操作；其使用与 mybatis 和 Hibernate ORM 不同的思路来实现对象关系映射(ORM) 。本篇主要介绍基于 jOOQ 代码生成的 SQL 语句拼接 (SQL Builder) 。"
date = 2019-10-23T10:28:13+08:00
draft = false
template = "page.html"
[taxonomies]
categories =  ["Java"]
tags = ["jdbc", "sql", "jooq"]
+++

[jOOQ][jooq] 是基于 JDBC 之上的一个抽象层，提供了多种多样的模型来与关系型数据库进行互操作；其使用与 [mybatis](https://mybatis.org/mybatis-3/) 和 [Hibernate ORM](http://hibernate.org/orm/) 不同的思路来实现 [对象关系映射 ORM](https://en.wikipedia.org/wiki/Object-relational_mapping) 。

[JOOQ 的使用 - 代码生成配置 (PostgreSQL & DDL Driven)](@/posts/2019-10-21-jooq-usecases-code-generating/index.md) 介绍了使用 [jOOQ][jooq] 为数据库表生成实体类代码；[JOOQ 的使用 - 拼接 SQL 语句 (SQL Builder)](@/posts/2019-10-17-jooq-usecases-sql-builder/index.md) 介绍了基于 [jOOQ][jooq] 的 SQL 语句拼接；作为对比，本篇主要介绍基于生成代码的 SQL 语句拼接 (SQL Builder) 。

<!-- more -->

# Generated Classes

本文使用了 [代码生成](@/posts/2019-10-21-jooq-usecases-code-generating/index.md) 里面的基于 [PostgreSQL][postgresql] 实例驱动生成的代码，代码结构如下：

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

所有的 SQL 生成用例都使用了 [JUnit 5](https://junit.org/junit5/) 的参数化测试能力，参数化的数据为 6 种不同的关系型数据库方言。

公共代码：

```java
// junit 5 parameterized test data source
static Stream<Arguments> dslContexts() {
    return Stream.of(
        Arguments.of(SQLDialect.DEFAULT),
        Arguments.of(SQLDialect.H2),
        Arguments.of(SQLDialect.HSQLDB),
        Arguments.of(SQLDialect.POSTGRES),
        Arguments.of(SQLDialect.MYSQL)
    );
}
```

## select

使用生成类来生成 `select` 的 SQL：

```java
@ParameterizedTest
@MethodSource("dslContexts")
void test_buildSelect(final SQLDialect dialect) {
    final DSLContext dsl = DSL.using(dialect);

    final String sql = dsl
        .select(Tables.AUTHOR.ID, Tables.AUTHOR.FIRST_NAME,
                Tables.AUTHOR.LAST_NAME, Tables.BOOK.TITLE)
        .from(Tables.AUTHOR.as("u"))
        .join(Tables.BOOK.as("b"))
        .on(Tables.AUTHOR.ID.eq(Tables.BOOK.AUTHOR_ID))
        .where(Tables.AUTHOR.FIRST_NAME.eq("Elvis"))
        .and(Tables.AUTHOR.LAST_NAME.eq("Wang"))
        .getSQL(ParamType.INLINED);
    System.out.println(dialect.getName() + " => " + sql);
}
```

生成的 `select` 语句简单对比如下：

- [SQL:1999][sql99]

    ```sql
    select "PUBLIC"."AUTHOR"."ID",
        "PUBLIC"."AUTHOR"."FIRST_NAME",
        "PUBLIC"."AUTHOR"."LAST_NAME",
        "PUBLIC"."BOOK"."TITLE"
    from "PUBLIC"."AUTHOR" "u"
    join "PUBLIC"."BOOK" "b"
    on "PUBLIC"."AUTHOR"."ID" = "PUBLIC"."BOOK"."AUTHOR_ID"
    where ("PUBLIC"."AUTHOR"."FIRST_NAME" = 'Elvis'
    and "PUBLIC"."AUTHOR"."LAST_NAME" = 'Wang')
    ```

- [H2][h2]

    ```sql
    select "PUBLIC"."AUTHOR"."ID",
        "PUBLIC"."AUTHOR"."FIRST_NAME",
        "PUBLIC"."AUTHOR"."LAST_NAME",
        "PUBLIC"."BOOK"."TITLE" from "PUBLIC"."AUTHOR" "u"
    join "PUBLIC"."BOOK" "b"
    on "PUBLIC"."AUTHOR"."ID" = "PUBLIC"."BOOK"."AUTHOR_ID"
    where ("PUBLIC"."AUTHOR"."FIRST_NAME" = 'Elvis'
    and "PUBLIC"."AUTHOR"."LAST_NAME" = 'Wang')
    ```

- [HSQLDB][hsqldb]

    ```sql
    select "PUBLIC"."AUTHOR"."ID",
        "PUBLIC"."AUTHOR"."FIRST_NAME",
        "PUBLIC"."AUTHOR"."LAST_NAME",
        "PUBLIC"."BOOK"."TITLE" from "PUBLIC"."AUTHOR" as "u"
    join "PUBLIC"."BOOK" as "b"
    on "PUBLIC"."AUTHOR"."ID" = "PUBLIC"."BOOK"."AUTHOR_ID"
    where ("PUBLIC"."AUTHOR"."FIRST_NAME" = 'Elvis'
    and "PUBLIC"."AUTHOR"."LAST_NAME" = 'Wang')
    ```

- [MySQL][mysql]

    ```sql
    select `PUBLIC`.`AUTHOR`.`ID`,
        `PUBLIC`.`AUTHOR`.`FIRST_NAME`,
        `PUBLIC`.`AUTHOR`.`LAST_NAME`,
        `PUBLIC`.`BOOK`.`TITLE`
    from `PUBLIC`.`AUTHOR` as `u`
    join `PUBLIC`.`BOOK` as `b`
    on `PUBLIC`.`AUTHOR`.`ID` = `PUBLIC`.`BOOK`.`AUTHOR_ID`
    where (`PUBLIC`.`AUTHOR`.`FIRST_NAME` = 'Elvis'
    and `PUBLIC`.`AUTHOR`.`LAST_NAME` = 'Wang')
    ```

- [Postgres][postgresql]

    ```sql
    select "PUBLIC"."AUTHOR"."ID",
        "PUBLIC"."AUTHOR"."FIRST_NAME",
        "PUBLIC"."AUTHOR"."LAST_NAME",
        "PUBLIC"."BOOK"."TITLE"
    from "PUBLIC"."AUTHOR" as "u"
    join "PUBLIC"."BOOK" as "b"
    on "PUBLIC"."AUTHOR"."ID" = "PUBLIC"."BOOK"."AUTHOR_ID"
    where ("PUBLIC"."AUTHOR"."FIRST_NAME" = 'Elvis'
    and "PUBLIC"."AUTHOR"."LAST_NAME" = 'Wang')
    ```

## insert

使用生成类来生成 `insert` 的 SQL：

```java
@ParameterizedTest
@MethodSource("dslContexts")
void test_buildInsert(final SQLDialect dialect) {
    final DSLContext dsl = DSL.using(dialect);

    final String sql = dsl
        .insertInto(Tables.AUTHOR)
        .columns(Tables.AUTHOR.FIRST_NAME, Tables.AUTHOR.LAST_NAME)
        .values("Elvis", "Wang")
        .getSQL(ParamType.INLINED);
    System.out.println(dialect.getName() + " => " + sql);
}
```

生成的 `insert` 语句简单对比如下：

- [SQL:1999][sql99]

    ```sql
    insert into "PUBLIC"."AUTHOR"
        ("FIRST_NAME", "LAST_NAME")
    values ('Elvis', 'Wang')
    ```

- [H2][h2]

    ```sql
    insert into "PUBLIC"."AUTHOR"
        ("FIRST_NAME", "LAST_NAME")
    values ('Elvis', 'Wang')
    ```

- [HSQLDB][hsqldb]

    ```sql
    insert into "PUBLIC"."AUTHOR"
        ("FIRST_NAME", "LAST_NAME")
    values ('Elvis', 'Wang')
    ```

- [MySQL][mysql]

    ```sql
    insert into `PUBLIC`.`AUTHOR`
        (`FIRST_NAME`, `LAST_NAME`)
    values ('Elvis', 'Wang')
    ```

- [Postgres][postgresql]

    ```sql
    insert into "PUBLIC"."AUTHOR"
        ("FIRST_NAME", "LAST_NAME")
    values ('Elvis', 'Wang')
    ```

## update

使用生成类来生成 `update` 的 SQL：

```java
@ParameterizedTest
@MethodSource("dslContexts")
void test_buildUpdate(final SQLDialect dialect) {
    final DSLContext dsl = DSL.using(dialect);

    final String sql = dsl
        .update(Tables.AUTHOR)
        .set(Tables.AUTHOR.FIRST_NAME, "Elvis")
        .set(Tables.AUTHOR.LAST_NAME, "Wang")
        .where(Tables.AUTHOR.ID.eq(1993L))
        .getSQL(ParamType.INLINED);
    System.out.println(dialect.getName() + " => " + sql);
}
```

生成的 `update` 语句简单对比如下：

- [SQL:1999][sql99]

    ```sql
    update "PUBLIC"."AUTHOR"
    set "PUBLIC"."AUTHOR"."FIRST_NAME" = 'Elvis',
        "PUBLIC"."AUTHOR"."LAST_NAME" = 'Wang'
    where "PUBLIC"."AUTHOR"."ID" = 1993
    ```

- [H2][h2]

    ```sql
    update "PUBLIC"."AUTHOR"
    set "PUBLIC"."AUTHOR"."FIRST_NAME" = 'Elvis',
        "PUBLIC"."AUTHOR"."LAST_NAME" = 'Wang'
    where "PUBLIC"."AUTHOR"."ID" = 1993
    ```

- [HSQLDB][hsqldb]

    ```sql
    update "PUBLIC"."AUTHOR"
    set "PUBLIC"."AUTHOR"."FIRST_NAME" = 'Elvis',
        "PUBLIC"."AUTHOR"."LAST_NAME" = 'Wang'
    where "PUBLIC"."AUTHOR"."ID" = 1993
    ```

- [MySQL][mysql]

    ```sql
    update `PUBLIC`.`AUTHOR`
    set `PUBLIC`.`AUTHOR`.`FIRST_NAME` = 'Elvis',
        `PUBLIC`.`AUTHOR`.`LAST_NAME` = 'Wang'
    where `PUBLIC`.`AUTHOR`.`ID` = 1993
    ```

- [Postgres][postgresql]

    ```sql
    update "PUBLIC"."AUTHOR"
    set "FIRST_NAME" = 'Elvis',
        "LAST_NAME" = 'Wang'
    where "PUBLIC"."AUTHOR"."ID" = 1993
    ```

## delete

使用生成类来生成 `delete` 的 SQL：

```java
@ParameterizedTest
@MethodSource("dslContexts")
void test_buildDelete(final SQLDialect dialect) {
    final DSLContext dsl = DSL.using(dialect);

    final String sql = dsl
        .delete(Tables.AUTHOR)
        .where(Tables.AUTHOR.ID.eq(1993L))
        .getSQL(ParamType.INLINED);
    System.out.println(dialect.getName() + " => " + sql);
}
```

生成的 `delete` 语句简单对比如下：

- [SQL:1999][sql99]

    ```sql
    delete from "PUBLIC"."AUTHOR"
    where "PUBLIC"."AUTHOR"."ID" = 1993
    ```

- [H2][h2]

    ```sql
    delete from "PUBLIC"."AUTHOR"
    where "PUBLIC"."AUTHOR"."ID" = 1993
    ```

- [HSQLDB][hsqldb]

    ```sql
    delete from "PUBLIC"."AUTHOR"
    where "PUBLIC"."AUTHOR"."ID" = 1993
    ```

- [MySQL][mysql]

    ```sql
    delete from `PUBLIC`.`AUTHOR`
    where `PUBLIC`.`AUTHOR`.`ID` = 1993
    ```

- [Postgres][postgresql]

    ```sql
    delete from "PUBLIC"."AUTHOR"
    where "PUBLIC"."AUTHOR"."ID" = 1993
    ```

---

完整的示例代码可以参见 [jOOQ Usecases](https://github.com/wbprime/java-mods/tree/master/jooq-usecases) 。

想了解更多的 [jOOQ][jooq] 用法，可以阅读 [官方文档][documentation] 。

---

可以明显感觉到，使用 [jOOQ][jooq] 生成的代码为 SQL 拼接提供了更强的类型安全保证。

以上。

[jooq]: https://www.jooq.org/ "jOOQ generates Java code from your database and lets you build type safe SQL queries through its fluent API."
[documentation]: https://www.jooq.org/learn/ "jOOQ Documentation"
[sql99]: https://en.wikipedia.org/wiki/SQL:1999 "SQL:1999"
[h2]: http://www.h2database.com/html/main.html "H2 Database Engine"
[hsqldb]: http://hsqldb.org "HSQLDB - 100% Java Database"
[mysql]: http://www.mysql.com "The world's most popular open source database"
[postgresql]: https://www.postgresql.org "PostgreSQL: The World's Most Advanced Open Source Relational Database"
