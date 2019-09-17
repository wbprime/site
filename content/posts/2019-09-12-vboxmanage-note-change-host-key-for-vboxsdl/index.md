+++
title = "Change Host Key for VBOXSDL"
description = "如果平时使用 Linux，偶尔会有使用 Windows 软件（如 WPS/QQ 等）的需求场景时，最好的解决方案就是使用虚拟机了。如果使用了 VirtualBox 的话，那么可以了解一下其除了基于 Qt 的虚拟机运行界面之外还额外提供了一个基于 SDL 的虚拟机运行界面。如果需要修改 SDL 界面下的 host 键，可以使用参数 `--hostkey`。"
date = 2019-09-12T17:39:15+08:00
draft = false
template = "page.html"
[taxonomies]
categories =  ["Utils"]
tags = ["virtualbox", "vboxsdl", "host-key"]
+++

如果平时使用 Linux，偶尔会有使用 Windows 软件（如 WPS/QQ 等）的需求场景时，最好的解决方案就是使用虚拟机了。如果使用了 [Virtual Box](https://www.virtualbox.org/) 的话，那么可以了解一下其除了基于 [Qt](https://www.qt.io/) 的虚拟机运行界面之外还额外提供了一个基于 [SDL](https://www.libsdl.org/) 的虚拟机运行界面。

我在使用 virtualbox 的管理界面创建配置好一个基于 Windows 7 的虚拟机 "se7en" 之后，可以直接在终端运行：

```sh
${VBOXSDL} --startvm se7en
```

界面非常简洁，不依赖 [Qt](https://www.qt.io/) 。

`${VBOXSDL}` 是 vboxsdl 的可执行文件名；根据 VirtualBox 的安装方式不同，可能会是 "VBoxSDL" 或 "vboxsdl"。

如果需要修改 SDL 界面下的 host 键，可以使用参数 "--hostkey"。

```sh
# Set Host key for vboxsdl
# LSHIFT = 304 1
# RSHIFT = 303 2
# LCTRL = 301 8192
# RCTRL = 305 128
# LALT = 308 256
# RALT = 27 0
${VBOXSDL} --hostkey 308 256 --startvm se7en
```

该命令会设置 host 键为 左-Alt 键并启动虚拟机。

---

以上。
