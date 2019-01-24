+++
title = "Nginx Note - Config Parsing and Loading"
description = "Note of config parsing and loading for Nginx"
date = 2018-02-06T16:26:08+08:00
draft = false
[taxonomies]
categories =  ["Notes"]
tags = ["nginx"]
+++

转载自[Nginx 配置信息的解析流程](http://www.lenky.info/archives/2011/09/22)

# Nginx 配置信息的解析流程

这一系列的文章还是在09年写的，存在电脑里很久了，现在贴出来。顺序也不记得了，看到那个就发那个吧，最近都会发上来。欢迎转载，但请保留链接：http://lenky.info/ ，谢谢。

Nginx 的配置文件格式是 Nginx 作者自己定义的，并没有采用像语法分析生成器LEMON那种经典的LALR（1）来描述配置信息，这样做的好处就是自由，而坏处就是对于Nginx的每一项配置信息都必须自己去解析，因此我们很容易看到Nginx模块里大量篇幅的配置信息解析代码，比如模块 `ngx_http_core_module`。

当然，Nginx配置文件的格式也不是随意的，它有自己的一套规范：

Nginx配置文件是由多个配置项组成的。每一个配置项都有一个项目名和对应的项目值，项目名又被称为指令（Directive），而项目值可能简单的字符串（以分号结尾），也可能是由简单字符串和多个配置项组合而成配置块的复杂结构（以大括号}结尾），因此我们可以将配置项归纳为两种：简单配置项和复杂配置项。

![配置解析示意图](/2018-02-06-nginx-note.dir/nginx_config_parsing_procedure_01.png)

其项目名 "daemon" 为一个token，项目值 "off" 也是一个token。而简单配置项：

`error_page  404   /404.html;`

其项目值就包含有两个token，分别为 "404" 和 "/404.html" 。

对于复杂配置项：

```
location /www {
    index    index.html index.htm index.php;
}
```

