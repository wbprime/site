+++
title = "一次由于误用 HashMap 值的 null 特性的 BUG"
description = "最近又经历了一次线上服务的 BUG，原因是服务内部的代码逻辑问题，比较简单。在此记录下来是为了提醒不要再犯。"
date = 2019-03-21T10:04:03+08:00
draft = false
template = "page.html"
[taxonomies]
categories =  ["Practise"]
tags = ["bug", "HashMap", "null", "nullability"]
+++

最近又经历了一次线上服务的 BUG，原因是服务内部的代码逻辑问题，比较简单。

在此记录下来是为了提醒不要再犯。

<!-- more -->

# 背景

服务中处理对用户信息的增删改查的请求。在对外的修改接口中，使用一个 HashMap 来记录需要修改的用户信息
的类型：如要修改用户的邮箱和手机号，则会传进来一个有两个 k-v 记录的 HashMap。服务会遍历该 HashMap 的记录，
通过拼接字符串的方式生成一条修改用户信息的 SQL，再通过 JDBC 的 PreparedStatement 来进行实际的修改。

这些逻辑都是上古时代遗留下来的代码实现，属于不可知领域。项目组没有魄力也没有时间精力去重构代码，导致
会有一些隐藏的 BUG，平时不出问题，当你需要去打补丁加需求的时候冷不丁地给你来一下子。

# BUG 说明

如前所述，当调用方需要修改用户信息时，会传入一个 HashMap，里面是需要修改的用户的信息，还有一个是用户
的 UID。

在处理业务线的一个新需求时，需要对修改用户的逻辑进行变动。在 REVIEW 老代码的时候，发现有一个地方可以
进行优化：即如果修改用户邮箱时，因为有用户邮箱唯一性的限定，可以当用户重复绑定一个相同邮箱的时候可以
跳过处理。

旧逻辑如下：

![old updating email](update_email.old.svg)

当通过邮箱找到一个已存在的用户后，如果该用户是本次需要修改的用户（即重复修改）则认为不会破坏邮箱的唯
一性约束，反之则说明要修改成为的邮箱已经被别的用户占用了，不能修改。

考虑到用户信息是分表存储的，如果一次修改用户的请求里面包含的用户字段除了邮箱之外还有别的字段，且别的
字段都与邮箱不是在同一个表里面，则旧的处理流程可以进行优化：当确定是重复修改邮箱之后，可以把邮箱字段
从 HashMap 中删去并下传到后续的处理中。

![new updating email](update_email.new.svg)

**BUG**

问题在于，遗留代码中对于 HashMap 是做了一层封装的。要想从 HashMap 里面删除字段，只能使用
`HashMap.put(key, null)` 的方式；而在实际处理修改请求生成 SQL 时是使用的遍历，而且（自作聪明地）对
null 值进行了处理，null 值被转换为了空字符串。这样删除邮箱字段的意图，就变成了设置邮箱为空字符串。

所以最后的结果是，当用户修改邮箱的时候没有问题，但是修改相同邮箱时就会清空掉自己的邮箱。

# 代码 REVIEW

代码在组内进行了 REVIEW。

但是可能是由于大家伙的项目比较多，压力大，所以 REVIEW 的效果不是很好。

# 测试

由于老服务没有规范的质量监管流程，也没有 QA 进行服务测试。在开发完成上线之前，有开发人员做了一部分的
功能测试。对于用户的修改逻辑是进行了测试的，但平良心说，没有考虑到重复修改相同邮箱的 CASE，所有没有
对该种情况进行测试覆盖。

# 反思

## 代码 REVIEW

代码 REVIEW 很重要，但是不能流于形式。

切记切记。

## 测试

老服务的测试一般都很麻烦。各个类各个静态块静态方法相互关联，你中有我我中有你，单元测试基本没有办法写；集成测试环境不完备，各种约束条件需要处理，有的很难绕过。

但是测试很重要，一定要提高覆盖率。

而且，对于老服务而言，修改所涉及的分支路径在上线之前一定要测试测试测试。

切记切记。

## Non-null vs Null

<blockquote class="blockquote-center">
I call it my billion-dollar mistake. It was the invention of the null reference in 1965.
</blockquote>

代码中的 `null`，图灵得主 Tony Hoare 称之为 *十亿美元的错误* ( *Null References: The Billion Dollar Mistake* )。

对于代码中出现的引用，我的风格是全部默认为 non-null 的，如果能处理 null 的参数，则显式在参数前添加标
记，如果会产生 null 的结构，也需要标记。

标记注解可以使用 com.google.code.findbugs:jsr305 里面的注解：

- `javax.annotation.Nonnull`
- `javax.annotation.Nullable`

对于能够接受 null 值的 Map 实现（`java.util.HashMap` `java.util.TreeMap` 等），在处理 Map#get 的返回值时
，一定要先处理 null 的情况；如果条件允许，就使用不接受 null 值的 Map 实现（
`java.util.concurrent.ConcurrentHashMap` 等或者 [Google Guava] 的
`com.google.common.collect.ImmutableMap` 等）。

对于 null 值使用，在代码中可以使用 Optional 的模式进行替换。具体到 JDK 8 中是 `java.util.Optional` 类；JDK 8
之前可以考虑使用 [Guava Optional]。

[Google Guava]: https://github.com/google/guava
[Guava Optional]: https://google.github.io/guava/releases/snapshot-jre/api/docs/
