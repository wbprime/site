+++
title = "Generating HLS/M3U8 Using FFmpeg"
description = "最近的项目中需要提供音视频多码率转码支持，集中调研了 FFmpeg 对 Apple HTTP Live Streaming (HLS) 的支持，总结一下，遂成本文。"
date = 2019-07-06T15:18:04+08:00
draft = false
template = "page.html"
[taxonomies]
categories =  ["Streaming"]
tags = ["ffmpeg", "hls", "m3u8"]
+++

[FFmpeg][ffmpeg] 是开源的音视频处理项目，以使用灵活、功能丰富著称，被各大互联网公司用来构建多媒体服务的基础。

最近的项目中需要提供音视频多码率转码支持，集中调研了 [FFmpeg][ffmpeg] 对 [Apple HTTP Live Streaming (HLS)](https://developer.apple.com/streaming/) 的支持，总结一下，遂成本文。

说明：本文中使用的命令行 `ffmpeg` 版本为：

> ```
>  ~  ffmpeg
> ffmpeg version n4.1.3 Copyright (c) 2000-2019 the FFmpeg developers
>   built with gcc 8.2.1 (GCC) 20181127
>   configuration: --prefix=/usr --disable-debug --disable-static --disable-stripping --enable-fontconfig --enable-gmp --enable-gnutls --enable-gpl --enable-ladspa --enable-libaom --enable-libass --enable-libbluray --enable-libdrm --enable-libfreetype --enable-libfribidi --enable-libgsm --enable-libiec61883 --enable-libjack --enable-libmodplug --enable-libmp3lame --enable-libopencore_amrnb --enable-libopencore_amrwb --enable-libopenjpeg --enable-libopus --enable-libpulse --enable-libsoxr --enable-libspeex --enable-libssh --enable-libtheora
> --enable-libv4l2 --enable-libvidstab --enable-libvorbis --enable-libvpx --enable-libwebp --enable-libx264 --enable-libx265 --enable-libxcb --enable-libxml2 --enable-libxvid --enable-nvdec --enable-nvenc --enable-omx --enable-shared --enable-version3
>   libavutil      56. 22.100 / 56. 22.100
>   libavcodec     58. 35.100 / 58. 35.100
>   libavformat    58. 20.100 / 58. 20.100
>   libavdevice    58.  5.100 / 58.  5.100
>   libavfilter     7. 40.101 /  7. 40.101
>   libswscale      5.  3.100 /  5.  3.100
>   libswresample   3.  3.100 /  3.  3.100
>   libpostproc    55.  3.100 / 55.  3.100
> ```

<!-- more -->

# `hls` Muxer

[FFmpeg][ffmpeg] 通过 [hls][ffmpeg_hls_muxer] 封装器格式可以支持 [HTTP Live Streaming (HLS)][hls] 输出。其会将原视频分割为多个 [MPEG-TS (.ts)][format_ts] 格式的片段文件，以及一个 [M3U8 (.m3u8)][format_m3u8] 格式的播放列表文件。

官方提供的示例：

```sh
ffmpeg -i in.mkv -c:v h264 -flags +cgop -g 30 -hls_time 1 out.m3u8
```

会生成一个播放列表文件 `out.m3u8`，以及多个视频分片文件 `out0.ts`, `out1.ts`, `out2.ts` 等。

另外，[FFmpeg][ffmpeg] 也提供了 [segment][ffmpeg_segment_muxer] 封装器格式可以生成 [HTTP Live Streaming (HLS)][hls] 格式的输出。

# 单码率 M3U8

## 起始

一个典型的生成 [HLS][hls] 输出的命令行如下：

```sh
ffmpeg -hide_banner -loglevel warning \
    -ss 10 -t 10 \
    -i test.avi \
    -c:v libx264 -crf 23 -preset veryfast \
    -c:a aac -b:a 128k -ac 2 \
    -f hls \
    -hls_time 4 \
    -hls_playlist_type vod \
    -hls_segment_filename hls.test%d.ts \
    -hls_list_size 0 \
    hls.test.m3u8
```

1. `-f hls` 行指定了输出格式为 `hls`，ffmpeg 会使用 [hls][ffmpeg_hls_muxer] 来对输出视频做封装操作。
2. `-hls_time 4` 行指定了输出视频片段的长度，期望输出的视频片段每一个的时长都是 4s。
3. `-hls_playlist_type vod` 行指定了播放列表的类型；`vod` 即点播，`event` 即直播。
4. `-hls_segment_filename hls.test%d.ts` 行指定了输出片段文件名格式，可以使用 `%d` 或 `%5d` 的格式化符
5. `-hls_list_size 0` 行指定了输出视频片段的最大个数；如果设置为 0，说明不限制个数。

该命令会生成以下文件：

- hls.test0.ts
- hls.test1.ts
- hls.test2.ts
- hls.test3.ts
- hls.test.m3u8

生成的 `hls.test.m3u8` 文件如下：

> ```
> #EXTM3U
> #EXT-X-VERSION:3
> #EXT-X-TARGETDURATION:7
> #EXT-X-MEDIA-SEQUENCE:0
> #EXT-X-PLAYLIST-TYPE:VOD
> #EXTINF:6.760000,
> hls.test0.ts
> #EXTINF:6.040000,
> hls.test1.ts
> #EXTINF:4.520000,
> hls.test2.ts
> #EXTINF:2.680000,
> hls.test3.ts
> #EXT-X-ENDLIST
> ```

注意到视频片段的长度并不是指定的 4s。这是因为 [FFmpeg][ffmpeg] 在生成片段时，总是会在满足时长要求条件的第一个关键帧 (I-frame) 处截断，而不是严格按照时长来截断的。

[Demo Script](m3u8.simple.01.sh)

## 指定 GOP

如果能够使得视频在指定的时间间隔处存在关键帧，则输出的片段时长都能保持一致。可以通过 `-g` 参数来设置 GOP (相邻的两个关键帧之间的帧数）。

新的命令如下：

```sh
ffmpeg -hide_banner -loglevel warning \
    -ss 10 -t 10 \
    -i test.avi \
    -c:v libx264 -crf 23 -preset veryfast \
    -g 30 \
    -c:a aac -b:a 128k -ac 2 \
    -f hls \
    -hls_time 4 \
    -hls_playlist_type vod \
    -hls_segment_filename hls.test%d.ts \
    -hls_list_size 0 \
    hls.test.m3u8
```

生成的文件如下：

> ```
> #EXTM3U
> #EXT-X-VERSION:3
> #EXT-X-TARGETDURATION:5
> #EXT-X-MEDIA-SEQUENCE:0
> #EXT-X-PLAYLIST-TYPE:VOD
> #EXTINF:4.280000,
> hls.test0.ts
> #EXTINF:4.800000,
> hls.test1.ts
> #EXTINF:3.600000,
> hls.test2.ts
> #EXTINF:3.720000,
> hls.test3.ts
> #EXTINF:3.600000,
> hls.test4.ts
> #EXT-X-ENDLIST
> ```

片段的时长和目标时长接近了很多，但还是有一些差异。

[Demo Script](m3u8.simple.02.sh)

## 取消运动场景检测

[FFmpeg][ffmpeg] 在编码时进行运动检测。当检测到运动场景变换时，其会自动插入一帧关键帧；该行为可以通过 `-sc_threshold` 禁用。

新命令如下：

```sh
ffmpeg -hide_banner -loglevel warning \
    -ss 10 -t 10 \
    -i test.avi \
    -c:v libx264 -crf 23 -preset veryfast \
    -g 30 \
    -sc_threshold 0 \
    -c:a aac -b:a 128k -ac 2 \
    -f hls \
    -hls_time 4 \
    -hls_playlist_type vod \
    -hls_segment_filename hls.test%d.ts \
    -hls_list_size 0 \
    hls.test.m3u8
```

新的播放列表如下：

> ```
> #EXTM3U
> #EXT-X-VERSION:3
> #EXT-X-TARGETDURATION:5
> #EXT-X-MEDIA-SEQUENCE:0
> #EXT-X-PLAYLIST-TYPE:VOD
> #EXTINF:4.800000,
> hls.test0.ts
> #EXTINF:3.600000,
> hls.test1.ts
> #EXTINF:3.600000,
> hls.test2.ts
> #EXTINF:4.800000,
> hls.test3.ts
> #EXTINF:3.200000,
> hls.test4.ts
> #EXT-X-ENDLIST
> ```

[Demo Script](m3u8.simple.03.sh)

## closed GOP

对于视频分片来说，通常还需要设置 *closed GOP* 模式，防止视频帧去引用 GOP 之外的帧。

[FFmpeg][ffmpeg] 中通过 `-flags +cgop` 选项设置 *closed GOP* 模式。

```
ffmpeg -hide_banner -loglevel warning \
    -ss 10 -t 10 \
    -i test.avi \
    -c:v libx264 -crf 23 -preset veryfast \
    -g 30 \
    -sc_threshold 0 \
    -c:a aac -b:a 128k -ac 2 \
    -f hls \
    -hls_time 4 \
    -hls_playlist_type vod \
    -hls_segment_filename hls.test%d.ts \
    -hls_list_size 0 \
    hls.test.m3u8
```

[Demo Script](m3u8.simple.04.sh)

# 多码率 M3U8

## 多码率

[FFmpeg][ffmpeg] 可以直接支持对多码率 [HLS][hls] 输出的支持。其在生成多个单码率播放列表之外，还会生成一个层级播放列表文件。

命令如下：

```sh
ffmpeg -hide_banner -loglevel warning \
    -ss 10 -t 10 \
    -i test.avi \
    -g 30 \
    -sc_threshold 0 \
    -c:a aac -b:a 128k -ac 2 \
    -f hls \
    -hls_time 4 \
    -hls_playlist_type event \
    -hls_segment_filename hls.test%d.%v.ts \
    -hls_list_size 0 \
    -map v:0 -c:v:0 libx264 -b:v:0 2000k \
    -map v:0 -c:v:1 libx264 -b:v:1 6000k \
    -map a:0 \
    -map a:0 \
    -var_stream_map "v:0,a:0 v:1,a:1" \
    -master_pl_name hls.test.m3u8 \
    hls.test.m3u8
```

说明如下：

- `-var_stream_map "v:0,a:0 v:1,a:1"` 行通过逗号和空格分隔的参数指定输出两个码率的播放列表，并指定第一个播放列表对应的视频从第一个视频流和第一个音频流生成，第二个播放列表对应的视频片段从第二个视频流和第二个音频流生成；使用本参数之后，`-hls_segment_filename` 选项需要添加一个 `%v` 的占位符，表示第几个播放列表。
- `-master_pl_name hls.test.master.m3u8` 行指定输出的层级播放列表文件名

注意，由于需要输出多码率，所以在输出参数需要通过 `-map` 参数准备多个输出流。

另外，在本文使用的ffmpeg版本中，`-hls_playlist_type` 需要设置为 `event`，否则会报 `core dump` 异常。

生成的层级播放列表文件内容如下：

> ```
> #EXTM3U
> #EXT-X-VERSION:3
> #EXT-X-STREAM-INF:BANDWIDTH=2340800,RESOLUTION=1280x720,CODECS="avc1.64001f,mp4a.40.2"
> hls.test.0.m3u8
>
> #EXT-X-STREAM-INF:BANDWIDTH=6740800,RESOLUTION=1280x720,CODECS="avc1.64001f,mp4a.40.2"
> hls.test.1.m3u8
> ```

对于多码率的选择，可以参考 [Apple 的推荐码率](https://developer.apple.com/documentation/http_live_streaming/hls_authoring_specification_for_apple_devices)。

[Demo Script](m3u8.multi.01.sh)

## 多分辨率

如果需要输出的多码率视频匹配多个分辨率，可以使用 `-complex_filter` 组装过滤器图来实现。

命令如下：

```sh
ffmpeg -hide_banner -loglevel warning \
    -ss 10 -t 10 \
    -i test.avi \
    -filter_complex "[v:0]split=2[vtemp001][vout002];[vtemp001]scale=w=960:h=540[vout001]" \
    -g 30 \
    -sc_threshold 0 \
    -c:a aac -b:a 128k -ac 2 \
    -f hls \
    -hls_time 4 \
    -hls_playlist_type event \
    -hls_segment_filename hls.test%d.%v.ts \
    -hls_list_size 0 \
    -map "[vout001]" -c:v:0 libx264 -b:v:0 2000k \
    -map "[vout002]" -c:v:1 libx264 -b:v:1 6000k \
    -map a:0 \
    -map a:0 \
    -var_stream_map "v:0,a:0 v:1,a:1" \
    -master_pl_name hls.test.m3u8 \
    hls.test.m3u8
```

注意，过滤器图需要生成多个输出，`-map` 行需要与对应的输出匹配。

生成的播放列表文件内容如下：

> ```
> #EXTM3U
> #EXT-X-VERSION:3
> #EXT-X-STREAM-INF:BANDWIDTH=2340800,RESOLUTION=960x540,CODECS="avc1.64001f,mp4a.40.2"
> hls.test.0.m3u8
>
> #EXT-X-STREAM-INF:BANDWIDTH=6740800,RESOLUTION=1280x720,CODECS="avc1.64001f,mp4a.40.2"
> hls.test.1.m3u8
> ```

[Demo Script](m3u8.multi.02.sh)

# FFmpeg 3.x

遗憾的是，`-var_stream_map` 选项是在 [FFmpeg][ffmpeg] 4.x 版本以后添加的功能，在 3.x 版本中不被支持。

解决办法 <s>删掉 3.x 安装 4.x</s> (\_^\_^\_)

我采用的方法是多输出然后手动合并。

首先执行：

```sh
ffmpeg -hide_banner -loglevel warning \
    -ss 10 -t 10 \
    -i test.avi \
    -filter_complex "[v:0]split=2[vtemp001][vout002];[vtemp001]scale=w=960:h=540[vout001]" \
    -g 30 \
    -sc_threshold 0 \
    -c:a aac -b:a 128k -ac 2 \
    -f hls \
    -hls_time 4 \
    -hls_playlist_type event \
    -hls_segment_filename hls.test%d.0.ts \
    -hls_list_size 0 \
    -map "[vout001]" -c:v:0 libx264 -b:v:0 2000k \
    -map a:0 \
    hls.test.0.m3u8 \
    -g 30 \
    -sc_threshold 0 \
    -c:a aac -b:a 128k -ac 2 \
    -f hls \
    -hls_time 4 \
    -hls_playlist_type event \
    -hls_segment_filename hls.test%d.1.ts \
    -hls_list_size 0 \
    -map "[vout002]" -c:v:1 libx264 -b:v:1 6000k \
    -map a:0 \
    hls.test.1.m3u8
```

然后，使用 `ffprobe` 遍历每一个码率的片段文件，获取最大码率和分辨率之后手动生成层级播放列表文件。

[Demo Script](m3u8.multi.03.sh)

以上！

参考：

- [Using FFmpeg as a HLS streaming server (Part 1) – HLS Basics](https://www.martin-riedl.de/2018/08/24/using-ffmpeg-as-a-hls-streaming-server-part-1/)
- [Using FFmpeg as a HLS streaming server (Part 2) – Enhanced HLS Segmentation](https://www.martin-riedl.de/2018/08/24/using-ffmpeg-as-a-hls-streaming-server-part-2/)
- [Using FFmpeg as a HLS streaming server (Part 3) – Multiple Bitrates](https://www.martin-riedl.de/2018/08/25/using-ffmpeg-as-a-hls-streaming-server-part-3/)
- [Using FFmpeg as a HLS streaming server (Part 4) – Multiple Video Resolutions](https://www.martin-riedl.de/2018/08/26/using-ffmpeg-as-a-hls-streaming-server-part-4-multiple-video-resolutions/)
- [Using FFmpeg as a HLS streaming server (Part 5) – Folder Structure](https://www.martin-riedl.de/2018/08/30/using-ffmpeg-as-a-hls-streaming-server-part-5-folder-structure/)

[ffmpeg]: http://ffmpeg.org/ "FFmpeg A complete, cross-platform solution to record, convert and stream audio and video."
[hls]: https://tools.ietf.org/html/draft-pantos-http-live-streaming-06 "HTTP Live Streaming draft-pantos-http-live-streaming-06"
[ffmpeg_segment_muxer]: http://ffmpeg.org/ffmpeg-formats.html#segment_002c-stream_005fsegment_002c-ssegment
[ffmpeg_hls_muxer]: http://ffmpeg.org/ffmpeg-formats.html#hls-2
[format_ts]: https://en.wikipedia.org/wiki/MPEG_transport_stream "MPEG transport stream"
[format_m3u8]: https://en.wikipedia.org/wiki/M3U "M3U"
