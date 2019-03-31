+++
title = "在 Vert.x 项目中集成使用 Guice 依赖注入"
description = "在 Vert.x 项目中集成使用 Guice 依赖注入"
date = 2019-03-31T11:19:14+08:00
draft = false
template = "page.html"
[taxonomies]
categories =  ["Vertx"]
tags = ["vert.x", "guice", "jsr303"]
+++

本文介绍了项目中在基于 [Eclipse Vert.x][vertx] 的 Verticle 中使用 [Google Guice][guice] 进行依赖注入的实践，主要的思路是在 `io.vertx.core.Verticle#start` 方法中主动创建 `com.google.inject.Injector` 注入器实例并实行注入操作，在注入操作成功结束之后再进行后续的操作。

在开发结束后的某一天，通过万能的搜索发现已经有人提供了基于 [Google Guice][guice] 的 [Eclipse Vert.x][vertx] 扩展: [Vert.x Guice Extension][vertx-guice]。[Vert.x Guice Extension][vertx-guice] 扩展了 [Vert.x][vertx] 内置的 `io.vertx.core.spi.VerticleFactory` 机制，能够使用 SPI 的方式加载需要的
Guice 依赖。

本文总结一下自己的实现思路，然后分析 [Vert.x Guice Extension][vertx-guice] 的实现细节，希望能提高自己的代码水平。

<!-- more -->

# 我的方案

我的方案很直接很粗暴：把 Verticle 的启动方法作为程序的入口，在入口方法里面创建 Guice Injector 实例并对启动的 Verticle 实例进行主动注入。

```
public class MainVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

    @Inject
    @Named(Constants.KEY_HTTP_LISTEN_HOST)
    private String listenIp;

    @Inject
    @Named(Constants.KEY_HTTP_LISTEN_PORT)
    private Integer listenPort = 8001;

    @Inject
    @RegularRouter
    private Set<Controller> regularControllers;

    @Inject
    private Set<Runnable> warmUps;

    @Override
    public void start(final Future<Void> future) throws Exception {
        final Injector injector = Guice.createInjector(new MainModule(vertx, config()));
        injector.injectMembers(this);

        final Router router = Router.router(vertx);
        router.exceptionHandler(ex -> LOGGER.warn("Uncaught exception", ex));

        regularControllers.stream().sorted().forEach(c -> {
            LOGGER.debug("Registered regular Controller \"{}\"", c);
            c.accept(router);
        });

        Completable.fromRunnable(() -> warmUps.parallelStream().forEach(Runnable::run))
            .subscribeOn(RxHelper.blockingScheduler(vertx))
            .subscribe(
                () -> {
                    LOGGER.info("Warm up finished, try to start HTTP listening on {}:{} ...", listenIp, listenPort);
                    vertx.createHttpServer()
                        .requestHandler(router::accept)
                        .listen(listenPort, listenIp, e -> {
                            if (e.succeeded()) {
                                future.complete();
                            } else {
                                future.fail(e.cause());
                            }
                        });
                },
                ex -> {
                    LOGGER.error("Warmup failed, disable HTTP listening", ex);
                    future.fail(ex);
                }
            );
    }
}
```

代码很简洁明了。

服务中需要启动一个 HttpServer 监听，监听的 host 和端口需要在服务外部通过配置的方式传进来。这些配置是通过 Guice 配置并注入的。

构造的 HttpServer 需要为不同的 Endpoint (path & method) 设置不同的响应代码 (Handler)，我将其抽象为一系列的 Controller：其实就是 JDK8 的 Consumer 接口，实现了支持为 Router 添加 Handler 的 `accept` 方法以及添加了能够实现优先级的 `order` 方法。

```java
import java.util.function.Consumer;
public interface Controller extends Consumer<Router>, Comparable<Controller> {
    default long order() {return 0L;}

    @Override
    default int compareTo(final Controller o) {
        return Long.compare(order(), o.order());
    }

	@Override
    default void accept(final Router r) {
        r.route("/").handler(ctx -> {
            final JsonObject msg = new JsonObject();
            msg.put("Hello", "World");

            ctx.response().end(msg.encode());
        });
    }
}
```

