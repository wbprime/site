+++
title = "Ffmpeg -vsync 使用场景辨析"
description = "Ffmpeg -vsync option explained"
date = 2019-07-21T18:18:28+08:00
draft = false
template = "page.html"
[taxonomies]
categories =  ["Streaming"]
tags = ["ffmpeg", "vsync", "fps"]
+++

在使用 [Ffmpeg][ffmpeg] 进行视频抽帧时，发现 `-vsync` 参数的意思理解的不是很清楚。本文通过不同的场景
使用结果对比来辨析 `-vsync` 选项的枚举参数值的区别。

以下是 [Ffmpeg][ffmpeg] 官网上关于 `-vsync` 的全部说明：

> ```
> -vsync parameter
>
>     Video sync method. For compatibility reasons old values can be specified as numbers. Newly added values will have to be specified as strings always.
>
>     0, passthrough
>         Each frame is passed with its timestamp from the demuxer to the muxer.
>     1, cfr
>         Frames will be duplicated and dropped to achieve exactly the requested constant frame rate.
>     2, vfr
>         Frames are passed through with their timestamp or dropped so as to prevent 2 frames from having the same timestamp.
>     drop
>         As passthrough but destroys all timestamps, making the muxer generate fresh timestamps based on frame-rate.
>     -1, auto
>         Chooses between 1 and 2 depending on muxer capabilities. This is the default method.
>
>     Note that the timestamps may be further modified by the muxer, after this. For example, in the case that the format option avoid_negative_ts is enabled.
>
>     With -map you can select from which stream the timestamps should be taken. You can leave either video or audio unchanged and sync the remaining stream(s) to the unchanged one.
> ```

<!-- more -->

# 字面理解

简单翻译一下官方说明：

- *passthrough* 每一帧从解码器到编码器，时间戳保持不变
- *cfr* 如果指定了输出帧率，输入帧会按照需要进行复制（如果输出帧率大于输入帧率）或丢弃（如果输出帧率小于输入帧率）
- *vfr* 输入帧从解码器到编码器，时间戳保持不变；如果出现相同时间戳的帧，则丢弃之
- *drop* 同 *passthrough*，但将所有帧的时间戳清空

# Case 分析

