---
title: "Libevent Note"
date: 2018-02-05T17:08:50+08:00
categories: "Notes"
tags: ["libevent"]
description: "Note on libevent"
draft: false
---

# Event Context

## Structure

`event_base`

## Setup a Default Context

```
#include <event2/event.h>
struct event_base *event_base_new(void);
```

## Setup a Custom Context

可以用 `event_config` 来自定义上下文。

```
struct event_config *event_config_new(void);
struct event_base *event_base_new_with_config(const struct event_config *cfg);
void event_config_free(struct event_config *cfg);
```

`event_config` 的自定义方法：

```
#include <event2/event.h>
int event_config_avoid_method(struct event_config *cfg, const char *method);

enum event_method_feature {
    EV_FEATURE_ET = 0x01,
    EV_FEATURE_O1 = 0x02,
    EV_FEATURE_FDS = 0x04,
};
int event_config_require_features(struct event_config *cfg,
                                  enum event_method_feature feature);

enum event_base_config_flag {
    EVENT_BASE_FLAG_NOLOCK = 0x01,
    EVENT_BASE_FLAG_IGNORE_ENV = 0x02,
    EVENT_BASE_FLAG_STARTUP_IOCP = 0x04,
    EVENT_BASE_FLAG_NO_CACHE_TIME = 0x08,
    EVENT_BASE_FLAG_EPOLL_USE_CHANGELIST = 0x10,
    EVENT_BASE_FLAG_PRECISE_TIMER = 0x20
};
int event_config_set_flag(struct event_config *cfg,
    enum event_base_config_flag flag);
```

`event_config_avoid_method()` 指定不使用的后端名

	`const char **event_get_supported_methods(void);` 可以用于获取可用的后端名列表

## Deallocate a Context

```
#include <event2/event.h>
void event_base_free(struct event_base *base);
```

## Reinit a Context

```
#include <event2/event.h>
int event_reinit(struct event_base *base);
```

一般在`fork`之后需要调用待方法。

# Iterate All Active/Pending Events

```
typedef int (*event_base_foreach_event_cb)(
    const struct event_base *,
    const struct event *, void *);
    
int event_base_foreach_event(
    struct event_base *base,
    event_base_foreach_event_cb fn,
    void *arg);
```

`event_base_foreach_event()` 会以随机的顺序遍历当前所有可用的event，每一个event执行一次参数fn的函数，参数arg会被传入参数fn中作为第三个参数。

参数fn的返回值如果不是0,则退出迭代过程。

`event_base_foreach_event()`的返回值是参数fn的最后一次返回值。

# Event Loop

## Run Event Loop

```c
#define EVLOOP_ONCE             0x01
#define EVLOOP_NONBLOCK         0x02
#define EVLOOP_NO_EXIT_ON_EMPTY 0x04

int event_base_loop(struct event_base *base, int flags);
```

flags:

- `EVLOOP_ONCE` the loop wait active events
- `EVLOOP_NONBLOCK` the loop checks available active events
- `EVLOOP_NO_EXIT_ON_EMPTY` the loop will not exit even when no active events

As a convinience, `libevent` provides another method.

```c
int event_base_dispatch(struct event_base *base);
```

Thus the loop keeps running until there are no more registered events or until
event_base_loopbreak() or event_base_loopexit() is called.

## Stop Event Loop

- `int event_base_loopexit(struct event_base *base, const struct timeval *tv);` 
  
    设定base在指定时间间隔后退出循环；如果参数tv为null，则立即退出；如果要退出时
    ，还有callback没有运行，则运行完了再退出
    
- `int event_base_loopbreak(struct event_base *base);`
    
    设定base马上退出循环
    
`event_base_loopbreak()` VS `event_base_loopexit(base, NULL)`

二者的区别在于前者等待正在运行的callback执行结束立即退出；后者等所有的就绪的
callback执行结束才退出。另外，如果当前没有循环，loopexit会是的下一次运行的loop退
出；而loopbreak不会影响到后续的loop。

## Get the Exit Reason

```
int event_base_got_exit(struct event_base *base);
int event_base_got_break(struct event_base *base);
```

