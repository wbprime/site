+++
title = "Ffmpeg snapshot/thumbnail 快照套路总结"
description = "使用 Ffmpeg 对视频进行抽帧操作，即把视频中感兴趣的帧输出为图片；该操作也经常被称之为视频快照、视频截图等。视频抽帧按照输出文件的格式不同，可以输出为 `jpg`、`png`、`gif`、`webp` 等格式文件。视频抽帧按照选取视频帧的条件不同，可以分为按类型、按时间、按场景变化等不同的套路。本文从以上两个方面汇总使用 Ffmpeg 抽帧的套路。"
date = 2019-07-21T15:37:14+08:00
draft = false
template = "page.html"
[taxonomies]
categories =  ["Streaming"]
tags = ["ffmpeg", "snapshoting", "gif", "webp", "select"]
+++

使用 [Ffmpeg][ffmpeg] 对视频进行抽帧操作，即把视频中感兴趣的帧输出为图片；该操作也经常被称之为视频快照、视频截图等。

视频抽帧按照输出文件的格式不同，可以输出为 `jpg`、`png`、`gif`、`webp` 等格式文件。

视频抽帧按照选取视频帧的条件不同，可以分为按类型、按时间、按场景变化等不同的套路。

本文从以上两个方面汇总使用 [Ffmpeg][ffmpeg] 抽帧的套路。

<!-- more -->

# 参数

## 输出的帧的数量

可以通过 `-frames:v` 选项或 `-vframes` 来控制。

## 输出的帧的频率

可以通过 `-r` 选项来控制；也可以通过 [fps][fps_filter] 或 [setpts][setpts_filter] 来控制。

## 选取帧的方式

可以通过万能的 [select][select_filter] 来设定，也可以通过设定输出帧率配合 `-vsync` 来设定。

# 输出格式

## png

视频抽帧输出为 `png` 格式文件，是最常见的模式。

首先需要设置输出的格式(通过 `-f` 选项)为 [image2][image2_format]。

输出文件名需要以 `.png` 结尾；由于 `image2` 格式支持输出多张图像文件，所以输出文件名支持 `%d` 占位符，也支持其他的占位符，具体用法参见 [image2][image2_format] 的文档。

典型的用法如下：

```sh
ffmpeg -i input.flv -vf fps=1 -frames:v 10 out%d.png
```

## jpg

视频抽帧输出为 `jpg/jpeg` 格式文件的方式与 `png` 很类似。

首先需要设置输出的格式(通过 `-f` 选项)为 [image2][image2_format]。

输出文件名需要以 `.jpg` 结尾；文件名也支持占位符。

另外，可以通过额外的参数控制输出图像的质量：`-q` 取值范围为 `[2, 31]`, `2` 的输出质量最好。

典型的用法如下：

```sh
ffmpeg -i input.flv -vf fps=1 -frames:v 10 -q:v 2 out%d.jpg
```

## gif

`gif` 格式支持动画显示，所以可以把多个图像文件合并为一个 `gif` 文件。

首先需要设置输出的格式(通过 `-f` 选项)为 [gif][gif_format]。

输出文件名需要以 `.gif` 结尾。

另外，可以通过额外的参数控制动画的循环次数：`-loop` 取值 `> 0` 表示动画的循环次数；取值 `-1` 表示不循环；取值 `0` 表示无限循环。

典型的用法如下：

```sh
ffmpeg -i input.flv -vf fps=1 -frames:v 10 -loop 0 -f gif out.gif
```

