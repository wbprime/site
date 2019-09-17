+++
title = "Query Key-Frame Info from Video Stream using FFprobe"
description = "使用 ffprobe 获取视频中的关键帧信息命令如下：`ffprobe -loglevel error -hide_banner -select_streams v -skip_frame nokey -show_frames -show_entries frame=pict_type -of json=compact=1 input.avi`"
date = 2019-09-09T09:52:45+08:00
draft = false
template = "page.html"
[taxonomies]
categories =  ["Streaming"]
tags = ["ffprobe", "key-frame", "note"]
+++


使用 [ffprobe](https://ffmpeg.org/ffprobe.html) 获取视频中的关键帧信息命令如下：

```sh
ffprobe -loglevel error -hide_banner -select_streams v -skip_frame nokey -show_frames -show_entries frame=pict_type -of json=compact=1 input.avi
ffprobe -loglevel error -hide_banner -select_streams v -show_format -show_frames -show_entries frame=pict_t ype:format=filename,duration,size:format_tags= -show_streams -of json=compact=1 input.mp4
```

[ffprobe](https://ffmpeg.org/ffprobe.html) 的更多用法请参考[其文档](https://ffmpeg.org/ffprobe-all.html) 。

---

以上。