添加了 `order` 方法的 Controller 的多个实现类可以通过 [com.google.inject.extensions:guice-multibindings][guice-multibindings] 提供的 `com.google.inject.multibindings.Multibinder` 来提供注入为 `java.util.Set` 的支持。不同的 Controller 实现类可以处于多个包中，分别通过以下代码提供：

```java
{
	final Multibinder<Controller> binder =
		Multibinder.newSetBinder(binder(), Controller.class, RegularRouter.class);

	binder.addBinding().to(FastController.class).in(Scopes.SINGLETON);
	binder.addBinding().to(SlowController.class).in(Scopes.SINGLETON);
}
```

同样，可以添加对实现了 `java.lang.Runnable` 的类型注入支持，模拟生命周期管理。

最关键的依赖注入的执行代码在 `start` 方法的最开始：

```java
final Injector injector = Guice.createInjector(new MainModule(vertx, config()));
injector.injectMembers(this); // On-demand Injection via injectMembers
```

代码从依赖提供者 `MainModule` 创建注入器，成功之后再对当前的 `MainVerticle` 执行 [On-demand Injection][guice-injections]。此行代码执行之后，类中所需的依赖就应该会注入完毕。

可以看出，我对 Guice 和 Vert.x 的结合使用需要修改业务代码以插入注入代码，和项目耦合很深，不能很简单地应用到别的项目中。理想的情况下，在 Vert.x 的项目中嵌入 Guice，最好能：

1. 不侵入正常的 Verticle 代码，在正常的 Verticle 部署流程中自动创建所需的依赖并对目标 Verticle 实行依赖注入
2. 依赖的提供者 （Guice `com.google.inject.Module`) 需要能比较方便的切换
3. 不能影响 Vert.x 的异步流程，不能打破[黄金法则](https://vertx.io/docs/vertx-core/java/#_don_t_block_me)

# Vert.x Guice Extension

[Vert.x Guice Extension][vertx-guice] 是基于 `io.vertx.core.spi.VerticleFactory` 的扩展，能够无缝地接入 Vert.x 部署 Verticle 的过程中。

## UseCase

使用了 [Vert.x Guice Extension][vertx-guice] 的项目中需要添加以下依赖：

```xml
<dependency>
  <groupId>com.englishtown.vertx</groupId>
  <artifactId>vertx-guice</artifactId>
  <version>2.3.1</version>
</dependency>
```

然后正常实现所需要的 Verticle，不需要添加 Guice 相关代码（@Inject 等注解除外）。

```
public class TheVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(TheVerticle.class);

    @Inject
    @Named(Constants.KEY_HTTP_LISTEN_HOST)
    private String listenIp;

    @Inject
    @Named(Constants.KEY_HTTP_LISTEN_PORT)
    private Integer listenPort = 8001;

    @Inject
    @RegularRouter
    private Set<Controller> regularControllers;

    @Inject
    private Set<Runnable> warmUps;

    @Override
    public void start(final Future<Void> future) throws Exception {
		// Not needed
        // final Injector injector = Guice.createInjector(new TheModule(vertx, config()));
        // injector.injectMembers(this);

        final Router router = Router.router(vertx);
        router.exceptionHandler(ex -> LOGGER.warn("Uncaught exception", ex));

        regularControllers.stream().sorted().forEach(c -> {
            LOGGER.debug("Registered regular Controller \"{}\"", c);
            c.accept(router);
        });

        Completable.fromRunnable(() -> warmUps.parallelStream().forEach(Runnable::run))
            .subscribeOn(RxHelper.blockingScheduler(vertx))
            .subscribe(
                () -> {
                    LOGGER.info("Warm up finished, try to start HTTP listening on {}:{} ...", listenIp, listenPort);
                    vertx.createHttpServer()
                        .requestHandler(router::accept)
                        .listen(listenPort, listenIp, e -> {
                            if (e.succeeded()) {
                                future.complete();
                            } else {
                                future.fail(e.cause());
                            }
                        });
                },
                ex -> {
                    LOGGER.error("Warmup failed, disable HTTP listening", ex);
                    future.fail(ex);
                }
            );
    }
}
```

提供服务中所需的依赖：

```java
class TheModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new RestModule());
        install(new AuthModule());
		// And more ...

		bind(MyService.class).to(MyServiceImpl.class);
		// And more ...
    }

	@Provides
    @Singleton
    private OtherService providesOtherService(final MyService s) {
        return new OtherServiceImpl(s);
    }
}
```

TheVerticle 和 TheModule 的结合代码由 [Vert.x Guice Extension][vertx-guice] 提供，使用者只需要修改 Verticle 的部署代码为：

```java
Vertx.vertx().deployVerticle(
	"java-guice:" + TheVerticle.class.getName(),
	new DeploymentOptions()
	.setConfig(new JsonObject().put("guice_binder", TheModule.class.getName()))
);
```

作为对比，之前的部署代码为：

```java
Vertx.vertx().deployVerticle(TheVerticle.class.getName());
```

变化之处有两点：

1. Verticle 的名称由原来的 Verticle 类名变为类名加前缀 "java-guice:"
2. 部署时需要附带上一个指向 Module 的依赖提供者实现的配置项，配置项的键名为 "guice_binder"，值为依赖提供者的类名（如果不想添加此配置项，则需要将依赖提供者的类名设置为固定的 ”com.englishtown.vertx.guice.BootstrapBinder"）

## VerticleFactory

[Vert.x Guice Extension][vertx-guice] 基于前缀 "java-guice:" 的功能扩展依赖于 Vert.x 的 `io.vertx.core.spi.VerticleFactory` SPI 机制。

Vert.x 是根据 Verticle 的名称来进行部署的。由于需要支持不同语言的 Verticle 实现（Java, JavaScript, Ruby 等)，Vert.x 对于 Verticle 的创建提供了扩展点 `io.vertx.core.spi.VerticleFactory`。Vert.x 管理不同类型的 VerticleFactory 实现，使用类 scheme 的前缀（prefix）规则区分 Verticle 名称；不同的 VerticleFactory 实现根据 Verticle 名称的前缀来确定是否能创建该 Verticle。

