+++
title = "JOOQ 的使用 - 代码生成配置 (PostgreSQL & DDL)"
description = "jOOQ 是基于 JDBC 之上的一个抽象层，提供了多种多样的模型来与关系型数据库进行互操作；其使用与 mybatis 和 Hibernate ORM 不同的思路来实现对象关系映射(ORM) 。本篇主要介绍基于 jOOQ 的数据库实例和 SQL DDL 文件驱动的代码生成实践。"
date = 2019-10-21T11:04:07+08:00
draft = false
template = "page.html"
[taxonomies]
categories =  ["Java"]
tags = ["jdbc", "sql", "jooq"]
+++

[jOOQ][jooq] 是基于 JDBC 之上的一个抽象层，提供了多种多样的模型来与关系型数据库进行互操作；其使用与 [mybatis](https://mybatis.org/mybatis-3/) 和 [Hibernate ORM](http://hibernate.org/orm/) 不同的思路来实现 [对象关系映射 ORM](https://en.wikipedia.org/wiki/Object-relational_mapping) 。

[jOOQ][jooq] 的一个最主要的特性就是基于代码生成的类型安全 SQL。其主要是从已有的数据库配置信息中收集
到足够的表结构、字段结构和索引等信息，生成对应的 Java 类型；业务使用生成的 Java 类型来进行 SQL 操作
，可以得到足够的类型安全保证。同时，由于代码生成避免了基于反射的对象关系映射，在调试时会有更好的表现
。

截止到版本 `3.12.1`, [jOOQ][jooq] 支持从以下数据库配置源生成代码：

1. 关系数据库实例，包括(注：免费版本的 jOOQ 可能不支持特定的数据库提供商的产品)：
    - Aurora MySQL Edition
    - Aurora PostgreSQL Edition
    - Azure SQL Data Warehouse
    - Azure SQL Database
    - CUBRID
    - DB2 LUW
    - Derby
    - Firebird
    - H2
    - HANA
    - HSQLDB
    - Informix
    - Ingres
    - MariaDB
    - Microsoft Access
    - MySQL
    - Oracle
    - PostgreSQL
    - Redshift
    - SQL Server
    - SQLite
    - Sybase Adaptive Server Enterprise
    - Sybase SQL Anywhere
    - Teradata
    - Vertica
2. [Java Persistence API (JPA)](https://en.wikipedia.org/wiki/Java_Persistence_API) 相关的实体类型（带注解）
3. 符合约定条件的描述表结构信息等的 XML 文件
4. 符合标准的用于创建表结构等的 SQL DDL 语句文件

本篇主要介绍基于 [jOOQ][jooq] 的数据库实例和 SQL DDL 文件驱动的代码生成实践。

<!-- more -->

# Maven 配置

在 [Maven](https://maven.apache.org/) 中使用 [jOOQ][jooq] 的代码生成功能，需要在 `pom.xml` 文件中添加依赖和插件的配置：

```xml
<dependencies>
    <!-- omitted -->
    <dependency>
        <groupId>org.jooq</groupId>
        <artifactId>jooq</artifactId>
        <version>${jooq_version}</version>
    </dependency>
    <!-- omitted -->
</dependencies>
<build>
    <plugins>
        <plugin>
            <groupId>org.jooq</groupId>
            <artifactId>jooq-codegen-maven</artifactId>
            <version>${jooq_version}</version>

            <!--
            <executions>
                <execution>
                    <id>jooq-generating</id>
                    <phase>generate-sources</phase>
                    <goals>
                        <goal>generate</goal>
                    </goals>
                </execution>
            </executions>
            -->

            <dependencies>
                <dependency>
                    <groupId>org.jooq</groupId>
                    <artifactId>jooq-meta</artifactId>
                    <version>${jooq_version}</version>
                </dependency>
                <dependency>
                    <groupId>org.jooq</groupId>
                    <artifactId>jooq-meta-extensions</artifactId>
                    <version>${jooq_version}</version>
                </dependency>
                <dependency>
                    <groupId>org.jooq</groupId>
                    <artifactId>jooq-codegen</artifactId>
                    <version>${jooq_version}</version>
                </dependency>
            </dependencies>
            <configuration>
                <configurationFile>src/main/resources/jooq.xml</configurationFile>
            </configuration>
        </plugin>
    </plugins>
</build>
```

在 `pom.xml` 所在目录执行 `mvn jooq-codegen:generate` 即可生成所有需要的类文件。如果需要每次执行 `mvn install` 时自动执行，可以将 `jooq-codegen:generate` 绑定到 `generate-sources` 阶段。

截至 2019-10-21，[jOOQ][jooq] 的最新版本是 `3.12.1` 。

`src/main/resources/jooq.xml` 指向 [jOOQ][jooq] 代码生成的配置文件。[jOOQ][jooq] 代码生成插件需要引用一个 XML 格式的配置文件，该配置文件中包含了用于获取数据库数据表结构等元数据的必要信息，配置项细则见 <http://www.jooq.org/xsd/jooq-codegen-3.12.0.xsd> 。

更多信息请参考 [jOOQ][jooq] 代码生成部分的[文档](https://www.jooq.org/doc/3.12/manual-single-page/#code-generation)。

# PostgreSQL 数据库实例驱动代码生成

## 数据库表结构

数据库建表 SQL 语句如下：

```sql
CREATE TABLE author (
    id              BIGSERIAL     NOT NULL PRIMARY KEY,
    first_name      VARCHAR(50)   NOT NULL,
    last_name       VARCHAR(50)   NOT NULL,
    date_of_birth   DATE,
    year_of_birth   BIGINT
);

CREATE TABLE book (
    id              BIGSERIAL    NOT NULL PRIMARY KEY,
    author_id       BIGINT       NOT NULL,
    title           VARCHAR(400) NOT NULL
);
```

## Maven 配置

[jOOQ][jooq] 的代码生成插件需要使用 [PostgreSQL][postgresql] 的 JDBC 支持。在 `pom.xml` 中修改如下：

```xml
<plugin>
    <groupId>org.jooq</groupId>
    <artifactId>jooq-codegen-maven</artifactId>
    <version>${jooq_version}</version>

    <dependencies>
        <dependency>
            <groupId>org.jooq</groupId>
            <artifactId>jooq-meta</artifactId>
            <version>${jooq_version}</version>
        </dependency>
        <dependency>
            <groupId>org.jooq</groupId>
            <artifactId>jooq-meta-extensions</artifactId>
            <version>${jooq_version}</version>
        </dependency>
        <dependency>
            <groupId>org.jooq</groupId>
            <artifactId>jooq-codegen</artifactId>
            <version>${jooq_version}</version>
        </dependency>
        <!-- for postgresql -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>${postgresql_versoin}</version>
        </dependency>
    </dependencies>
    <configuration>
        <configurationFile>src/main/resources/jooq-postgresql.xml</configurationFile>
    </configuration>
</plugin>
```

示例中使用的 [PostgreSQL][postgresql] 的 JDBC 版本为 `42.2.8` 。

## 配置文件

所引用的 [jOOQ][jooq] 代码生成插件配置文件 `jooq-postgresql.xml` 内容如下：

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<configuration xmlns="http://www.jooq.org/xsd/jooq-codegen-3.12.0.xsd">
    <jdbc>
        <driver>org.postgresql.Driver</driver>
        <url>jdbc:postgresql:jooq</url>
        <user>jooq</user>
        <password>jOOQ@postgresql</password>
    </jdbc>
    <generator>
        <database>
            <name>org.jooq.meta.postgres.PostgresDatabase</name>
            <includes>.*</includes>
            <inputSchema>public</inputSchema>
        </database>
        <generate>
            <validationAnnotations>false</validationAnnotations>

            <javaTimeTypes>true</javaTimeTypes>

            <pojos>true</pojos>
            <pojosEqualsAndHashCode>true</pojosEqualsAndHashCode>
            <pojosToString>true</pojosToString>
            <immutablePojos>false</immutablePojos>
            <serializablePojos>false</serializablePojos>

            <interfaces>true</interfaces>
            <serializableInterfaces>false</serializableInterfaces>

            <daos>true</daos>
        </generate>
        <target>
            <packageName>im.wangbo.java.usecases.generated.postgresql</packageName>
            <directory>src/main/java</directory>
            <encoding>UTF-8</encoding>
        </target>
    </generator>
</configuration>
```

配置文件需要指定目标数据库的类型为 `org.postgresql.Driver`，同时指定到开发环境数据库实例连接的 URL、用户名、密码等信息。由于用户名和密码信息被明文保存在代码仓库中，所以在配置代码生成的时候千万不要使用线上环境或其他敏感数据库的配置信息，可以使用开发环境的配置或者使用从 Docker/Podman 部署的相同配置的数据库实例配置；也可以使用下面介绍的从建表语句 SQL DDL 文件驱动的代码生成。

`<database>` 节点配置了需要包含的数据表名等；`<generate>` 节点配置了如何生成代码等；`<target>` 节点配置了生成代码的输出路径等。

更详细的配置说明可以参见 [官方文档](https://www.jooq.org/doc/3.12/manual-single-page/#code-generation) 。

## 生成的类

运行 `mvn clean jooq-codegen:generate` 生成的类文件如下：

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

详细的生成结果可以参见 [jOOQ Usecases](https://github.com/wbprime/java-mods/tree/master/jooq-usecases) 。

# 从建表 SQL DDL 语句文件生成代码

## Maven 配置

此时只需要在 `pom.xml` 中指定 [jOOQ][jooq] 代码生成插件配置文件的路径即可，如下：

```xml
<plugin>
    <groupId>org.jooq</groupId>
    <artifactId>jooq-codegen-maven</artifactId>
    <version>${jooq_version}</version>

    <dependencies>
        <dependency>
            <groupId>org.jooq</groupId>
            <artifactId>jooq-meta</artifactId>
            <version>${jooq_version}</version>
        </dependency>
        <dependency>
            <groupId>org.jooq</groupId>
            <artifactId>jooq-meta-extensions</artifactId>
            <version>${jooq_version}</version>
        </dependency>
        <dependency>
            <groupId>org.jooq</groupId>
            <artifactId>jooq-codegen</artifactId>
            <version>${jooq_version}</version>
        </dependency>
    </dependencies>
    <configuration>
        <configurationFile>src/main/resources/jooq-ddl.xml</configurationFile>
    </configuration>
</plugin>
```

示例中使用的 [PostgreSQL][postgresql] 的 JDBC 版本为 `42.2.8` 。

## 配置文件

所引用的 [jOOQ][jooq] 代码生成插件配置文件 `jooq-ddl.xml` 内容如下：

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<configuration xmlns="http://www.jooq.org/xsd/jooq-codegen-3.12.0.xsd">
    <generator>
        <database>
            <name>org.jooq.meta.extensions.ddl.DDLDatabase</name>
            <properties>
                <property>
                    <key>scripts</key>
                    <value>src/main/resources/database.sql</value>
                </property>
                <property>
                    <key>sort</key>
                    <value>semantic</value>
                </property>
                <property>
                    <key>unqualifiedSchema</key>
                    <value>public</value>
                </property>
            </properties>
        </database>
        <generate>
            <validationAnnotations>false</validationAnnotations>

            <javaTimeTypes>true</javaTimeTypes>

            <pojos>true</pojos>
            <pojosEqualsAndHashCode>true</pojosEqualsAndHashCode>
            <pojosToString>true</pojosToString>
            <immutablePojos>false</immutablePojos>
            <serializablePojos>false</serializablePojos>

            <interfaces>true</interfaces>
            <serializableInterfaces>false</serializableInterfaces>

            <daos>true</daos>
        </generate>
        <target>
            <packageName>im.wangbo.java.usecases.generated.ddl</packageName>
            <directory>src/main/java</directory>
            <encoding>UTF-8</encoding>
        </target>
    </generator>
</configuration>
```

## 生成的类

运行 `mvn clean jooq-codegen:generate` 生成的类文件如下：

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
└── Tables.java
```

详细的生成结果可以参见 [jOOQ Usecases](https://github.com/wbprime/java-mods/tree/master/jooq-usecases) 。

# 生成类文件的说明

## 接口类

接口类的生成是可选的。如果在 [jOOQ][jooq] 代码生成插件配置文件的 `<generate>` 节点中开启了生成接口类的配置，则会生成接口类。

```xml
<generate>
    <!-- more -->
    <interfaces>true</interfaces>
    <serializableInterfaces>false</serializableInterfaces>
    <!-- more -->
</generate>
```

[jOOQ][jooq] 会为每一个数据库表生成一个接口类，接口类中包含了每一个字段的 Getter/Setter 方法以及对象转换方法 `from` 和 `into` 方法。

根据前述配置为数据表 `author` 生成的接口类为 `tables/interfaces/IAuthor`，主要内容如下：

```java
public interface IAuthor {
    public void setId(Long value);
    public Long getId();

    public void setFirstName(String value);
    public String getFirstName();

    public void setLastName(String value);
    public String getLastName();

    public void setDateOfBirth(LocalDate value);
    public LocalDate getDateOfBirth();

    public void setYearOfBirth(Long value);
    public Long getYearOfBirth();

    public void from(IAuthor from);
    public <E extends IAuthor> E into(E into);
}
```

## POJO 类

POJO 类的生成是可选的。如果在 [jOOQ][jooq] 代码生成插件配置文件的 `<generate>` 节点中开启了生成 POJO 类的配置，则会生成 POJO 类。

```xml
<generate>
    <!-- more -->
    <pojos>true</pojos>
    <pojosEqualsAndHashCode>true</pojosEqualsAndHashCode>
    <pojosToString>true</pojosToString>
    <immutablePojos>false</immutablePojos>
    <serializablePojos>false</serializablePojos>
    <!-- more -->
</generate>
```

[jOOQ][jooq] 会为每一个数据库表生成一个 POJO 类，该类中包含了每一个字段的 Getter/Setter 方法、对象转换方法 `from` 和 `into` 方法。

如果配置了生成接口类，则生成的 POJO 类会实现对应的接口类。

根据前述配置为数据表 `author` 生成的 POJO 类为 `tables/pojos/Author` 。

## 表记录类 TableRecord/UpdatableRecord

[jOOQ][jooq] 会为每一个数据库表生成一个表记录类，该类是 `org.jooq.TableRecord` 或 `org.jooq.UpdatableRecord` 类的子类，其中包含每一个字段的 Getter/Setter 方法。

如果配置了生成接口类，则生成的 POJO 类会实现对应的接口类。

根据前述配置为数据表 `author` 生成的 POJO 类为 `tables/records/AuthorRecord` 。

## 表类 Table

[jOOQ][jooq] 会为每一个数据库表生成一个表类，该类是 `org.jooq.Table` 类的子类，其中包含每一个字段的引用。

根据前述配置为数据表 `author` 生成的表类为 `Author` 。

## DAO 类

DAO 类的生成是可选的。如果在 [jOOQ][jooq] 代码生成插件配置文件的 `<generate>` 节点中开启了生成 DAO 类的配置，则会生成 DAO 类。

```xml
<generate>
    <!-- more -->
    <daos>true</daos>
    <!-- more -->
</generate>
```

[jOOQ][jooq] 会为每一个数据库表生成一个 DAO 类，该类中包含 `insert`/`update`/`delete`/`fetchById` 等方法。

根据前述配置为数据表 `author` 生成的 DAO 类为 `tables/daos/AuthorDao` 。

## 静态帮助类

[jOOQ][jooq] 会生成一系列的帮助类，提供静态字段或方法可以获取可用的表、主键、索引和递增序列等引用，分别是：

- `Tables`
    ```java
    public class Tables {
        /**
         * The table <code>public.author</code>.
         */
        public static final Author AUTHOR = Author.AUTHOR;

        /**
         * The table <code>public.book</code>.
         */
        public static final Book BOOK = Book.BOOK;
    }
    ```
- `Keys`
    ```java
    public class Keys {
        // -------------------------------------------------------------------------
        // IDENTITY definitions
        // -------------------------------------------------------------------------

        public static final Identity<AuthorRecord, Long> IDENTITY_AUTHOR =
                 Identities0.IDENTITY_AUTHOR;
        public static final Identity<BookRecord, Long> IDENTITY_BOOK =
                 Identities0.IDENTITY_BOOK;

        // -------------------------------------------------------------------------
        // UNIQUE and PRIMARY KEY definitions
        // -------------------------------------------------------------------------

        public static final UniqueKey<AuthorRecord> AUTHOR_PKEY =
                 UniqueKeys0.AUTHOR_PKEY;
        public static final UniqueKey<BookRecord> BOOK_PKEY =
                 UniqueKeys0.BOOK_PKEY;

        // -------------------------------------------------------------------------
        // FOREIGN KEY definitions
        // -------------------------------------------------------------------------


        // -------------------------------------------------------------------------
        // [#1459] distribute members to avoid static initialisers > 64kb
        // -------------------------------------------------------------------------

        private static class Identities0 {
            public static Identity<AuthorRecord, Long> IDENTITY_AUTHOR =
                 Internal.createIdentity(Author.AUTHOR, Author.AUTHOR.ID);
            public static Identity<BookRecord, Long> IDENTITY_BOOK =
                 Internal.createIdentity(Book.BOOK, Book.BOOK.ID);
        }

        private static class UniqueKeys0 {
            public static final UniqueKey<AuthorRecord> AUTHOR_PKEY =
                 Internal.createUniqueKey(Author.AUTHOR, "author_pkey", Author.AUTHOR.ID);
            public static final UniqueKey<BookRecord> BOOK_PKEY =
                 Internal.createUniqueKey(Book.BOOK, "book_pkey", Book.BOOK.ID);
        }
    }
    ```
- `Indexes`
    ```java
    public class Indexes {
        // -------------------------------------------------------------------------
        // INDEX definitions
        // -------------------------------------------------------------------------

        public static final Index AUTHOR_PKEY = Indexes0.AUTHOR_PKEY;
        public static final Index BOOK_PKEY = Indexes0.BOOK_PKEY;

        // -------------------------------------------------------------------------
        // [#1459] distribute members to avoid static initialisers > 64kb
        // -------------------------------------------------------------------------

        private static class Indexes0 {
            public static Index AUTHOR_PKEY =
                 Internal.createIndex("author_pkey", Author.AUTHOR,
                        new OrderField[] { Author.AUTHOR.ID }, true);
            public static Index BOOK_PKEY =
                 Internal.createIndex("book_pkey", Book.BOOK,
                        new OrderField[] { Book.BOOK.ID }, true);
        }
    }
    ```
- `Sequences`
    ```java
    public class Sequences {
        // The sequence <code>public.author_id_seq</code>
        public static final Sequence<Long> AUTHOR_ID_SEQ =
                 new SequenceImpl<Long>("author_id_seq", Public.PUBLIC,
                        org.jooq.impl.SQLDataType.BIGINT.nullable(false));

        // The sequence <code>public.book_id_seq</code>
        public static final Sequence<Long> BOOK_ID_SEQ =
                 new SequenceImpl<Long>("book_id_seq", Public.PUBLIC,
                        org.jooq.impl.SQLDataType.BIGINT.nullable(false));
    }
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