上述的方法的最大问题是输出的图像质量较差，优化的方法是（参见 [Stackoverflow 答案](https://superuser.com/questions/556029/how-do-i-convert-a-video-to-gif-using-ffmpeg-with-reasonable-quality)）添加过滤器 `palettegen` 和 `paletteuse`：

```sh
ffmpeg -i input.flv -frames:v 10 -loop 0 -f gif \
    -filter "split[x1][x2];[x1]palettegen[p];[x2][p]paletteuse" \
    -r 1 \
    out.gif
```

## webp

`webp` 是那家 *中国大陆地区不存在* 公司首倡的图像格式，目标是替代 `gif` 和 `png` 的应用场景。其支持静态图像和动态图像（动画）。`gif` 和 `webp` 的对比可以参见[客户端上动态图格式对比和解决方案](https://zhuanlan.zhihu.com/p/25598828)
。

首先需要设置输出的格式(通过 `-f` 选项)为 `webp`；输出编码器设置为 [libwebp][webp_codec]。

输出文件名需要以 `.webp` 结尾。

另外，可以通过额外的参数控制动画的循环次数：`-loop` 取值 `> 0` 表示动画的循环次数；取值 `-1` 表示不循环；取值 `0` 表示无限循环。

```sh
ffmpeg -i input.flv -vf fps=1 -frames:v 10 -loop 0 -f webp -c:v libwebp out.webp
```

如果需要控制输出图像质量，可以参见 [libwebp][webp_codec] 文档；默认的配置基本能够满足需要。

# 选帧方式

## 等间隔选帧

通过 `-r` 参数实现。

## `thumbnail` 过滤器

[thumbnail][thumbnail_filter] 过滤器可以在指定大小的一个帧集合里面选出最有代表性的帧。

```sh
ffmpeg -i input.flv -frames:v 10 -loop 0 -f webp -c:v libwebp \
    -filter:v thumbnail \
    out.webp
```

## `select` 过滤器

[select][select_filter] 是一个超级强大的过滤器，可以通过表达式实现各种选取帧的套路。

### 通过帧类型选取

只选取 `I` 帧：

```sh
ffmpeg -i input.flv -frames:v 10 -loop 0 -f webp -c:v libwebp \
    -filter "select='eq(pict_type,I)'" \
    out.webp
```

选取 `I` 和 `P` 帧：

```sh
ffmpeg -i input.flv -frames:v 10 -loop 0 -f webp -c:v libwebp \
    -filter "select='eq(pict_type,I)+eq(pict_type,P)'" \
    out.webp
```

### 通过时间间隔选取

通过 `select` 也可以实现按时间间隔选取帧。如每 5 秒一帧：

```sh
ffmpeg -i input.flv -frames:v 10 -loop 0 -f webp -c:v libwebp \
    -filter "select='gte(t,selected_n\\*5\\+start_t)'" \
    out.webp
```

### 通过帧间隔选取

通过 `select` 也可以实现按帧的间隔选取帧。如每 5 帧一帧：

```sh
ffmpeg -i input.flv -frames:v 10 -loop 0 -f webp -c:v libwebp \
    -filter "select='not(mod(n,100))'" \
    out.webp
```

### 按场景变化程度阈值选取

场景变化是指相邻的两帧之间的运动矢量也就是两帧内容的差异。选择场景变化程度超过某阈值的帧在某些应用场景中有独特的优势。

```sh
ffmpeg -i input.flv -frames:v 10 -loop 0 -f webp -c:v libwebp \
    -filter "select='gt(scene,0.4)'" \
    out.webp
```

### 多种条件选取

按照 [Ffmpeg][ffmpeg] 中的[表达式运算规则](http://ffmpeg.org/ffmpeg-utils.html#Expression-Evaluation)，`*` 运算符表示逻辑和（AND），`+` 运算符表示逻辑或（OR）。如果了解这一点，就可以在 `select` 过滤器中组合各种条件实现复杂的帧选取逻辑。

如 `select='eq(pict_type,I)+gt(scene,0.4)'` 可以选取所有场景变化满足条件的帧和所有 `I` 帧。

# 补充说明

## 字符转义

在组合 `select` 的参数时，需要注意特殊字符（如 `,` `+` `*`）的转义，具体参见 [官方文档](https://ffmpeg.org/ffmpeg-filters.html#Notes-on-filtergraph-escaping)。

## 输出动画的速度

在按照帧类型选取时，由于帧的时间戳的不均匀变化，可能会导致输出动画的速度不固定。

如果需要确定的输出动画速度，可以使用 `-r` 选项配合 [setpts][setpts_filter] 过滤器一起使用。

## 视频片段选取

如果能在输入参数处设置片段的起始信息，可以加快抽帧的速度，参见官方关于 [Seeking](https://trac.ffmpeg.org/wiki/Seeking) 的说明。

## 输出图像的大小

如果需要指定输出图像的大小，可以用于 `-s` 参数或直接使用 [scale][scale_filter] 过滤器。

# 示例脚本下载

1. [snapshot_by_frame_type](app_ffmpeg_snapshot.by_frametype.sh) 按照帧类型抽帧，可以设置目标类型和输出动画速度
2. [snapshot_by_interval](app_ffmpeg_snapshot.by_interval.sh) 按照时间间隔抽帧，可以设置时间间隔和输出动画速度
3. [snapshot_by_thumbnail](app_ffmpeg_snapshot.by_thumbnail.sh) 按照帧类型和场景变化抽帧，可以设置输出动画速度

以上。

更多参见：

- <https://trac.ffmpeg.org/wiki/Create%20a%20thumbnail%20image%20every%20X%20seconds%20of%20the%20video>
- <http://superuser.com/questions/538112/meaningful-thumbnails-for-a-video-using-ffmpeg>

[ffmpeg]: http://ffmpeg.org/
[fps_filter]: https://ffmpeg.org/ffmpeg-filters.html#fps
[setpts_filter]: https://ffmpeg.org/ffmpeg-filters.html#setpts_002c-asetpts
[select_filter]: https://ffmpeg.org/ffmpeg-filters.html#select_002c-aselect
[thumbnail_filter]: https://ffmpeg.org/ffmpeg-filters.html#thumbnail
[scale_filter]: http://ffmpeg.org/ffmpeg-filters.html#scale-1
[image2_format]: https://ffmpeg.org/ffmpeg-all.html#image2-2
[gif_format]: https://ffmpeg.org/ffmpeg-all.html#gif-2
[webp_codec]: https://ffmpeg.org/ffmpeg-all.html#libwebp