相关内容可参见 [Vert.x Vertivle Deployment][vertx-verticle-factory]。

Vert.x 官方支持的 Verticle 名称前缀有：

- "js:" 用于创建使用 JavaScript 编写的 Verticle
- "groovy:" 用于创建使用 Groovy 编写的 Verticle
- "service:" 用于创建 [Vert.x Service][vertx-service-factory] 服务
- 等

如果提供的 Verticle 名称没有前缀，Vert.x 会根据后缀名来确定对应的 VerticleFactory；如果没有后缀名，则会默认作为 Java 实现来创建。

## GuiceVerticleFactory

在 [Vert.x Guice Extension][vertx-guice] 中，提供了一个 `io.vertx.core.spi.VerticleFactory` 的实现类 `com.englishtown.vertx.guice.GuiceVerticleFactory`，提供对于 "java-guice:" 前缀名称的创建支持。

```java
public class GuiceVerticleFactory implements VerticleFactory {

    public static final String PREFIX = "java-guice";

	@Override
    public String prefix() {
        return PREFIX;
    }

	@Override
    public Verticle createVerticle(String verticleName, ClassLoader classLoader) throws Exception {
        verticleName = VerticleFactory.removePrefix(verticleName);

        // Use the provided class loader to create an instance of GuiceVerticleLoader.  This is necessary when working with vert.x IsolatingClassLoader
        @SuppressWarnings("unchecked")
        Class<Verticle> loader = (Class<Verticle>) classLoader.loadClass(GuiceVerticleLoader.class.getName());
        Constructor<Verticle> ctor = loader.getConstructor(String.class, ClassLoader.class, Injector.class);

        if (ctor == null) {
            throw new IllegalStateException("Could not find GuiceVerticleLoader constructor");
        }

        return ctor.newInstance(verticleName, classLoader, getInjector());
    }
}
```

在 GuiceVerticleFactory 的 `createVerticle` 实现中，使用了 `com.englishtown.vertx.guice.GuiceVertileLoader` 类。该类是 Verticle 的一个实现，用来辅助创建目标 Verticle 并代理目标 Verticle 的生命周期管理。

