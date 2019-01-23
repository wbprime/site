---
title : "Nginx Note - Modules Design"
date : 2018-02-06T16:23:34+08:00
categories : ["Notes"]
tags : ["nginx"]
description : "Note of modules designing for Nginx"
draft : false
---

转载自[Nginx 模块的统一架构](http://www.lenky.info/archives/2011/09/62)

# Nginx 的模块统一架构

这一系列的文章还是在09年写的，存在电脑里很久了，现在贴出来。顺序也不记得了，看到那个就发那个吧，最近都会发上来。欢迎转载，但请保留链接：http://lenky.info/ ，谢谢。

当我们configure配置 Nginx 后，将在一个名为objs的文件夹里找一个 `ngx_modules.c` 源文件，该文件内就包含了我们待编译生成的 Nginx 程序即将包含的模块。

Nginx 的模块可以简单点分为 `core`, `conf`, `event`, `http` 和 `mail` 五种类型，这从 Nginx 源码的目录结构也可以依稀看出。

封装 Nginx 模块的结构体为 `ngx_module_s`，定义如下：

```
typedef struct ngx_module_s      ngx_module_t;
struct ngx_module_s {
    //存储当前模块在同类模块中的排号。
    ngx_uint_t            ctx_index;
    //存储当前模块在所有模块中的排号。
    ngx_uint_t            index;
    //以下四字段留作备用，用于将来扩展。
    ngx_uint_t            spare0;
    ngx_uint_t            spare1;
    ngx_uint_t            spare2;
    ngx_uint_t            spare3;
    //记录模块版本号
    ngx_uint_t            version;
    //指向模块特有的数据，因为指向的数据类型不确定，所以是个void类型指针。
    void                 *ctx;
    //指向一个ngx_command_t类型的数组，具体后面会讲解到。
    ngx_command_t        *commands;
    //取值为NGX_CORE_MODULE、NGX_EVENT_MODULE、
    //NGX_HTTP_MODULE、NGX_MAIL_MODULE、NGX_CONF_MODULE
    //之一，用于标识当前模块属于哪种类型。
    ngx_uint_t            type;
    //以下7个字段为函数指针，可以给它们赋上合适的回调函数，则nginx会在
    //相应的时机调用它们，如果模块不关心（比如不用做初始化或销毁）则直接
    //赋值为NULL即可。
    //回调函数，master初始化时调用。
    ngx_int_t           (*init_master)(ngx_log_t *log);
    //回调函数，模块初始化时调用。
    ngx_int_t           (*init_module)(ngx_cycle_t *cycle);
    //回调函数，工作进程初始化时调用。
    ngx_int_t           (*init_process)(ngx_cycle_t *cycle);
    //回调函数，线程初始化时调用。
    ngx_int_t           (*init_thread)(ngx_cycle_t *cycle);
    //回调函数，线程退出时调用。
    void                (*exit_thread)(ngx_cycle_t *cycle);
    //回调函数，工作进程退出时调用。
    void                (*exit_process)(ngx_cycle_t *cycle);
    //回调函数，master退出时调用。
    void                (*exit_master)(ngx_cycle_t *cycle);
    //同之前的spare0~ spare4字段一样，下面这八个字段留作备用。
    uintptr_t             spare_hook0;
    uintptr_t             spare_hook1;
    uintptr_t             spare_hook2;
    uintptr_t             spare_hook3;
    uintptr_t             spare_hook4;
    uintptr_t             spare_hook5;
    uintptr_t             spare_hook6;
    uintptr_t             spare_hook7;
};
```

众览Nginx各模块对应的 `ngx_module_t`
结构体变量，可以看到上面介绍的各字段基本都为 `NULL` 或 `0`，而只有几个字段
`ctx`, `commands`, `type`，几乎每个模块都有使用，而 `type`
显然是必须的，那么另外两个字段 `ctx`, `commands` 有何作用？

前面说 `ctx`
记录的是模块自身特有的数据，因为指向的数据类型不确定，所以该字段是个 `void`
类型指针。事实上，该字段指向的数据只有五种类型，根据其模块类型 `type`
的不同而不同，具有相同模块类型 `type`
的模块，其ctx字段指向的数据类型是一样的。各种类型模块的ctx指向的数据结构体如下：

1. NGX_CORE_MODULE

```
typedef struct {
    ngx_str_t           name;
    void               *(*create_conf)(ngx_cycle_t *cycle);
    char               *(*init_conf)(ngx_cycle_t *cycle, void *conf);
} ngx_core_module_t;
```

2. NGX_EVENT_MODULE

```
typedef struct {
    ngx_str_t            *name;
    void                 *(*create_conf)(ngx_cycle_t *cycle);
    char                 *(*init_conf)(ngx_cycle_t *cycle, void *conf);
    ngx_event_actions_t   actions;
} ngx_event_module_t;
```

3. NGX_CONF_MODULE

无

4. NGX_HTTP_MODULE

```
typedef struct {
    ngx_int_t   (*preconfiguration)(ngx_conf_t *cf);
    ngx_int_t   (*postconfiguration)(ngx_conf_t *cf);
    void       *(*create_main_conf)(ngx_conf_t *cf);
    char       *(*init_main_conf)(ngx_conf_t *cf, void *conf);
    void       *(*create_srv_conf)(ngx_conf_t *cf);
    char       *(*merge_srv_conf)(ngx_conf_t *cf, void *prev, void *conf);
    void       *(*create_loc_conf)(ngx_conf_t *cf);
    char       *(*merge_loc_conf)(ngx_conf_t *cf, void *prev, void *conf);
} ngx_http_module_t;
```

5. NGX_MAIL_MODULE

```
typedef struct {
    ngx_mail_protocol_t *protocol;
    void                *(*create_main_conf)(ngx_conf_t *cf);
    char                *(*init_main_conf)(ngx_conf_t *cf, void *conf);
    void                *(*create_srv_conf)(ngx_conf_t *cf);
    char                *(*merge_srv_conf)(ngx_conf_t *cf, void *prev, void *conf);
} ngx_mail_module_t;
```

可以看到 `ctx`
字段指向的数据基本都是一些由回调函数组成的结构体类型，这些回调函数会在进行模块的配置信息解析的恰当时候被调用，主要用来创建存储模块配置信息所需的空间、配置信息的初始化、合并等。而字段
`commands` 在各种模块中都很统一，指向的是 `ngx_command_s`
结构体类型变量，对于该结构体的说明在 Nginx 配置信息解析流程一文中已经有详细分析，知道该结构体类型变量的作用就是解析并获取 Nginx 配置值。

弄清楚了 `ngx_module_s` 结构体各个字段的含义，对于 Nginx 模块架构的理解就算是入门了，

转载请保留地址：http://www.lenky.info/archives/2011/09/62 或 http://lenky.info/?p=62

备注：如无特殊说明，文章内容均出自Lenky个人的真实理解而并非存心妄自揣测来故意愚人耳目。由于个人水平有限，虽力求内容正确无误，但仍然难免出错，请勿见怪，如果可以则请留言告之，并欢迎来信讨论。另外值得说明的是，Lenky的部分文章以及部分内容参考借鉴了网络上各位网友的热心分享，特别是一些带有完全参考的文章，其后附带的链接内容也许更直接、更丰富，而我只是做了一下归纳&转述，在此也一并表示感谢。关于本站的所有技术文章，欢迎转载，但请遵从CC创作共享协议，而一些私人性质较强的心情随笔，建议不要转载。