使用 [Blender - Big Buck Bunny](https://peach.blender.org/) 网站提供的视频进行 case 分析，其 `ffprobe` 输出信息如下：

```json
{
  "streams": [
    {
      "index": 0,
      "codec_name": "h264",
      "codec_long_name": "H.264 / AVC / MPEG-4 AVC / MPEG-4 part 10",
      "profile": "High",
      "codec_type": "video",
      "codec_time_base": "1/60",
      "codec_tag_string": "avc1",
      "codec_tag": "0x31637661",
      "width": 1920,
      "height": 1080,
      "coded_width": 1920,
      "coded_height": 1088,
      "has_b_frames": 2,
      "sample_aspect_ratio": "1:1",
      "display_aspect_ratio": "16:9",
      "pix_fmt": "yuv420p",
      "level": 40,
      "chroma_location": "left",
      "refs": 1,
      "is_avc": "true",
      "nal_length_size": "4",
      "r_frame_rate": "30/1",
      "avg_frame_rate": "30/1",
      "time_base": "1/15360",
      "start_pts": 0,
      "start_time": "0.000000",
      "duration_ts": 9747456,
      "duration": "634.600000",
      "bit_rate": "2736521",
      "bits_per_raw_sample": "8",
      "nb_frames": "19038",
      "disposition": {
        "default": 1,
        "dub": 0,
        "original": 0,
        "comment": 0,
        "lyrics": 0,
        "karaoke": 0,
        "forced": 0,
        "hearing_impaired": 0,
        "visual_impaired": 0,
        "clean_effects": 0,
        "attached_pic": 0,
        "timed_thumbnails": 0
      },
      "tags": {
        "language": "und",
        "handler_name": "GPAC ISO Video Handler"
      }
    },
    {
      "index": 1,
      "codec_name": "ac3",
      "codec_long_name": "ATSC A/52A (AC-3)",
      "codec_type": "audio",
      "codec_time_base": "1/48000",
      "codec_tag_string": "ac-3",
      "codec_tag": "0x332d6361",
      "sample_fmt": "fltp",
      "sample_rate": "48000",
      "channels": 6,
      "channel_layout": "5.1(side)",
      "bits_per_sample": 0,
      "dmix_mode": "-1",
      "ltrt_cmixlev": "-1.000000",
      "ltrt_surmixlev": "-1.000000",
      "loro_cmixlev": "-1.000000",
      "loro_surmixlev": "-1.000000",
      "r_frame_rate": "0/0",
      "avg_frame_rate": "0/0",
      "time_base": "1/48000",
      "start_pts": 0,
      "start_time": "0.000000",
      "duration_ts": 30438912,
      "duration": "634.144000",
      "bit_rate": "320000",
      "nb_frames": "19556",
      "disposition": {
        "default": 1,
        "dub": 0,
        "original": 0,
        "comment": 0,
        "lyrics": 0,
        "karaoke": 0,
        "forced": 0,
        "hearing_impaired": 0,
        "visual_impaired": 0,
        "clean_effects": 0,
        "attached_pic": 0,
        "timed_thumbnails": 0
      },
      "tags": {
        "language": "und",
        "handler_name": "GPAC ISO Audio Handler"
      },
      "side_data_list": [
        {
          "side_data_type": "Audio Service Type"
        }
      ]
    }
  ],
  "format": {
    "filename": "big_buck_bunny.1920x1080.30fps.mp4",
    "nb_streams": 2,
    "nb_programs": 0,
    "format_name": "mov,mp4,m4a,3gp,3g2,mj2",
    "format_long_name": "QuickTime / MOV",
    "start_time": "0.000000",
    "duration": "634.600000",
    "size": "242901742",
    "bit_rate": "3062108",
    "probe_score": 100,
    "tags": {
      "major_brand": "isom",
      "minor_version": "512",
      "compatible_brands": "isomiso2avc1mp41",
      "title": "Big Buck Bunny, Sunflower version",
      "artist": "Blender Foundation 2008, Janus Bager Kristensen 2013",
      "composer": "Sacha Goedegebure",
      "encoder": "Lavf58.20.100",
      "comment": "Creative Commons Attribution 3.0 - http://bbb3d.renderfarming.net",
      "genre": "Animation"
    }
  }
}
```

## 应用于转码

对输入视频进行转码，为 4 个 `-vsync` 枚举值分别生成两个输出视频，其中一个输出视频的帧率大于输入帧率，另一个小于。

即对于输入帧率为 `30/1`，分别按以下参数进行转码：

- `-vsync passthrough -r 24`
- `-vsync passthrough -r 45`
- `-vsync cfr -r 24`
- `-vsync cfr -r 45`
- `-vsync vfr -r 24`
- `-vsync vfr -r 45`
- `-vsync drop -r 24`
- `-vsync drop -r 45`

详见 [转码脚本](vsync_transcode.sh) 和 [辅助脚本](vsync_ffprobe.sh)。

执行结果如下：

| Vsync Mode  | FPS Mode | Real Output FPS |
|-------------|----------|-----------------|
| cfr         | 24       | 24              |
| cfr         | 45       | 45              |
| drop        | 24       | FAILED          |
| drop        | 45       | FAILED          |
| passthrough | 24       | 24              |
| passthrough | 45       | 45              |
| vfr         | 24       | 24              |
| vfr         | 45       | 45              |

对于 `drop` 模式，命令执行失败。

## 应用于抽帧

对输入视频进行抽帧，为 4 个 `-vsync` 枚举值分别生成两组输出，其中一个输出的帧率大于输入帧率，另一个小于。

即对于输入帧率为 `30/1`，分别按以下参数进行转码：

- `-vsync passthrough -r 24 -frame_pts true`
- `-vsync passthrough -r 45 -frame_pts true`
- `-vsync cfr -r 24 -frame_pts true`
- `-vsync cfr -r 45 -frame_pts true`
- `-vsync vfr -r 24 -frame_pts true`
- `-vsync vfr -r 45 -frame_pts true`
- `-vsync drop -r 24 -frame_pts true`
- `-vsync drop -r 45 -frame_pts true`

添加了 `-frame_pts` 选项以从输出图片的文件名中观察对应帧的 PTS。

详见 [抽帧脚本](vsync_snapshot.sh)。

执行结果如下：

| Vsync Mode  | FPS Mode | Count |
|-------------|----------|-------|
| cfr         | 24       | 50    |
| cfr         | 45       | 91    |
| drop        | 24       | 60    |
| drop        | 45       | 60    |
| passthrough | 24       | 48    |
| passthrough | 45       | 60    |
| vfr         | 24       | 49    |
| vfr         | 45       | 60    |

生成的对应图片文件名和大小如下：

| File Name                          |
|------------------------------------|
| output.cfr.fps24.00000.png         |
| output.cfr.fps24.00001.png         |
| output.cfr.fps24.00002.png         |
| output.cfr.fps24.00003.png         |
| output.cfr.fps24.00004.png         |
| output.cfr.fps24.00005.png         |
| output.cfr.fps24.00006.png         |
| output.cfr.fps24.00007.png         |
| output.cfr.fps24.00008.png         |
| output.cfr.fps24.00009.png         |
| output.cfr.fps24.00010.png         |
| output.cfr.fps24.00011.png         |
| output.cfr.fps24.00012.png         |
| output.cfr.fps24.00013.png         |
| output.cfr.fps24.00014.png         |
| output.cfr.fps24.00015.png         |
| output.cfr.fps24.00016.png         |
| output.cfr.fps24.00017.png         |
| output.cfr.fps24.00018.png         |
| output.cfr.fps24.00019.png         |
| output.cfr.fps24.00020.png         |
| output.cfr.fps24.00021.png         |
| output.cfr.fps24.00022.png         |
| output.cfr.fps24.00023.png         |
| output.cfr.fps24.00024.png         |
| output.cfr.fps24.00025.png         |
| output.cfr.fps24.00026.png         |
| output.cfr.fps24.00027.png         |
| output.cfr.fps24.00028.png         |
| output.cfr.fps24.00029.png         |
| output.cfr.fps24.00030.png         |
| output.cfr.fps24.00031.png         |
| output.cfr.fps24.00032.png         |
| output.cfr.fps24.00033.png         |
| output.cfr.fps24.00034.png         |
| output.cfr.fps24.00035.png         |
| output.cfr.fps24.00036.png         |
| output.cfr.fps24.00037.png         |
| output.cfr.fps24.00038.png         |
| output.cfr.fps24.00039.png         |
| output.cfr.fps24.00040.png         |
| output.cfr.fps24.00041.png         |
| output.cfr.fps24.00042.png         |
| output.cfr.fps24.00043.png         |
| output.cfr.fps24.00044.png         |
| output.cfr.fps24.00045.png         |
| output.cfr.fps24.00046.png         |
| output.cfr.fps24.00047.png         |
| output.cfr.fps24.00048.png         |
| output.cfr.fps24.00049.png         |
| output.cfr.fps45.00000.png         |
| output.cfr.fps45.00001.png         |
| output.cfr.fps45.00002.png         |
| output.cfr.fps45.00003.png         |
| output.cfr.fps45.00004.png         |
| output.cfr.fps45.00005.png         |
| output.cfr.fps45.00006.png         |
| output.cfr.fps45.00007.png         |
| output.cfr.fps45.00008.png         |
| output.cfr.fps45.00009.png         |
| output.cfr.fps45.00010.png         |
| output.cfr.fps45.00011.png         |
| output.cfr.fps45.00012.png         |
| output.cfr.fps45.00013.png         |
| output.cfr.fps45.00014.png         |
| output.cfr.fps45.00015.png         |
| output.cfr.fps45.00016.png         |
| output.cfr.fps45.00017.png         |
| output.cfr.fps45.00018.png         |
| output.cfr.fps45.00019.png         |
| output.cfr.fps45.00020.png         |
| output.cfr.fps45.00021.png         |
| output.cfr.fps45.00022.png         |
| output.cfr.fps45.00023.png         |
| output.cfr.fps45.00024.png         |
| output.cfr.fps45.00025.png         |
| output.cfr.fps45.00026.png         |
| output.cfr.fps45.00027.png         |
| output.cfr.fps45.00028.png         |
| output.cfr.fps45.00029.png         |
| output.cfr.fps45.00030.png         |
| output.cfr.fps45.00031.png         |
| output.cfr.fps45.00032.png         |
| output.cfr.fps45.00033.png         |
| output.cfr.fps45.00034.png         |
| output.cfr.fps45.00035.png         |
| output.cfr.fps45.00036.png         |
| output.cfr.fps45.00037.png         |
| output.cfr.fps45.00038.png         |
| output.cfr.fps45.00039.png         |
| output.cfr.fps45.00040.png         |
| output.cfr.fps45.00041.png         |
| output.cfr.fps45.00042.png         |
| output.cfr.fps45.00043.png         |
| output.cfr.fps45.00044.png         |
| output.cfr.fps45.00045.png         |
| output.cfr.fps45.00046.png         |
| output.cfr.fps45.00047.png         |
| output.cfr.fps45.00048.png         |
| output.cfr.fps45.00049.png         |
| output.cfr.fps45.00050.png         |
| output.cfr.fps45.00051.png         |
| output.cfr.fps45.00052.png         |
| output.cfr.fps45.00053.png         |
| output.cfr.fps45.00054.png         |
| output.cfr.fps45.00055.png         |
| output.cfr.fps45.00056.png         |
| output.cfr.fps45.00057.png         |
| output.cfr.fps45.00058.png         |
| output.cfr.fps45.00059.png         |
| output.cfr.fps45.00060.png         |
| output.cfr.fps45.00061.png         |
| output.cfr.fps45.00062.png         |
| output.cfr.fps45.00063.png         |
| output.cfr.fps45.00064.png         |
| output.cfr.fps45.00065.png         |
| output.cfr.fps45.00066.png         |
| output.cfr.fps45.00067.png         |
| output.cfr.fps45.00068.png         |
| output.cfr.fps45.00069.png         |
| output.cfr.fps45.00070.png         |
| output.cfr.fps45.00071.png         |
| output.cfr.fps45.00072.png         |
| output.cfr.fps45.00073.png         |
| output.cfr.fps45.00074.png         |
| output.cfr.fps45.00075.png         |
| output.cfr.fps45.00076.png         |
| output.cfr.fps45.00077.png         |
| output.cfr.fps45.00078.png         |
| output.cfr.fps45.00079.png         |
| output.cfr.fps45.00080.png         |
| output.cfr.fps45.00081.png         |
| output.cfr.fps45.00082.png         |
| output.cfr.fps45.00083.png         |
| output.cfr.fps45.00084.png         |
| output.cfr.fps45.00085.png         |
| output.cfr.fps45.00086.png         |
| output.cfr.fps45.00087.png         |
| output.cfr.fps45.00088.png         |
| output.cfr.fps45.00089.png         |
| output.cfr.fps45.00090.png         |
| output.drop.fps24.00000.png        |
| output.drop.fps24.00001.png        |
| output.drop.fps24.00002.png        |
| output.drop.fps24.00003.png        |
| output.drop.fps24.00004.png        |
| output.drop.fps24.00005.png        |
| output.drop.fps24.00006.png        |
| output.drop.fps24.00007.png        |
| output.drop.fps24.00008.png        |
| output.drop.fps24.00009.png        |
| output.drop.fps24.00010.png        |
| output.drop.fps24.00011.png        |
| output.drop.fps24.00012.png        |
| output.drop.fps24.00013.png        |
| output.drop.fps24.00014.png        |
| output.drop.fps24.00015.png        |
| output.drop.fps24.00016.png        |
| output.drop.fps24.00017.png        |
| output.drop.fps24.00018.png        |
| output.drop.fps24.00019.png        |
| output.drop.fps24.00020.png        |
| output.drop.fps24.00021.png        |
| output.drop.fps24.00022.png        |
| output.drop.fps24.00023.png        |
| output.drop.fps24.00024.png        |
| output.drop.fps24.00025.png        |
| output.drop.fps24.00026.png        |
| output.drop.fps24.00027.png        |
| output.drop.fps24.00028.png        |
| output.drop.fps24.00029.png        |
| output.drop.fps24.00030.png        |
| output.drop.fps24.00031.png        |
| output.drop.fps24.00032.png        |
| output.drop.fps24.00033.png        |
| output.drop.fps24.00034.png        |
| output.drop.fps24.00035.png        |
| output.drop.fps24.00036.png        |
| output.drop.fps24.00037.png        |
| output.drop.fps24.00038.png        |
| output.drop.fps24.00039.png        |
| output.drop.fps24.00040.png        |
| output.drop.fps24.00041.png        |
| output.drop.fps24.00042.png        |
| output.drop.fps24.00043.png        |
| output.drop.fps24.00044.png        |
| output.drop.fps24.00045.png        |
| output.drop.fps24.00046.png        |
| output.drop.fps24.00047.png        |
| output.drop.fps24.00048.png        |
| output.drop.fps24.00049.png        |
| output.drop.fps24.00050.png        |
| output.drop.fps24.00051.png        |
| output.drop.fps24.00052.png        |
| output.drop.fps24.00053.png        |
| output.drop.fps24.00054.png        |
| output.drop.fps24.00055.png        |
| output.drop.fps24.00056.png        |
| output.drop.fps24.00057.png        |
| output.drop.fps24.00058.png        |
| output.drop.fps24.00059.png        |
| output.drop.fps45.00000.png        |
| output.drop.fps45.00001.png        |
| output.drop.fps45.00002.png        |
| output.drop.fps45.00003.png        |
| output.drop.fps45.00004.png        |
| output.drop.fps45.00005.png        |
| output.drop.fps45.00006.png        |
| output.drop.fps45.00007.png        |
| output.drop.fps45.00008.png        |
| output.drop.fps45.00009.png        |
| output.drop.fps45.00010.png        |
| output.drop.fps45.00011.png        |
| output.drop.fps45.00012.png        |
| output.drop.fps45.00013.png        |
| output.drop.fps45.00014.png        |
| output.drop.fps45.00015.png        |
| output.drop.fps45.00016.png        |
| output.drop.fps45.00017.png        |
| output.drop.fps45.00018.png        |
| output.drop.fps45.00019.png        |
| output.drop.fps45.00020.png        |
| output.drop.fps45.00021.png        |
| output.drop.fps45.00022.png        |
| output.drop.fps45.00023.png        |
| output.drop.fps45.00024.png        |
| output.drop.fps45.00025.png        |
| output.drop.fps45.00026.png        |
| output.drop.fps45.00027.png        |
| output.drop.fps45.00028.png        |
| output.drop.fps45.00029.png        |
| output.drop.fps45.00030.png        |
| output.drop.fps45.00031.png        |
| output.drop.fps45.00032.png        |
| output.drop.fps45.00033.png        |
| output.drop.fps45.00034.png        |
| output.drop.fps45.00035.png        |
| output.drop.fps45.00036.png        |
| output.drop.fps45.00037.png        |
| output.drop.fps45.00038.png        |
| output.drop.fps45.00039.png        |
| output.drop.fps45.00040.png        |
| output.drop.fps45.00041.png        |
| output.drop.fps45.00042.png        |
| output.drop.fps45.00043.png        |
| output.drop.fps45.00044.png        |
| output.drop.fps45.00045.png        |
| output.drop.fps45.00046.png        |
| output.drop.fps45.00047.png        |
| output.drop.fps45.00048.png        |
| output.drop.fps45.00049.png        |
| output.drop.fps45.00050.png        |
| output.drop.fps45.00051.png        |
| output.drop.fps45.00052.png        |
| output.drop.fps45.00053.png        |
| output.drop.fps45.00054.png        |
| output.drop.fps45.00055.png        |
| output.drop.fps45.00056.png        |
| output.drop.fps45.00057.png        |
| output.drop.fps45.00058.png        |
| output.drop.fps45.00059.png        |
| output.passthrough.fps24.00000.png |
| output.passthrough.fps24.00001.png |
| output.passthrough.fps24.00002.png |
| output.passthrough.fps24.00003.png |
| output.passthrough.fps24.00004.png |
| output.passthrough.fps24.00005.png |
| output.passthrough.fps24.00006.png |
| output.passthrough.fps24.00007.png |
| output.passthrough.fps24.00008.png |
| output.passthrough.fps24.00009.png |
| output.passthrough.fps24.00010.png |
| output.passthrough.fps24.00011.png |
| output.passthrough.fps24.00012.png |
| output.passthrough.fps24.00013.png |
| output.passthrough.fps24.00014.png |
| output.passthrough.fps24.00015.png |
| output.passthrough.fps24.00016.png |
| output.passthrough.fps24.00017.png |
| output.passthrough.fps24.00018.png |
| output.passthrough.fps24.00019.png |
| output.passthrough.fps24.00020.png |
| output.passthrough.fps24.00021.png |
| output.passthrough.fps24.00022.png |
| output.passthrough.fps24.00023.png |
| output.passthrough.fps24.00024.png |
| output.passthrough.fps24.00025.png |
| output.passthrough.fps24.00026.png |
| output.passthrough.fps24.00027.png |
| output.passthrough.fps24.00028.png |
| output.passthrough.fps24.00029.png |
| output.passthrough.fps24.00030.png |
| output.passthrough.fps24.00031.png |
| output.passthrough.fps24.00032.png |
| output.passthrough.fps24.00033.png |
| output.passthrough.fps24.00034.png |
| output.passthrough.fps24.00035.png |
| output.passthrough.fps24.00036.png |
| output.passthrough.fps24.00037.png |
| output.passthrough.fps24.00038.png |
| output.passthrough.fps24.00039.png |
| output.passthrough.fps24.00040.png |
| output.passthrough.fps24.00041.png |
| output.passthrough.fps24.00042.png |
| output.passthrough.fps24.00043.png |
| output.passthrough.fps24.00044.png |
| output.passthrough.fps24.00045.png |
| output.passthrough.fps24.00046.png |
| output.passthrough.fps24.00047.png |
| output.passthrough.fps45.00000.png |
| output.passthrough.fps45.00002.png |
| output.passthrough.fps45.00003.png |
| output.passthrough.fps45.00005.png |
| output.passthrough.fps45.00006.png |
| output.passthrough.fps45.00008.png |
| output.passthrough.fps45.00009.png |
| output.passthrough.fps45.00011.png |
| output.passthrough.fps45.00012.png |
| output.passthrough.fps45.00014.png |
| output.passthrough.fps45.00015.png |
| output.passthrough.fps45.00017.png |
| output.passthrough.fps45.00018.png |
| output.passthrough.fps45.00020.png |
| output.passthrough.fps45.00021.png |
| output.passthrough.fps45.00023.png |
| output.passthrough.fps45.00024.png |
| output.passthrough.fps45.00026.png |
| output.passthrough.fps45.00027.png |
| output.passthrough.fps45.00029.png |
| output.passthrough.fps45.00030.png |
| output.passthrough.fps45.00032.png |
| output.passthrough.fps45.00033.png |
| output.passthrough.fps45.00035.png |
| output.passthrough.fps45.00036.png |
| output.passthrough.fps45.00038.png |
| output.passthrough.fps45.00039.png |
| output.passthrough.fps45.00041.png |
| output.passthrough.fps45.00042.png |
| output.passthrough.fps45.00044.png |
| output.passthrough.fps45.00045.png |
| output.passthrough.fps45.00047.png |
| output.passthrough.fps45.00048.png |
| output.passthrough.fps45.00050.png |
| output.passthrough.fps45.00051.png |
| output.passthrough.fps45.00053.png |
| output.passthrough.fps45.00054.png |
| output.passthrough.fps45.00056.png |
| output.passthrough.fps45.00057.png |
| output.passthrough.fps45.00059.png |
| output.passthrough.fps45.00060.png |
| output.passthrough.fps45.00062.png |
| output.passthrough.fps45.00063.png |
| output.passthrough.fps45.00065.png |
| output.passthrough.fps45.00066.png |
| output.passthrough.fps45.00068.png |
| output.passthrough.fps45.00069.png |
| output.passthrough.fps45.00071.png |
| output.passthrough.fps45.00072.png |
| output.passthrough.fps45.00074.png |
| output.passthrough.fps45.00075.png |
| output.passthrough.fps45.00077.png |
| output.passthrough.fps45.00078.png |
| output.passthrough.fps45.00080.png |
| output.passthrough.fps45.00081.png |
| output.passthrough.fps45.00083.png |
| output.passthrough.fps45.00084.png |
| output.passthrough.fps45.00086.png |
| output.passthrough.fps45.00087.png |
| output.passthrough.fps45.00089.png |
| output.vfr.fps24.00000.png         |
| output.vfr.fps24.00001.png         |
| output.vfr.fps24.00002.png         |
| output.vfr.fps24.00003.png         |
| output.vfr.fps24.00004.png         |
| output.vfr.fps24.00005.png         |
| output.vfr.fps24.00006.png         |
| output.vfr.fps24.00007.png         |
| output.vfr.fps24.00008.png         |
| output.vfr.fps24.00009.png         |
| output.vfr.fps24.00010.png         |
| output.vfr.fps24.00011.png         |
| output.vfr.fps24.00012.png         |
| output.vfr.fps24.00013.png         |
| output.vfr.fps24.00014.png         |
| output.vfr.fps24.00015.png         |
| output.vfr.fps24.00016.png         |
| output.vfr.fps24.00017.png         |
| output.vfr.fps24.00018.png         |
| output.vfr.fps24.00019.png         |
| output.vfr.fps24.00020.png         |
| output.vfr.fps24.00021.png         |
| output.vfr.fps24.00022.png         |
| output.vfr.fps24.00023.png         |
| output.vfr.fps24.00024.png         |
| output.vfr.fps24.00025.png         |
| output.vfr.fps24.00026.png         |
| output.vfr.fps24.00027.png         |
| output.vfr.fps24.00028.png         |
| output.vfr.fps24.00029.png         |
| output.vfr.fps24.00030.png         |
| output.vfr.fps24.00031.png         |
| output.vfr.fps24.00032.png         |
| output.vfr.fps24.00033.png         |
| output.vfr.fps24.00034.png         |
| output.vfr.fps24.00035.png         |
| output.vfr.fps24.00036.png         |
| output.vfr.fps24.00037.png         |
| output.vfr.fps24.00038.png         |
| output.vfr.fps24.00039.png         |
| output.vfr.fps24.00040.png         |
| output.vfr.fps24.00041.png         |
| output.vfr.fps24.00042.png         |
| output.vfr.fps24.00043.png         |
| output.vfr.fps24.00044.png         |
| output.vfr.fps24.00045.png         |
| output.vfr.fps24.00046.png         |
| output.vfr.fps24.00047.png         |
| output.vfr.fps24.00048.png         |
| output.vfr.fps45.00000.png         |
| output.vfr.fps45.00002.png         |
| output.vfr.fps45.00003.png         |
| output.vfr.fps45.00005.png         |
| output.vfr.fps45.00006.png         |
| output.vfr.fps45.00008.png         |
| output.vfr.fps45.00009.png         |
| output.vfr.fps45.00011.png         |
| output.vfr.fps45.00012.png         |
| output.vfr.fps45.00014.png         |
| output.vfr.fps45.00015.png         |
| output.vfr.fps45.00017.png         |
| output.vfr.fps45.00018.png         |
| output.vfr.fps45.00020.png         |
| output.vfr.fps45.00021.png         |
| output.vfr.fps45.00023.png         |
| output.vfr.fps45.00024.png         |
| output.vfr.fps45.00026.png         |
| output.vfr.fps45.00027.png         |
| output.vfr.fps45.00029.png         |
| output.vfr.fps45.00030.png         |
| output.vfr.fps45.00032.png         |
| output.vfr.fps45.00033.png         |
| output.vfr.fps45.00035.png         |
| output.vfr.fps45.00036.png         |
| output.vfr.fps45.00038.png         |
| output.vfr.fps45.00039.png         |
| output.vfr.fps45.00041.png         |
| output.vfr.fps45.00042.png         |
| output.vfr.fps45.00044.png         |
| output.vfr.fps45.00045.png         |
| output.vfr.fps45.00047.png         |
| output.vfr.fps45.00048.png         |
| output.vfr.fps45.00050.png         |
| output.vfr.fps45.00051.png         |
| output.vfr.fps45.00053.png         |
| output.vfr.fps45.00054.png         |
| output.vfr.fps45.00056.png         |
| output.vfr.fps45.00057.png         |
| output.vfr.fps45.00059.png         |
| output.vfr.fps45.00060.png         |
| output.vfr.fps45.00062.png         |
| output.vfr.fps45.00063.png         |
| output.vfr.fps45.00065.png         |
| output.vfr.fps45.00066.png         |
| output.vfr.fps45.00068.png         |
| output.vfr.fps45.00069.png         |
| output.vfr.fps45.00071.png         |
| output.vfr.fps45.00072.png         |
| output.vfr.fps45.00074.png         |
| output.vfr.fps45.00075.png         |
| output.vfr.fps45.00077.png         |
| output.vfr.fps45.00078.png         |
| output.vfr.fps45.00080.png         |
| output.vfr.fps45.00081.png         |
| output.vfr.fps45.00083.png         |
| output.vfr.fps45.00084.png         |
| output.vfr.fps45.00086.png         |
| output.vfr.fps45.00087.png         |
| output.vfr.fps45.00089.png         |

# （不负责任的）结论

- cfr 可以和 `-r` 输出选项搭配使用，使输出视频的帧率符合指定值
- vfr passthrough drop 都不应该和 `-r` 输出选项搭配使用
- vfr 常用于按类型过滤帧等输入帧被主动丢弃的场景
- passthrough drop 不清楚应用场景

参见官方文档中关于 `-vsync` 的[说明文档](http://ffmpeg.org/ffmpeg-all.html#Advanced-options)。

To be continued

[ffmpeg]: http://ffmpeg.org
