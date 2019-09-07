+++
title = "Multiple JWT Auth Handlers in Vert.x"
description = "在实际的项目中，遇到了一个支持多用户提供方的需求：项目的用户是从多个其他项目导入的；项目的逻辑比较简单，不想维护自己的用户信息数据和外部映射。比如，项目需要支持主站用户、视频网站等入口登录访问。正常情况下，只需要项目维护一套内部用户表、一个外部项目表以及内部用户和外部用户的映射表；在用户导入或用户绑定请求时，建立外部项目 ID 和外部项目用户 ID 与内部用户 ID 的对应关系；在登录请求时，根据外部项目 ID 和外部项目用户 ID 调用用户认证回调，通过后再寻找到内部用户的 ID 即可。实际在项目的设计和实现过程中，采用了一个比较好玩的方法：在内部用户表的基础上，对用户使用 JWT 的认证模型；用户登录时，根据外部项目 ID 和外部项目用户 ID 调用用户认证回调，发放 JWT token，在 token 中限定用户的访问权限。"
date = 2019-03-12T21:19:30+08:00
draft = false
template = "page.html"
[taxonomies]
categories =  ["Vertx"]
tags = ["vert.x", "jwt", "auth"]
+++

[Vert.x](https://vertx.io) 的官方 Web 开发包 [Vert.x-Web](https://vertx.io/docs/vertx-web/java/) 中
提供了内置的 Authentication&Authorisation 支持；通过扩展的
[Auth Common](https://vertx.io/docs/vertx-auth-common/java/) 模块和 [JDBC
auth](https://vertx.io/docs/vertx-auth-jdbc/java/) [MongoDB
auth](https://vertx.io/docs/vertx-auth-mongo/java/) [Shiro
auth](https://vertx.io/docs/vertx-auth-shiro/java/) [JWT
auth](https://vertx.io/docs/vertx-auth-jwt/java/) [OAuth 2](https://vertx.io/docs/vertx-auth-oauth2/java/) 等模块，可以覆盖大部分的用户认证与验权的支持。

在实际的项目中，遇到了一个支持多用户提供方的需求：项目的用户是从多个其他项目导入的；项目的逻辑比较简
单，不想维护自己的用户信息数据和外部映射。

比如，项目需要支持主站用户、视频网站等入口登录访问。正常情况下，只需要项目维护一套内部用户表、一个外
部项目表以及内部用户和外部用户的映射表；在用户导入或用户绑定请求时，建立外部项目 ID 和外部项目用户
ID 与内部用户 ID 的对应关系；在登录请求时，根据外部项目 ID 和外部项目用户 ID 调用用户认证回调，通过
后再寻找到内部用户的 ID 即可。

实际在项目的设计和实现过程中，采用了一个比较好玩的方法：在内部用户表的基础上，对用户使用
[JWT](https://jwt.io) 的认证模型；用户登录时，根据外部项目 ID 和外部项目用户 ID 调用用户认证回调，发
放 JWT token，在 token 中限定用户的访问权限。

<!-- more -->

# Vert.x 对 JWT 的支持

相关文档参见 [Vert.x Auth Common](https://vertx.io/docs/vertx-auth-common/java/) [Vert.x
JWT](https://vertx.io/docs/vertx-auth-jwt/java/) [Vert.x Web AuthN &
AuthZ](https://vertx.io/docs/vertx-web/java/#_authentication_authorisation) 。

1. 确定 JWT 的算法和密钥

```java
WTAuthOptions authConfig = new JWTAuthOptions()
    .setKeyStore(new KeyStoreOptions()
    .setType("jceks")
    .setPath("keystore.jceks")
    .setPassword("secret"));

JWTAuth authProvider = JWTAuth.create(vertx, authConfig);
```

JWTAuth 是 Vert.x 的 AuthProvider 的一个实现；AuthProvider 主要提供根据一个 JSON 的用户信息对象进行用户认证的能力；JWTAuth 则通过校验一个 JWT 型的 JSON token 来验权，同时扩展加上了生成 JWT token 的方法。

JWT 算法和结构相关参见 [JWT](https://jwt.io) 。

2. 生成并发放 Token

这一步一般在登录时进行。

```
router.route("/login").handler(this::login);

private void login(final RoutingContext ctx) {
  if ("paulo".equals(ctx.request().getParam("username")) && "secret".equals(ctx.request().getParam("password"))) {
    ctx.response().end(authProvider.generateToken(new JsonObject().put("sub", "paulo"), new JWTOptions()));
  } else {
    ctx.fail(401);
  }
}
```

3. 验证 Token

这一步一般以拦截器的形式实现，用于在访问需要权限控制的接口时，提前通过 JWT 验权来确定是否放行。

```
router.route().handler(JWTAuthHandler.create(authProvider));

router.get().handler(this::findById);
router.post().handler(this::add);

private void findById(final RoutingContext ctx) {
	final User user = ctx.getUser();
	if (null != user) {
		// Authenticated
		// TODO
	}
}
private void add(final RoutingContext ctx) {
	final User user = ctx.getUser();
	if (null != user) {
		// Authenticated
		// TODO
	}
}
```

# 基于通用流程的改造

因为需要增加对多用户源的支持，所以需要扩充实现 JWT 验证的流程，使得能够：1. 不同用户源的用户需要使用不同的密钥和有效期等基本配置；2. 不同数据源的用户的登录接口参数可以不一样（如用户源 A 通过 username/password，用户源 B 通过 uid/token）

最主要的思路是把各个用户源不同的逻辑抽象出来，包括用户管理、JWT 密钥管理、用户认证、用户授权等；扩展
官方的 JWT Auth Provider，提供多源的分发验证。

## 用户源的抽象 UserRealm

```java
public interface RxUserRealm {
	Set<String> supportedRealms();

    Single<Boolean> isUserAvailable(String uid);

    Single<String> getJwtSecret();

    Single<JwtTokenDto> authenticate(final LoginDto login);

    Single<Boolean> authorize(final PrincipalDto pricipal, final PermissionDto permission);
}
```

 其中，supportedRealms 说明自身的用户源集合；isUserAvailable 提供根据用户ID或用户名查询用户是否存在的功能；getJwtSecret 提供查询用户源相关的 JWT 密钥的功能；authenticate & authorize 提供各用户源的用户认证和授权管理的功能。

接口的返回使用 [RxJava 2](https://github.com/ReactiveX/RxJava) 的类型，主要原因是这些接口可以是远程调用的，可以利用RxJava 的异步响应机制来封装差异。

作为示例，服务提供了一个 DemoUserRealm，用以提供无数据源用户体验服务的能力。

```java
@AutoService(RxUserRealm.class)
public class DemoUserRealm implements RxUserRealm {
	private static final String UID_PREFIX = "demo";
	private static final String JWT_SECRET = "demo";

    @Override
    public Set<String> supportedRealms() {
        return ImmutableSet.of("demo");
    }

	@Override
	public Single<Boolean> isUserAvailable(final String uid) {
		return Single.just(uid.startsWith(UID_PREFIX));
	}

	@Override
	public Single<String> getJwtSecret() {
		return Single.just(JWT_SECRET);
	}

	@Override
	public Single<JwtTokenDto> authenticate(final LoginDto login) {
		final String pw = login.claims().get("password");

		// Omitted

		return Single.just(JwtTokenDto.create()); // Omitted
	}

	@Override
	public Single<Boolean> authorize(final PrincipalDto principal, final PermissionDto permission) {
		switch (permission.category()) {
			case READ:
				return Single.just(Boolean.TRUE);
			case WRITE:
				return Single.just(Boolean.FALSE);
			default:
				return Single.just(Boolean.FALSE);
		}
	}
}
```

`@AutoService` 注解是 Google Auto-Service 包的一部分，用来辅助实现 Java 基于 `java.util.ServiceLoader` 的 SPI 机制。

## SpiBasedUserRealmService

为了提高扩展性，可以使用 Java ServiceLoader (`java.util.ServiceLoader`) 来进行 UserRealm 的管理。

```java
class SpiBasedUserRealmService {
	private Map<String, RxUserRealm> mappedRealms;

	SpiBasedUserRealmService() {
		final Map<String, RxUserRealm> map = new HashMap();

		final ServiceLoader<RxUserRealm> realms = ServiceLoader.load(RxUserRealm.class);
		for (final RxUserRealm r: realms) {
			final Set<String> types = r.supportedRealms();
			if (types.isEmpty()) continue;

			for (final String type: types) {
				if (map.containsKey(type)) continue;

				map.put(type, r);
			}
		}

		// Guava utils
		mappedRealms = ImmutableMap.copyOf(map);
	}

	public Optional<RxUserRealm> findRealm(final String realm) {
		return Optional.ofNullable(mappedRealms.get(type));
	}
}
```

## JWT Auth Provider

主要的逻辑在 `CustomJwtAuthProvider` 中。该类实现 Vert.x 内置的 JWTAuth 接口，以能够和 vert.x-web 模块无缝结合。

在 authenticate 的实现中，首先对 JWT 的 token 串进行只解码不验证，从解码出的 JSON 中可以获得对应的用户源类型，解码 JWT token 可以使用[这个](https://github.com/auth0/java-jwt)；可以通过用户源类型找到可用的 RxUserRealm 实例，查询对应的 JWT 配置；之后再使用配置创建原生的 JWTAuth 实例进行 authenticate。

在 generateToken 的实现中，首先根据用户源类型查询到可用的 RxUserRealm 实例，然后使用该实例的 JWT 配置创建原生的 JWTAuth 实例进行 generateToken。

```java
class CustomJwtAuthProvider implements JWTAuth {
    private final Scheduler workingScheduler;

    private final Vertx vertx;

    private final SpiBasedUserRealmService realmService;

    // ...
    // init code omited
    // ...

    @Override
    public void authenticate(final JsonObject authInfo, final Handler<AsyncResult<User>> resultHandler) {
        final JWT decode;
        try {
            final String jwtStr = authInfo.getString("jwt");
            decode = JWT.decode(jwtStr);
        } catch (RuntimeException ex) {
            resultHandler.handle(Future.failedFuture(ex));
            return;
        }

        final String realm = firstAudience(decode);
        authProviderByRealm(Strings.nullToEmpty(realm))
            .subscribeOn(workingScheduler)
            .subscribe(
                jwt -> jwt.authenticate(authInfo, re -> {
                    if (re.failed()) {
                        LOG.warn("JWT auth failed for realm \"{}\"", realm, re.cause());
                    } else {
                        LOG.debug("JWT auth succeed for realm \"{}\"", realm);
                    }
                    resultHandler.handle(re);
                }),
                ex -> {
                    LOG.warn("JWT auth exception for realm \"{}\"", realm, ex);
                    resultHandler.handle(Future.failedFuture(ex));
                }
            );
    }

    private String firstAudience(final Payload payload) {
        final List<String> audience = payload.getAudience();
        return (null != audience && ! audience.isEmpty()) ? audience.get(0) : "";
    }

    @Override
    public String generateToken(final JsonObject claims, final JWTOptions options) {
        final String realm = claims.getString("aud");
        return authProviderByRealm(Strings.nullToEmpty(realm))
            .map(jwt -> jwt.generateToken(claims, options))
            .blockingGet();
    }

    private Single<JWTAuth> authProviderByRealm(final String realm) {
		final Optional<RxUserRealm> opt = realmService.findRealm(realm);
		if (! opt.isPresent())
			return Single.error(new IllegalStateException("Realm not supported: " + realm));

        return opt.get().findJwtSecret(realm).map(this::jwtAuth);
    }

    private JWTAuth jwtAuth(final String key) {
        return JWTAuth.create(vertx, new JWTAuthOptions()
            .addPubSecKey(new PubSecKeyOptions()
                .setAlgorithm("HS256")
                .setPublicKey(key)
                .setSymmetric(true)
            )
        );
    }
}
```

## Auth Handler

对于需要用户认证和验权保护的接口，正常使用 vert.x-web 模块提供的 JWTAuthHandler 机制。

```java
// ...
// omited
final JWTAuth jwtAuth = new CustomJwtAuthProvider();
router.route().handler(JWTAuthHandler.create(jwtAuth);
// omited
// ...
```

## Login Controller

最后一步是在 HTTP 的 handler 里面使用 CustomJwtAuthProvider 生成 JWT token 并返回给调用方使用。

```java
// ...
// omited
router.post("/login").handler(this::login);
// omited
// ...

JWTAuth jwtAuth; // formerly inited
private void login(final RoutingContext ctx) {
	// Generate JWT token
	// final String token = jwtAuth.generateToken(...);
	final String token = "generated token";
	ctx.response().end(token);
}
```

# 总结

整个设计的基本思路就是基于内置的 JWTAuth 实现类
(io.vertx.ext.auth.jwt.impl.JWTAuthProviderImpl)，在 authenticate & generateToken 的实现中从参数中取
出用户源类型，再根据用户源类型执行各自的逻辑，之后再调用 JWTAuthProviderImpl 的实现。这样在 Vert.x
的框架范围之内做最小的改动实现了所需要的功能。不同的用户源的实现和管理可以使用不同的方式实现，我在项
目中使用的是 Java 的 SPI 服务发现机制；如果有必要，可以在项目中引入依赖注入框架（如 [Guice](https://github.com/google/guice) [Spring](https://spring.io/projects/spring-framework) 等）管理用户源逻辑的实现。
