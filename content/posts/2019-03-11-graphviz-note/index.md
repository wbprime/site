+++
title = "Graphviz Dot Note"
description = "Graphviz 之 设置输出图片的大小。"
date = 2019-03-11T21:27:50+08:00
draft = false
template = "page.html"
[taxonomies]
categories =  ["Graphviz"]
tags = ["graphviz", "dot", "note"]
+++

# 设置输出图片的大小

```
size = "8,8!"
dpi = "100"
```

以上设置会尽量输出一个长和宽的最大值为 800 pixel 的图片。其中，size 的单位为 inch，限制输出图片的最
大尺寸，如果以感叹号 "!" 结尾，表明需要将输出结果放到到不超过指定大小的大小；dpi 表示输出的 pixel
per inch。

如果，默认输出的图片长宽比为 4:3, 则应用以上设置会输出一个 800x600 的图片。

参见 Graphviz 文档之说明： [size](https://graphviz.gitlab.io/_pages/doc/info/attrs.html#d:size)
[dpi](https://graphviz.gitlab.io/_pages/doc/info/attrs.html#d:dpi)
[ratio](https://graphviz.gitlab.io/_pages/doc/info/attrs.html#d:ratio)