其项目名 `location` 为一个token，项目值是一个token(./posts/www")和多条简单配置项组成的复合结构。

前面将token解释为一个配置文件字符串内容中被空格、引号、括号，比如 '{' 等分割开来的字符子串，那么很明显，上面例子中的taken是被空格分割出来，事实上下面这样的配置也是正确的：

```
"daemon" "off";
'daemon' 'off';
daemon 'off';
"daemon" off;
```

当然，一般情况下没必要这样费事去加些引号，除非我们需要在token内包含空格而又不想使用转义字符('\')的话就可以利用引号，比如：

```
log_format   main '$remote_addr – $remote_user [$time_local]  $status '
'"$request" $body_bytes_sent "$http_referer" '
'"$http_user_agent" "$http_x_forwarded_for"';
```

但是像下面这种格式就会有问题，这对于我们来说很容易理解，不多详叙：

```
"daemon "off";
```

对于如此多的配置项，Nginx怎样去解析它们呢？在什么时候去解析呢？事实上，对于Nginx所有可能出现的配置项(通过项目名即指令Directive去判断)，Nginx都会提供有对应的代码去解析它，如果配置文件内出现了Nginx无法解析的配置项，那么Nginx将报错并直接退出程序。

举例来说，对于配置项 `daemon` ，在模块 `ngx_core_module` 的命令解析数组内的第一项就是保存的对该配置项进行解析所需要的信息，比如 `daemon` 配置项的类型，执行实际解析操作的回调函数，解析出来的配置项值所存放的地址等：

```c
{
    ngx_string("daemon"),
    NGX_MAIN_CONF|NGX_DIRECT_CONF|NGX_CONF_FLAG,
    ngx_conf_set_flag_slot,
    0,
    offsetof(ngx_core_conf_t, daemon),
    NULL 
},
```

而如果我在配置文件内加入如下配置内容：

`lenky on;`

启动 Nginx，直接返回错误，这是因为对于 `lenky` 指令，Nginx 没有对应的代码去解析它：

`[emerg]: unknown directive "lenky" in /usr/local/nginx/conf/nginx.conf:2`

上面给出的解析 `daemon` 配置项的数据类型为 `ngx_command_s` 结构体类型，该结构体类型对所有的 Nginx 配置项进行了统一的描述：

```
typedef struct ngx_command_s     ngx_command_t;
struct ngx_command_s {
    ngx_str_t             name;
    ngx_uint_t            type;
    char               *(*set)(ngx_conf_t *cf, ngx_command_t *cmd, void *conf);
    ngx_uint_t            conf;
    ngx_uint_t            offset;
    void                 *post;
};
```

一个 `ngx_command_s` 结构体类型的元素用于解析并获取一项Nginx配置，其中字段 `name` 指定获取的配置项目名称，字段 `set` 指向一个回调函数，该函数执行解析并获取配置项值的操作；而 `type` 指定该配置项的相关信息，比如：

1. 该配置的类型: `NGX_CONF_FLAG` 表示该配置项目有一个布尔类型的值，例如
   "daemon" 就是一个布尔类型配置项，值为 "on" 或 "off": `NGX_CONF_BLOCK`
   表示该配置项目有一个块类型的值，比如配置项 "http", "events" 等。
2. 该配置接收的参数个数: `NGX_CONF_NOARGS`, `NGX_CONF_TAKE1`,
   `NGX_CONF_TAKE2`, ..., `NGX_CONF_TAKE7`
   ，分别表示该配置项没有参数、一个、两个、七个参数。
3. 该配置的位置域: `NGX_MAIN_CONF`, `NGX_HTTP_MAIN_CONF`,
   `NGX_EVENT_CONF`,`NGX_HTTP_SRV_CONF`,`NGX_HTTP_LOC_CONF`,`NGX_HTTP_UPS_CONF`
   等等。

字段 `conf` 被 `NGX_HTTP_MODULE`
类型模块所用，该字段指定当前配置项所在的大致位置，取值为
`NGX_HTTP_MAIN_CONF_OFFSET`,
`NGX_HTTP_SRV_CONF_OFFSET`, `NGX_HTTP_LOC_CONF_OFFSET`
三者之一；其它模块不用该字段，直接指定为 "0"。

字段 `offset`
指定该配置项值的精确存放位置，一般指定为某一个结构体变量的字段偏移。也有那种块配置项，例如
"server"
，它不用保存配置项值，或者说无法保存，或者说其值被分得更细小而被保存起来，此时字段
"offset" 也指定为 "0" 即可。

字段 `post` 在大多数情况下为 `NULL`
，但在某些特殊配置项中也会指定值，而且多为回调函数指针，例如 `auth_basic`,
`connection_pool_size`, `request_pool_size`, `optimize_host_names`,
`client_body_in_file_only` 等配置项。

对于配置文件的格式以及配置项在Nginx中的封装基本就描述到这，下面开始对整个Nginx配置信息的解析流程进行描述。

假设我们以命令：

`nginx -c /usr/local/nginx/conf/nginx.conf`

启动Nginx，而配置文件 "nginx.conf" 也比较简单，如下所示：

```
worker_processes  2;
error_log  logs/error.log debug;
events {
    use epoll;
    worker_connections  1024;
}
http {
    include       mime.types;
    default_type  application/octet-stream;
    server {
        listen       8888;
        server_name  localhost;
        location / {
            root   html;
            index  index.html index.htm;
        }
        error_page  404              /404.html;
        error_page   500 502 503 504  /50x.html;
        location = /50x.html {
            root   html;
        }
    }
}
```

下面就来描述Nginx是如何将这些配置信息转化为Nginx内各对应变量的值以控制Nginx工作的。

首先，抹掉一些细节，我们跟着Nginx的启动流程进入到与配置信息相关的函数调用处：

(main–>ngx_init_cycle–>ngx_conf_parse)

```c
if (ngx_conf_parse(&conf, &cycle->conf_file) != NGX_CONF_OK) {
    environ = senv;
    ngx_destroy_cycle_pools(&conf);
    return NULL;
}
```

此处调用 `ngx_conf_parse` 传入了两个参数，第一个参数为 `ngx_conf_s` 变量，而第二个参数就是保存的配置文件路径字符串 "/usr/local/nginx/conf/nginx.conf"。 `ngx_conf_parse` 函数是执行配置解析的关键函数，其原型如下：

`char * ngx_conf_parse(ngx_conf_t *cf, ngx_str_t *filename)`

它是一个间接的递归函数，也就是说虽然我们在该函数体内看不到直接的对其本身的调用，但是它执行的一些函数(比如
`ngx_conf_handler`)内又会调用 `ngx_conf_parse`
函数，因此形成递归，这一般在处理一些特殊配置指令或复杂配置项，比如指令
`include`, `events`, `http`, `server`, `location`等的处理时。

`ngx_conf_parse` 函数体代码量不算太多，但是它也将配置内容的解析过程分得很清楚，总体来看分成三个步骤：

1. 区分当前解析状态；
2. 读取配置标记token；
3. 当读取了合适数量的标记token之后对其进行实际的处理，转换为Nginx内变量的值。

当执行到 `ngx_conf_parse` 函数内时，配置的解析可能处于三种状态：

1. 第一种，刚开始解析一个配置文件，即此时的参数filename指向一个配置文件路径字符串，需要函数 `ngx_conf_parse` 打开该文件并获取相关的文件信息以便下面代码读取文件内容并进行解析，除了在上面介绍的Nginx启动时开始主配置文件解析时属于这种情况，还有当遇到 `include` 指令时也将以这种状态调用 `ngx_conf_parse` 函数，因为 `include` 指令表示一个新的配置文件要开始解析。状态标记为 `type = parse_file`。
2.
第二种，开始解析一个配置块，即此时配置文件已经打开并且也已经对文件部分进行了解析，当遇到复杂配置项比如
`events`, `http` 等时，这些复杂配置项的处理函数又会递归的调用 `ngx_conf_parse` 函数，此时解析的内容还是来自当前的配置文件，因此无需再次打开它，状态标记为 `type = parse_block`。
3. 第三种，开始解析配置项，这在对用户通过命令行 `-g`
   参数输入的配置信息进行解析时处于这种状态，如: `nginx -g 'daemon on;'` Nginx在调用 `ngx_conf_parse` 函数对配置信息 `daemon on;` 进行解析时就是这种状态，状态标记为 `type = parse_param;`。

前面说过，Nginx配置是由标记组成的，在区分好了解析状态之后，接下来就要读取配置内容，而函数 `ngx_conf_read_token` 就是做这个事情的：

`rc = ngx_conf_read_token(cf);`

函数 `ngx_conf_read_token`
对配置文件内容逐个字符扫描并解析为单个的token，当然，该函数并不会频繁的去读取配置文件，它每次从文件内读取足够多的内容以填满一个大小为
`NGX_CONF_BUFFER`
的缓存区(除了最后一次，即配置文件剩余内容本来就不够了)，这个缓存区在函数
`ngx_conf_parse` 内申请并保存引用到变量 `cf->conf_file->buffer` 内，函数 `ngx_conf_read_token` 反复使用该缓存区，该缓存区可能有如下一些状态：

初始状态，即函数 `ngx_conf_parse` 内申请后的初始状态。

![初始状态](/2018-02-06-nginx-note.dir/nginx_config_parsing_procedure_02.png)

![处理中状态](/2018-02-06-nginx-note.dir/nginx_config_parsing_procedure_03.png)

这是在处理过程中的状态，有一部分配置内容已经被解析为一个个token并保存起来，而有一部分内容正要被组合成token，还有一部分内容等待处理。

![继续读文件状态](/2018-02-06-nginx-note.dir/nginx_config_parsing_procedure_04.png)

这是在字符都处理完了，需要继续从文件内读取新的内容到缓存区。前面图示说过，已解析字符已经没用了，因此我们可以将已扫描但还未组成token的字符移动到缓存区的前面，然后从配置文件内读取内容填满缓存区剩余的空间，情况如下：

![填充缓冲区状态](/2018-02-06-nginx-note.dir/nginx_config_parsing_procedure_05.png)

如果配置文件内容不够，即最后一次，那么情况就是下面这样：

![终止状态](/2018-02-06-nginx-note.dir/nginx_config_parsing_procedure_06.png)

函数 `ngx_conf_read_token` 在读取了合适数量的标记token之后就开始下一步骤即对这些标记进行实际的处理。那多少才算是读取了合适数量的标记呢？区别对待，对于简单配置项则是读取其全部的标记，也就是遇到结束标记分号;为止，此时一条简单配置项的所有标记都被读取并存放在 `cf->args` 数组内，因此可以调用其对应的回调函数进行实际的处理；对于复杂配置项则是读完其配置块前的所有标记，即遇到大括号 `{` 为止，此时复杂配置项处理函数所需要的标记都已读取到，而对于配置块 `{}` 内的标记将在接下来的函数 `ngx_conf_parse` 递归调用中继续处理，这可能是一个反复的过程。

当然，函数 `ngx_conf_read_token` 也可能在其它情况下返回，比如配置文件格式出错、文件处理完(遇到文件结束)、块配置处理完(遇到大括号 `}`)，这几种返回情况的处理都很简单，不多详叙。

对于简单或复杂配置项的处理，一般情况下，这是通过函数 `ngx_conf_handler`
来进行的，而也有特殊的情况，也就是配置项提供了自定义的处理函数，比如 `types`
指令。函数 `ngx_conf_handler`
也做了三件事情，首先，它需要找到当前解析出来的配置项所对应的 `ngx_command_s`
结构体，前面说过该 `ngx_command_s`
包含有配置项的相关信息以及对应的回调实际处理函数。如果没找到配置项所对应的
`ngx_command_s`
结构体，那么谁来处理这个配置项呢？自然是不行的，因此nginx就直接进行报错并退出程序。其次，找到当前解析出来的配置项所对应的
`ngx_command_s` 结构体之后还需进行一些有效性验证，因为 `ngx_command_s`
结构体内包含有配置项的相关信息，因此有效性验证是可以进行的，比如配置项的类型、位置、带参数的个数等等。只有经过了严格有效性验证的配置项才调用其对应的回调函数: `rv = cmd->set(cf, cmd, conf);` 进行处理，这也就是第三件事情。在处理函数内，根据实际的需要又可能再次调用函数 `ngx_conf_parse` ，如此反复直至所有配置信息都被处理完。

下面来看一个set回调函数的例子，以对配置指令 `daemon` 的解析函数为例，根据前面给出的指令 `daemon` 对应的 `ngx_command_s` 结构体可以看到，其 `set` 回调函数指向的是函数 `ngx_conf_set_flag_slot` ，该函数的原型如下：

`char * ngx_conf_set_flag_slot(ngx_conf_t *cf, ngx_command_t *cmd, void *conf);`

这是一个公共的解析函数，即它并不是单独为解析 `daemon` 配置指令而存在，而是对于所有 `NGX_CONF_FLAG` 类型的配置项都是用的该函数来进行解析。

```
char *
ngx_conf_set_flag_slot(ngx_conf_t *cf, ngx_command_t *cmd, void *conf)
{
    char  *p = conf;
    ngx_str_t        *value;
    ngx_flag_t       *fp;
    ngx_conf_post_t  *post;
    
    /* 解析出来的对应值存放的内存位置 */
    fp = (ngx_flag_t *) (p + cmd->offset);
    
    /* 该内存位置已有值，故知配置指令重复 */
    if (*fp != NGX_CONF_UNSET) {
        return "is duplicate";
    }
    
    /* cf->args存放的是与当前处理配置项相关的各个token，
        比如解析daemon配置指令时， 
        cf->args内的数据详细如下，以便于理解（通过gdb调试获得的结果）：
    (gdb) p *cf->args
    $1 = {elts = 0x9a0c798, nelts = 2, size = 8, nalloc = 10, pool = 0x9a0bf00}
    (gdb) p *(ngx_str_t*)(cf->args->elts)
    $2 = {len = 6, data = 0x9a0c7e8 "daemon"}
    (gdb) p *(((ngx_str_t*)(cf->args->elts)+1))
    $3 = {len = 3, data = 0x9a0c7f0 "off"}
    */
    
    value = cf->args->elts;
    
    /* 解析，布尔值的配置很好解析，"on"转为nginx内的1，"off"转为0。*/
    if (ngx_strcasecmp(value[1].data, (u_char *) "on") == 0) {
        *fp = 1;
    } else if (ngx_strcasecmp(value[1].data, (u_char *) "off") == 0) {
        *fp = 0;
    } else {   /* 出错提示 */
        ngx_conf_log_error(NGX_LOG_EMERG, cf, 0,
            "invalid value \"%s\" in \"%s\" directive, "
            "it must be \"on\" or \"off\"",
            value[1].data, cmd->name.data);
            return NGX_CONF_ERROR;
    }
    
    /* 其它处理函数，
        对于daemon配置指令来说为NULL，
        但是对于其它指令，比如optimize_server_names则还需调用自定义的处理。
    */
    if (cmd->post) {
        post = cmd->post;
        return post->post_handler(cf, post, fp);
    }
    
    return NGX_CONF_OK;
}
```

对于Nginx配置文件的解析流程基本就是如此，上面的介绍忽略了很多细节，前面也说过，事实上对于配置信息解析的代码(即各种各样的回调函数 `cmd->set` 的具体实现)占去了Nginx大幅的源代码，而我们这里并没有做过多的分析，仅例举了 `daemon` 配置指令的解析过程，因为对于不同的配置项，解析代码完全是根据自身应用而不同的，当然，除了一些可公共出来的代码以外。最后，看一个Nginx配置文件解析的流程图，如下：

![流程图](/2018-02-06-nginx-note.dir/nginx_config_parsing_procedure_07.png)

备注：如无特殊说明，文章内容均出自Lenky个人的真实理解而并非存心妄自揣测来故意愚人耳目。由于个人水平有限，虽力求内容正确无误，但仍然难免出错，请勿见怪，如果可以则请留言告之，并欢迎来信讨论。另外值得说明的是，Lenky的部分文章以及部分内容参考借鉴了网络上各位网友的热心分享，特别是一些带有完全参考的文章，其后附带的链接内容也许更直接、更丰富，而我只是做了一下归纳&转述，在此也一并表示感谢。关于本站的所有技术文章，欢迎转载，但请遵从CC创作共享协议，而一些私人性质较强的心情随笔，建议不要转载。
