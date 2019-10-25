+++
title = "JOOQ 的使用 - 使用事务 (Transaction)"
description = "jOOQ 是基于 JDBC 之上的一个抽象层，提供了多种多样的模型来与关系型数据库进行互操作；其使用与 mybatis 和 Hibernate ORM 不同的思路来实现对象关系映射(ORM) 。本篇主要介绍 jOOQ 中对事务 (Transaction) 的支持。"
date = 2019-10-25T18:00:41+08:00
draft = false
template = "page.html"
[taxonomies]
categories =  ["Java"]
tags = ["jdbc", "sql", "jooq"]
+++

[jOOQ][jooq] 是基于 JDBC 之上的一个抽象层，提供了多种多样的模型来与关系型数据库进行互操作；其使用与 [mybatis](https://mybatis.org/mybatis-3/) 和 [Hibernate ORM](http://hibernate.org/orm/) 不同的思路来实现 [对象关系映射 ORM](https://en.wikipedia.org/wiki/Object-relational_mapping) 。

本篇主要介绍 [jOOQ][jooq] 的事务支持。

<!-- more -->

# Common

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

所有的用例都基于 [JUnit 5](https://junit.org/junit5/) 框架，测试用的数据库提供方为 [H2][h2] ；测试代码中使用了参数化测试功能。

预准备阶段需要准备数据库：

```java
private DSLContext dsl;

@BeforeEach
void setUp(@TempDir final Path dir) throws Exception {
	final String url = "jdbc:h2:" + dir.toString() + "/test.db";
	final Connection connection = DriverManager.getConnection(url, "sa", "");

	dsl = DSL.using(connection, SQLDialect.H2);

	// Create table
	dsl.createTableIfNotExists(Tables.AUTHOR)
		.columns(Tables.AUTHOR.ID, Tables.AUTHOR.FIRST_NAME, Tables.AUTHOR.LAST_NAME,
			Tables.AUTHOR.DATE_OF_BIRTH, Tables.AUTHOR.YEAR_OF_BIRTH)
		.constraint(DSL.primaryKey(Tables.AUTHOR.ID))
		.execute();
}

@AfterEach
void tearDown() {
	dsl.close();
}
```

同时，为了简化测试代码，新建了 2 个接口类用于辅助模拟抛出不同异常的情况。

```java
interface ThrowingConsumer {
	void run(final AuthorRecord r) throws Exception;
}

interface ThrowingFunction<T> {
	T call(final AuthorRecord r) throws Exception;
}
```

# TransactionalRunnable

可以使用 `org.jooq.TransactionalRunnable` 接口封装需要在事务中执行的代码；封装好的代码需要提交给 `org.jooq.DSLContext#transaction(org.jooq.TransactionalRunnable)` 方法执行。

示例代码如下：

```java
@ParameterizedTest
@MethodSource("runnables")
void test_transactionRun(final ThrowingConsumer runnable) {
	final int created = dsl.createTableIfNotExists(Tables.AUTHOR)
		.columns(Tables.AUTHOR.ID, Tables.AUTHOR.FIRST_NAME, Tables.AUTHOR.LAST_NAME,
			Tables.AUTHOR.DATE_OF_BIRTH, Tables.AUTHOR.YEAR_OF_BIRTH)
		.constraint(DSL.primaryKey(Tables.AUTHOR.ID))
		.execute();
	Assertions.assertThat(created).isEqualTo(0);

	try {
		dsl.transaction(c -> {
			final DSLContext inner = DSL.using(c);

			final AuthorRecord inserted = inner.insertInto(Tables.AUTHOR)
				.columns(Tables.AUTHOR.FIRST_NAME, Tables.AUTHOR.LAST_NAME)
				.values("Elvis", "Wang")
				.returning(Tables.AUTHOR.ID)
				.fetchOne();

			// assertj
			Assertions.assertThat(inserted).isNotNull();

			runnable.run(inserted);

			final int n = inner.update(Tables.AUTHOR)
				.set(Tables.AUTHOR.FIRST_NAME, "James")
				.set(Tables.AUTHOR.LAST_NAME, "Zhang")
				.where(Tables.AUTHOR.ID.eq(inserted.getId()))
				.execute();

			// assertj
			Assertions.assertThat(n).isEqualTo(1);
		});
	} catch (DataAccessException ex) {
		ex.printStackTrace();
		System.out.println("Failed due to checked exception thrown in transaction");
	} catch (RuntimeException ex) {
		ex.printStackTrace();
		System.out.println("Failed due to unchecked exception thrown in transaction");
	}

	dump(Tables.AUTHOR.getName());
}
```

在封装好的事务代码中，如果正常运行无异常，则事务被顺利提交；如果抛出受检异常 (Checked Exception) 则事务回滚，且 `org.jooq.DSLContext#transaction(org.jooq.TransactionalRunnable)` 方法抛出 `org.jooq.exception.DataAccessException`；如果抛出非受检异常 (Unchecked Exception) 则事务回滚，且该异常被 `org.jooq.DSLContext#transaction(org.jooq.TransactionalRunnable)` 方法原样抛出。

如果在事务代码中需要使用 `org.jooq.DSLContext` 实例执行 [jOOQ][jooq] 代码，需要重新构造一个实例而不能使用外部的实例 (示例代码中使用了 `inner` 而不是 `dsl`) 。

测试使用的参数化数据从静态方法 `runnables` 的返回值中获取，如下：

```java
private static Long throwsNothing(final AuthorRecord inserted) throws Exception {
	System.out.println("Throws Nothing when processing " + inserted.getId());
	return inserted.getId();
}

private static Long throwsCheckedException(final AuthorRecord inserted) throws Exception {
	System.out.println("Throws CheckedException when processing " + inserted.getId());
	throw new Exception("Failed in processing " + inserted.getId());
}

private static Long throwsUncheckedException(final AuthorRecord inserted) throws Exception {
	System.out.println("Throws UncheckedException when processing " + inserted.getId());
	throw new RuntimeException("Failed in processing " + inserted.getId());
}

static Stream<Arguments> runnables() {
	return Stream.<ThrowingConsumer>of(
		TransactionIT::throwsNothing,
		TransactionIT::throwsCheckedException,
		TransactionIT::throwsUncheckedException
	).map(Arguments::of);
}
```

# TransactionalCallable

如果封装的事务代码需要返回值，可以使用 `org.jooq.TransactionalCallable<T>` 接口封装需要在事务中执行的代码；封装好的代码需要提交给 `org.jooq.DSLContext#transactionResult(org.jooq.TransactionalCallable<T>)` 方法执行。

示例代码如下：

```java
@ParameterizedTest
@MethodSource("callables")
void test_transactionCall(final ThrowingFunction<Long> callable) {
	try {
		final Long result = dsl.transactionResult(c -> {
			final DSLContext inner = DSL.using(c);

			final AuthorRecord inserted = inner.insertInto(Tables.AUTHOR)
				.columns(Tables.AUTHOR.FIRST_NAME, Tables.AUTHOR.LAST_NAME)
				.values("Elvis", "Wang")
				.returning(Tables.AUTHOR.ID)
				.fetchOne();

			// assertj
			Assertions.assertThat(inserted).isNotNull();

			final Long val = callable.call(inserted);

			// assertj
			Assertions.assertThat(val).isNotNull();

			final int n = inner.update(Tables.AUTHOR)
				.set(Tables.AUTHOR.FIRST_NAME, "James")
				.set(Tables.AUTHOR.LAST_NAME, "Zhang")
				.where(Tables.AUTHOR.ID.eq(inserted.getId()))
				.execute();

			// assertj
			Assertions.assertThat(n).isEqualTo(1);

			return val;
		});

		// assertj
		Assertions.assertThat(result).isNotNull();
	} catch (DataAccessException ex) {
		ex.printStackTrace();
		System.out.println("Failed due to checked exception thrown in transaction");
	} catch (RuntimeException ex) {
		ex.printStackTrace();
		System.out.println("Failed due to unchecked exception thrown in transaction");
	}

	dump(Tables.AUTHOR.getName());
}
```

在封装好的事务代码中，如果正常运行无异常，则事务被顺利提交；如果抛出受检异常 (Checked Exception) 则事务回滚，且 `org.jooq.DSLContext#transactionResult(org.jooq.TransactionalCallable<T>)` 方法抛出 `org.jooq.exception.DataAccessException`；如果抛出非受检异常 (Unchecked Exception) 则事务回滚，且该异常被 `org.jooq.DSLContext#transactionResult(org.jooq.TransactionalCallable<T>)` 方法原样抛出。

如果在事务代码中需要使用 `org.jooq.DSLContext` 实例执行 [jOOQ][jooq] 代码，需要重新构造一个实例而不能使用外部的实例 (示例代码中使用了 `inner` 而不是 `dsl`) 。

测试使用的参数化数据从静态方法 `runnables` 的返回值中获取，如下：

```java
private static Long throwsNothing(final AuthorRecord inserted) throws Exception {
	System.out.println("Throws Nothing when processing " + inserted.getId());
	return inserted.getId();
}

private static Long throwsCheckedException(final AuthorRecord inserted) throws Exception {
	System.out.println("Throws CheckedException when processing " + inserted.getId());
	throw new Exception("Failed in processing " + inserted.getId());
}

private static Long throwsUncheckedException(final AuthorRecord inserted) throws Exception {
	System.out.println("Throws UncheckedException when processing " + inserted.getId());
	throw new RuntimeException("Failed in processing " + inserted.getId());
}

static Stream<Arguments> callables() {
	return Stream.<ThrowingFunction<Long>>of(
		TransactionIT::throwsNothing,
		TransactionIT::throwsCheckedException,
		TransactionIT::throwsUncheckedException
	).map(Arguments::of);
}
```

# TransactionProvider

[jOOQ][jooq] 通过 `org.jooq.TransactionProvider` 接口提供了事务实现的自定义支持：

```java
public interface TransactionProvider {
    void begin(TransactionContext ctx) throws DataAccessException;

    void commit(TransactionContext ctx) throws DataAccessException;

    void rollback(TransactionContext ctx) throws DataAccessException;
}
```

自定义的 TransactionProvider 实现可以在构造 `org.jooq.DSLContext` 的时候指定；如果不指定的话，默认使用基于 `java.sql.Savepoint` 的实现。

[官方文档](https://www.jooq.org/doc/3.12/manual-single-page/#transaction-management) 提供了一个使用 [Spring](https://spring.io/projects/spring-framework) 的 [`DataSourceTransactionManager`](https://docs.spring.io/spring-framework/docs/5.1.3.RELEASE/javadoc-api/org/springframework/jdbc/datasource/DataSourceTransactionManager.html) 自定义实现 TransactionProvider 的例子。

---

完整的示例代码可以参见 [jOOQ Usecases](https://github.com/wbprime/java-mods/tree/master/jooq-usecases) 。

---

[jOOQ][jooq] 提供了非声明式的事务支持，详见 [官方文档](https://www.jooq.org/doc/3.12/manual-single-page/#transaction-management) 。

以上。

[jooq]: https://www.jooq.org/ "jOOQ generates Java code from your database and lets you build type safe SQL queries through its fluent API."
[documentation]: https://www.jooq.org/learn/ "jOOQ Documentation"
[sql99]: https://en.wikipedia.org/wiki/SQL:1999 "SQL:1999"
[h2]: http://www.h2database.com/html/main.html "H2 Database Engine"
[hsqldb]: http://hsqldb.org "HSQLDB - 100% Java Database"
[mysql]: http://www.mysql.com "The world's most popular open source database"
[postgresql]: https://www.postgresql.org "PostgreSQL: The World's Most Advanced Open Source Relational Database"
