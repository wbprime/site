---
title: "Nginx Note - Signal Handling"
date: 2018-02-06T16:25:24+08:00
categories: ["Notes"]
tags: ["nginx"]
description: "Note of signal handling for Nginx"
draft: false
---

转载自[Nginx 的信号管理](http://www.lenky.info/archives/2011/09/60)

# Nginx 的信号管理

这一系列的文章还是在09年写的，存在电脑里很久了，现在贴出来。顺序也不记得了，看到那个就发那个吧，最近都会发上来。欢迎转载，但请保留链接：http://lenky.info/ ，谢谢。

Nginx对所有发往其自身的信号进行了统一管理，这部分相关实现代码不多，而且十分清晰易懂，下面来逐步解析。

首先，Nginx对一种信号以及该信号的处理封装了一个对应的名为`ngx_signal_t`的结构体：

```
typedef struct {
	int     signo;              // 信号值
	char   *signame;            // 信号名
	char   *name;               // 名称，和信号名不一样，名称表明该信号的自定义作用
	void  (*handler)(int signo);// 信号处理函数指针
} ngx_signal_t;
```

接着，Nginx定义了一个`ngx_signal_t`类型的全局数组变量`signals`，该变量内包含了Nginx所有要处理的信号，如下：

```
ngx_signal_t  signals[] = {
{
	ngx_signal_value(NGX_RECONFIGURE_SIGNAL),     //SIGHUP
	"SIG" ngx_value(NGX_RECONFIGURE_SIGNAL),      //"SIGHUP"，这里带了引号
	"reload",                                     //表示SIGHUP用户重新加载Nginx，也就是reload configure
	ngx_signal_handler },                         //信号处理函数，重点
	{ ngx_signal_value(NGX_REOPEN_SIGNAL),
	"SIG" ngx_value(NGX_REOPEN_SIGNAL),
	"reopen",
	ngx_signal_handler },
                                                  // ...
	{ SIGPIPE, "SIGPIPE, SIG_IGN", "", SIG_IGN }, //忽略信号也是一种处理方式
	{ 0, NULL, "", NULL }                         //数组结束哨兵元素，很多地方都这么用
}
```

`ngx_signal_value`、`NGX_RECONFIGURE_SIGNAL`、`ngx_value`都是一些宏，扩展开来就容易看懂，而`ngx_signal_handler`为一个函数指针，用来处理接收到的信号。

上面只是准备工作，还需调用类似于`signal`、`sigaction`这些系统函数来进行信号安插设置，已告诉系统当Nginx进程收到其关注的信号时调用Nginx自定义的信号处理函数。顺着Nginx的`main`函数往下寻找，可以看到一个`ngx_init_signals`函数调用，跟进该函数内部，了了几行代码，实现的正好就是信号安插设置功能，很容易看懂：

```
ngx_int_t
ngx_init_signals(ngx_log_t *log)
{
	ngx_signal_t      *sig;
	struct sigaction   sa;

	for (sig = signals; sig->signo != 0; sig++) {
		ngx_memzero(&sa, sizeof(struct sigaction));
		sa.sa_handler = sig->handler;
		sigemptyset(&sa.sa_mask);
		if (sigaction(sig->signo, &sa, NULL) == -1) {
			ngx_log_error(NGX_LOG_EMERG, log, ngx_errno,
			"sigaction(%s) failed", sig->signame);
			return NGX_ERROR;
		}
	}

	return NGX_OK;
}
```

当Nginx进程收到其关注的信号时就会执行相应的回调函数`ngx_signal_handler`，该函数内的实现逻辑也很简单，仅仅只是根据其收到的信号对相应的全局变量进行置位操作，这符合信号处理函数简单快速的一般特点。

```
void
ngx_signal_handler(int signo)
{
	char            *action;
	ngx_int_t        ignore;
	ngx_err_t        err;
	ngx_signal_t    *sig;
	ignore = 0;
	err = ngx_errno;

	for (sig = signals; sig->signo != 0; sig++) {
		if (sig->signo == signo) {
			break;
		}
	}

	ngx_time_update(0, 0);
	action = "";
	switch (ngx_process) {
		case NGX_PROCESS_MASTER:
		case NGX_PROCESS_SINGLE:
			switch (signo) {
				case ngx_signal_value(NGX_SHUTDOWN_SIGNAL):
					ngx_quit = 1;
					action = ", shutting down";
					break;
				case ngx_signal_value(NGX_TERMINATE_SIGNAL):
				case SIGINT:
					ngx_terminate = 1;
					action = ", exiting";
					break;
				case ngx_signal_value(NGX_NOACCEPT_SIGNAL):
					ngx_noaccept = 1;
					action = ", stop accepting connections";
					break;
				case ngx_signal_value(NGX_RECONFIGURE_SIGNAL):
					ngx_reconfigure = 1;
					action = ", reconfiguring";
					break;
				case ngx_signal_value(NGX_REOPEN_SIGNAL):
					ngx_reopen = 1;
					action = ", reopening logs";
					break;
				case ngx_signal_value(NGX_CHANGEBIN_SIGNAL):
					if (getppid() > 1 || ngx_new_binary > 0) {
						/*
						* Ignore the signal in the new binary if its parent is
						* not the init process, i.e. the old binary’s process
						* is still running.  Or ignore the signal in the old binary’s
						* process if the new binary’s process is already running.
						*/
						action = ", ignoring";
						ignore = 1;
						break;
					}
					ngx_change_binary = 1;
					action = ", changing binary";
					break;
				case SIGALRM:
					ngx_sigalrm = 1;
					break;
				case SIGIO:
					ngx_sigio = 1;
					break;
				case SIGCHLD:
					ngx_reap = 1;
					break;
			}
			break;
		case NGX_PROCESS_WORKER:
		case NGX_PROCESS_HELPER:
			switch (signo) {
				case ngx_signal_value(NGX_NOACCEPT_SIGNAL):
					ngx_debug_quit = 1;
				case ngx_signal_value(NGX_SHUTDOWN_SIGNAL):
					ngx_quit = 1;
					action = ", shutting down";
					break;
				case ngx_signal_value(NGX_TERMINATE_SIGNAL):
				case SIGINT:
					ngx_terminate = 1;
					action = ", exiting";
					break;
				case ngx_signal_value(NGX_REOPEN_SIGNAL):
					ngx_reopen = 1;
					action = ", reopening logs";
					break;
				case ngx_signal_value(NGX_RECONFIGURE_SIGNAL):
				case ngx_signal_value(NGX_CHANGEBIN_SIGNAL):
				case SIGIO:
					action = ", ignoring";
					break;
			}
		break;
	}

	ngx_log_error(NGX_LOG_NOTICE, ngx_cycle->log, 0,
				"signal %d (%s) received%s", signo, sig->signame, action);
	if (ignore) {
		ngx_log_error(NGX_LOG_CRIT, ngx_cycle->log, 0,
		"the changing binary signal is ignored: "
		"you should shutdown or terminate "
		"before either old or new binary’s process");
	}
	if (signo == SIGCHLD) {
		ngx_process_get_status();
	}
	ngx_set_errno(err);
}
```

转载请保留地址：http://www.lenky.info/archives/2011/09/60 或 http://lenky.info/?p=60

备注：如无特殊说明，文章内容均出自Lenky个人的真实理解而并非存心妄自揣测来故意愚人耳目。由于个人水平有限，虽力求内容正确无误，但仍然难免出错，请勿见怪，如果可以则请留言告之，并欢迎来信讨论。另外值得说明的是，Lenky的部分文章以及部分内容参考借鉴了网络上各位网友的热心分享，特别是一些带有完全参考的文章，其后附带的链接内容也许更直接、更丰富，而我只是做了一下归纳&转述，在此也一并表示感谢。关于本站的所有技术文章，欢迎转载，但请遵从CC创作共享协议，而一些私人性质较强的心情随笔，建议不要转载。
