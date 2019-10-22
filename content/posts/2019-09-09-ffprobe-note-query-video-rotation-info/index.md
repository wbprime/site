+++
title = "Query Video Rotation Info using FFprobe"
description = "使用 ffprobe 获取原视频中的旋转信息的命令如下：`ffprobe -loglevel error -hide_banner -select_streams v -show_entries stream_tags=rotate -of json=compact=1 input.avi`"
date = 2019-09-09T09:53:05+08:00
draft = false
template = "page.html"
[taxonomies]
categories =  ["Streaming"]
tags = ["ffprobe", "rotation", "note"]
+++

有一些通过手持设备录制的视频会把视频的旋转信息作为一个额外的字段放入视频的元数据中（比如 iOS）；在对
这些视频进行处理的时候，其元数据中的宽高信息就需要使用旋转信息进行修正。

使用 [ffprobe](https://ffmpeg.org/ffprobe.html) 获取原视频中的旋转信息的命令如下：

```sh
ffprobe -loglevel error -hide_banner -select_streams v -show_entries stream_tags=rotate -of json=compact=1 input.avi
```

查询得到的视频旋转信息是一个整型的旋转角度值（而不是弧度）；取值范围为 [0, 360]，常见的取值有
90/180/270 。

假定一个原视频通过 [ffprobe](https://ffmpeg.org/ffprobe.html) 提取的宽高信息为 800x600，若旋转信息为 90，则实际的视频宽高为 600x800。

<!-- more -->

---

以上。
