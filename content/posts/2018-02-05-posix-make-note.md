---
title : "Posix & GNU Make Note"
date : 2018-02-05T18:00:50+08:00
categories : ["Notes"]
tags : ["make", "makefile"]
description : "Note on make and makefile"
draft : false
---

## Compile

Source code -> object file: "\*.c -> \*.o"

## Link

Object files -> executable: "\*.o -> a.out"

# makefile

## Rules

```
target : prerequisites
    command1
    command2
    ...
    commandn
```

- `target` is a target to be generated.  It can be a object file, or a executable file or a label.
- `prerequisites` are dependecies of `target`.  It is required to generate `target`.
- `commandn` are commands to generate `target`.  Lines started with a '\t'

If any of `prerequisites` is newer than `target`, then make will call `command1` to `commandn`.
If any of `prerequisites` is not existed, then make will call `command1` to `commandn`.

在默认的方式下，也就是我们只输入make命令。那么，

1. make会在当前目录下找名字叫“Makefile”或“makefile”的文件。
2. 如果找到，它会找文件中的第一个目标文件（target），在上面的例子中，他会找到“edit”这个文件，并把这个文件作为最终的目标文件。
3. 如果edit文件不存在，或是edit所依赖的后面的 .o 文件的文件修改时间要比edit这个文件新，那么，他就会执行后面所定义的命令来生成edit这个文件。
4. 如果edit所依赖的.o文件也存在，那么make会在当前文件中找目标为.o文件的依赖性，如果找到则再根据那一个规则生成.o文件。（这有点像一个堆栈的过程）
5. 当然，你的C文件和H文件是存在的啦，于是make会生成 .o 文件，然后再用 .o 文件生命make的终极任务，也就是执行文件edit了。

## Variables

`$<` : 所有的依赖目标集
`$@` : 目标集

## Variables Used by Built-in Rules

- `CC` -- the c compiler to use
- `CXX` -- the c++ compiler to use
- `LD` -- the linker to use
- `CFLAGS` -- compilation flag for c source files
- `CXXFLAGS` -- compilation flags for c++ source files
- `CPPFLAGS` -- flags for the c-preprocessor (typically include file paths and symbols defined on the command line), used by c and c++
- `LDFLAGS` -- linker flags
- `LDLIBS` -- libraries to link
