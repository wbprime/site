+++
title = "Record video from Xorg using FFmpeg"
description = "使用 ffmpeg 在 X 下进行录屏，可以使用如下命令：`ffmpeg -f x11grab -video_size 1366x768 -framerate 24 -i :0.0+0,0 $(date +%Y%m%dT%H%M%S.%3N.avi)`"
date = 2019-09-09T09:43:41+08:00
draft = false
template = "page.html"
[taxonomies]
categories =  ["Streaming"]
tags = ["ffmpeg", "note"]
+++

使用 [ffmpeg](https://ffmpeg.org/ffmpeg.html) 在 X 下进行录屏，可以使用如下命令：

```sh
ffmpeg -f x11grab -video_size 1366x768 -framerate 24 -i :0.0+0,0 $(date +%Y%m%dT%H%M%S.%3N.avi)
```

使用了 [x11grap](https://ffmpeg.org/ffmpeg-devices.html#x11grab) 解码器。

参见 [Capturing your Desktop / Screen Recording](https://trac.ffmpeg.org/wiki/Capture/Desktop)

---

以上。
