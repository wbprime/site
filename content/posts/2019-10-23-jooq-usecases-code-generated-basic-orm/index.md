+++
title = "JOOQ 的使用 - 基于代码生成的 ORM (CRUD & DAO)"
description = "jOOQ 是基于 JDBC 之上的一个抽象层，提供了多种多样的模型来与关系型数据库进行互操作；其使用与 mybatis 和 Hibernate ORM 不同的思路来实现对象关系映射(ORM) 。本篇主要介绍基于 jOOQ 代码生成的 ORM 实践：包括关系型对象的 CRUD 和 DAO 。"
date = 2019-10-23T18:07:45+08:00
draft = false
template = "page.html"
[taxonomies]
categories =  ["Java"]
tags = ["jdbc", "sql", "jooq"]
+++

[jOOQ][jooq] 是基于 JDBC 之上的一个抽象层，提供了多种多样的模型来与关系型数据库进行互操作；其使用与 [mybatis](https://mybatis.org/mybatis-3/) 和 [Hibernate ORM](http://hibernate.org/orm/) 不同的思路来实现 [对象关系映射 ORM](https://en.wikipedia.org/wiki/Object-relational_mapping) 。

[JOOQ 的使用 - 代码生成配置 (PostgreSQL & DDL Driven)](@/posts/2019-10-21-jooq-usecases-code-generating/index.md) 介绍了使用 [jOOQ][jooq] 为数据库表生成实体类代码；本篇主要介绍基于生成的关系型实体的 CRUD 和 DAO 实践。

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

# CRUD

对于设置了主键 (Primary Key) 的数据表，[jOOQ][jooq] 代码生成插件会为其生成一个 `org.jooq.UpdatableRecord` 类的子类，如为数据表 `author` 生成类 `AuthorRecord` 。对该类可以直接执行 CRUD 的操作。

```java
final DSLContext dsl = DSL.using(SQLDialect.POSTGRES); // Only for show case

// READ
AuthorRecord fetched =
	dsl.fetchOne(Tables.AUTHOR, Tables.AUTHOR.ID.eq(1993L));
if (null == fetched) {
	fetched = dsl.newRecord(Tables.AUTHOR);

	fetched.setId(1993L);
	fetched.setFirstName("Elvis");
	fetched.setLastName("Wang");
}

fetched.setDateOfBirth(LocalDate.of(1993, 5, 25));
fetched.setYearOfBirth(1993L);

// CREATE on not-existed, otherwise UPDATE
fetched.store();

// DELETE
fetched.delete();
```

如果数据表没有设置主键，则生成的类是 `org.jooq.TableRecord` 的子类而非 `org.jooq.UpdatableRecord` 的子类，也就没有对 CRUD 的支持。

# DAO

[jOOQ][jooq] 为数据表生成的 DAO 实现类实现了 `org.jooq.DAO` 接口，如为数据表 `author` 生成类 `AuthorDao`，可以支持以下方法：

```java
// <R> corresponds to the DAO's related table
// <P> corresponds to the DAO's related generated POJO type
// <T> corresponds to the DAO's related table's primary key type.
// Note that multi-column primary keys are not yet supported by DAOs
public interface DAO<R extends TableRecord<R>, P, T> {

    // These methods allow for inserting POJOs
    void insert(P object) throws DataAccessException;
    void insert(P... objects) throws DataAccessException;
    void insert(Collection<P> objects) throws DataAccessException;

    // These methods allow for updating POJOs based on their primary key
    void update(P object) throws DataAccessException;
    void update(P... objects) throws DataAccessException;
    void update(Collection<P> objects) throws DataAccessException;

    // These methods allow for deleting POJOs based on their primary key
    void delete(P... objects) throws DataAccessException;
    void delete(Collection<P> objects) throws DataAccessException;
    void deleteById(T... ids) throws DataAccessException;
    void deleteById(Collection<T> ids) throws DataAccessException;

    // These methods allow for checking record existence
    boolean exists(P object) throws DataAccessException;
    boolean existsById(T id) throws DataAccessException;
    long count() throws DataAccessException;

    // These methods allow for retrieving POJOs by primary key or by some other field
    List<P> findAll() throws DataAccessException;
    P findById(T id) throws DataAccessException;
    <Z> List<P> fetch(Field<Z> field, Z... values) throws DataAccessException;
    <Z> P fetchOne(Field<Z> field, Z value) throws DataAccessException;

    // These methods provide DAO meta-information
    Table<R> getTable();
    Class<P> getType();
}
```

DAO 的用法不言自明。

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