判断event循环退出的原因（方式），分别对应与loopexit和loopbreak，如果是则返回非0值。

## Skip to Next Loop

`int event_base_loopcontinue(struct event_base *);`

退出本次循环，进行下一次循环

# Events

## Overview

`event` 封装了以下信息：

- 读写就绪的文件描述符（fd）
- 定时器超时
- 发生了信号的信息
- 用户自定义的事件

## Lifecycle of Event

- initialized
	event新建并添加到上下文（event_base）之后
- pending
	pending可以通过deleting来变成non-pending；反之可以通过adding将non-pending变为pending
    event变为active状态之后，会在将要调用callbakc之前变为non-pending的状态
- active
	不同类型的event被触发（文件描述符可读可写，定时器超时），关联的callback回调会被执行
- persistent
	event可以设置为persistent：event可以在执行了callback之后还保持pending状态
	
## Event 的创建与销毁

### 通用方法

```
#include <event2/event.h>
#define EV_TIMEOUT      0x01
#define EV_READ         0x02
#define EV_WRITE        0x04
#define EV_SIGNAL       0x08
#define EV_PERSIST      0x10
#define EV_ET           0x20

typedef void (*event_callback_fn)(evutil_socket_t, short, void *);

struct event *event_new(struct event_base *base, evutil_socket_t fd,
    short what, event_callback_fn cb,
    void *arg);
int event_base_once(struct event_base *, evutil_socket_t, short,
  void (*)(evutil_socket_t, short, void *), void *, const struct timeval *);

void event_free(struct event *event);
```

`event_new`
中的参数cb是目标callback，在对应的event被触发使会被调用，参数fd，被触发的事件类型和arg会被作为参数传入。

调用 `event_new()` 之后的event是initialized和non-pending的。

如果想将要创建的event作为第三个参数传入callback中，可以使用 `void *event_self_cbarg();` 的trick：`event_new(base, -1, EV_PERSIST, cb_func, event_self_cbarg());`。

### 定时事件

libevent提供了一系列的辅助函数简化定时事件的创建。

```
#define evtimer_new(base, callback, arg) \
    event_new((base), -1, 0, (callback), (arg))
#define evtimer_add(ev, tv) \
    event_add((ev),(tv))
#define evtimer_del(ev) \
    event_del(ev)
#define evtimer_pending(ev, tv_out) \
    event_pending((ev), EV_TIMEOUT, (tv_out))
```

### 信号处理事件

libevent提供了一系列的辅助函数简化信号事件的创建。

```c
#define evsignal_new(base, signum, cb, arg) \
    event_new(base, signum, EV_SIGNAL|EV_PERSIST, cb, arg)
#define evsignal_add(ev, tv) \
    event_add((ev),(tv))
#define evsignal_del(ev) \
    event_del(ev)
#define evsignal_pending(ev, what, tv_out) \
    event_pending((ev), (what), (tv_out))
```

举例：

```c
evsignal_new(base, SIGHUP, sighup_function, NULL);
```

## Pending & Non-Pending

pending -> non-pending

`int event_add(struct event *ev, const struct timeval *tv);`

如果当前event已经是pending了，会尝试修改过期时间。

non-pending -> pending

`int event_del(struct event *ev);`

如果只想修改event的过期时间而不改变pending状态，使用`int event_remove_timer(struct event *ev);`

## Priority

`int event_priority_set(struct event *event, int priority);`

## Get Event Info

```c
int event_pending(const struct event *ev, short what, struct timeval *tv_out);

#define event_get_signal(ev) /* ... */
evutil_socket_t event_get_fd(const struct event *ev);
struct event_base *event_get_base(const struct event *ev);
short event_get_events(const struct event *ev);
event_callback_fn event_get_callback(const struct event *ev);
void *event_get_callback_arg(const struct event *ev);
int event_get_priority(const struct event *ev);

void event_get_assignment(const struct event *event,
        struct event_base **base_out,
        evutil_socket_t *fd_out,
        short *events_out,
        event_callback_fn *callback_out,
        void **arg_out);
```

## Manually Trigger a Event

`void event_active(struct event *ev, int what, short ncalls);`