```java
public class GuiceVerticleLoader extends AbstractVerticle {
	public static final String CONFIG_BOOTSTRAP_BINDER_NAME = "guice_binder";
    public static final String BOOTSTRAP_BINDER_NAME = "com.englishtown.vertx.guice.BootstrapBinder";

	private Verticle realVerticle;

	@Override
    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);

		// Create the real verticle and init
		realVerticle = createRealVerticle();
		realVerticle.init(vertx, context);
	}

	@Override
    public void start(Future<Void> startedResult) throws Exception {
        // Start the real verticle
        realVerticle.start(startedResult);
    }

	@Override
    public void stop(Future<Void> stopFuture) throws Exception {
        // Stop the real verticle
        if (realVerticle != null) {
            realVerticle.stop(stopFuture);
            realVerticle = null;
        }
    }

	private Verticle createRealVerticle(Class<?> clazz) throws Exception {
		// i.e., "guice_binder"
		Object field = config.getValue(CONFIG_BOOTSTRAP_BINDER_NAME);
		jsonArray bootstrapNames;

		if (field instanceof JsonArray) {
            bootstrapNames = (JsonArray) field;
        } else {
			// i.e., "com.englishtown.vertx.guice.BootstrapBinder"
            bootstrapNames = new JsonArray().add((field == null ? BOOTSTRAP_BINDER_NAME : field));
        }

		List<Module> bootstraps = new ArrayList<>();
		for (int i = 0; i < bootstrapNames.size(); i++) {
            String bootstrapName = bootstrapNames.getString(i);
            try {
                Class bootstrapClass = classLoader.loadClass(bootstrapName);
                Object obj = bootstrapClass.newInstance();

                if (obj instanceof Module) {
                    bootstraps.add((Module) obj);
                } else {
                    logger.error("Class " + bootstrapName
                            + " does not implement Module.");
                }
            } catch (ClassNotFoundException e) {
                if (parent == null) {
                    logger.warn("Guice bootstrap binder class " + bootstrapName
                            + " was not found.  Are you missing injection bindings?");
                }
            }
        }

		Injector injector = parent == null ? Guice.createInjector(bootstraps) : parent.createChildInjector(bootstraps);
		return (Verticle) injector.getInstance(clazz);
	}
}
```

在 Verticle 的创建逻辑中，会首先尝试获取键 "guice_binder" 对应的配置项值，该配置项的值可以是全类名字符串或者全类名字符串数组；如果不配置的话，则会默认使用 "com.englishtown.vertx.guice.BootstrapBinder" 作为配置项的值。之后使用该值使用反射创建类实例；再然后使用该实例创建注入器；再然后从注入器中请求目标类名的 Verticle 注入结果实例并返回。

所以只需要把所有的依赖以及指定名称的 Verticle 实现的提供者（`com.google.inject.Module` 实现类）的全类名作为值，以 "guice_binder" 为键放入到部署参数配置里面，就可以自动生成一个注入器了。或者，在不提供该键值对的情况下，将依赖提供者实现命名为 "com.englishtown.vertx.guice.BootstrapBinder" 也可以达到相同的效果。

# 总结

[Vert.x Guice Extension][vertx-guice] 的实现充分利用了 Vert.x 的扩展机制，解耦合了 Verticle 的实现和依赖注入，思路非常漂亮。对于功能的添加又很克制，没有尝试去实现其他没有必要的 auto-wired、package scanning 等功能（如果需要这些功能，为什么不直接使用 [Spring Framework][spring] 呢？）。

以上

[vertx-guice]: https://github.com/ef-labs/vertx-guice "Google Guice"
[vertx]: https://vertx.io "Eclipse Vert.x"
[vertx-service-factory]: http://vertx.io/docs/vertx-service-factory/java/ "Eclipse Vert.x Service Factory"
[vertx-verticle-factory]: https://vertx.io/docs/vertx-core/java/#_deploying_verticles_programmatically "Deploying verticles programmatically"
[guice]: https://github.com/google/guice "Google Guice"
[guice-multibindings]: https://github.com/google/guice/wiki/Multibindings "Google Guice MultiBindings Extension"
[guice-injections]: https://github.com/google/guice/wiki/Injections "Google Guice injectMembers"
[spring]: https://spring.io
