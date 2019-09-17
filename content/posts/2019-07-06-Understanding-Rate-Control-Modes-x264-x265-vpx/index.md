+++
title = "[译] Understanding Rate Control Modes (x264, x265, vpx)"
description = "翻译自 Understanding Rate Control Modes (x264, x265, vpx)"
date = 2019-07-06T10:50:16+08:00
draft = false
template = "page.html"
[taxonomies]
categories =  ["Streaming"]
tags = ["ffmpeg", "h264", "rate-control", "crf", "cbr", "vbr", "abr", "vbv"]
+++

最近在做短视频转码的项目，涉及到视频在线传输的码率控制的问题，于网络搜寻资料时发现了一篇介绍基于
[FFmpeg][ffmpeg] 的码率控制综述文章，获益匪浅，翻译出来作为读书笔记。

原文：[Understanding Rate Control Modes (x264, x265, vpx)](https://slhck.info/video/2017/03/01/rate-control.html)

<!-- more -->

词汇对应：

- Encode/Encoding 编码 转码
- Rate Control 码率控制
- Variable Bitrate (VBR) 动态码率
- Constant Bitrate (CBR) 固定码率
- Average Bitrate (ABR) 平均码率
- Quantization Parameter (QP) 量化参数
- Constant Quality Factor (CRF) 固定质量因子
- Constrained Encoding/Video Buffer Verifer (VBV) 限制性编码
- 1-pass 单路
- 2-pass 两路

标题：快速理解视频编码中的码率控制套路

译文：

何为“码率控制”？码率控制即控制视频编码器在编码一帧视频图像时使用多少比特位。（无损）视频编码的目标是在尽量不损失视频质量的前提下，尽量生成最小的新视频，以节省空间。码率控制是视频编码取得空间和视频质量双向均衡的关键。

码率控制有多种套路 -- 你可能听说过 "1-pass" "2-pass" "CBR" "VBR" "VBV" 或 "CRF"。

套路太多？不仅太多，往往有时候你在网络看到到视频编码的命令行示例给你展示的是错误的套路。为此，本文专门介绍视频编码的各种套路。请注意，本文并不涉及码率-失真最优化方面的知识。

# 预备知识 -- 动态码率 VS 固定码率

大家对音频编码里的码率控制应该比较熟悉，尤其是伴随着 MP3 长大的一代们。最开始，音频编码使用的是固定码率技术；后来，出现了动态码率技术。动态码率能够在给定条件下，生成满足文件大小和音频质量均衡的音频文件；动态码率的控制参数被称之为质量级别 (VBR quality level)。

简言之，动态码率控制编码器，使其在编码“硬骨头”时使用更多的比特位，而在编码“软骨头”时使用较少的比特位。怎么区分所谓的“硬骨头”和“软骨头”呢？基本上视频中运动剧烈的片段都是“硬骨头”，因为片段中相邻帧之间的差异较大难以压缩。另外，大量空间细节和复杂纹理的视频也属于“硬骨头”。

# 视频编码的场景

选择视频编码的方式很大程度上取决于具体的应用场景。应用场景很多，以下是有代表性的几类：

1. **压缩归档** 原视频需要被重新编码压缩以归档，归档的视频可以被存储在外部的硬盘或网络存储上。此场景下，编码后的视频文件需要尽量占用较少的空间，而且并不关心压缩的程度（译注：意为被用来归档的视频由于被读取的机会较少，可用较大的读取时间复杂度换取较大的压缩比例，而且不需要视频是固定码率的）。
2. **流媒体** 转码后的视频需要在网络上传输，如视频点播应用，可能是 HTTP 直接下载或自适应下载的形式；对于前者，新视频可能需要确保不能超过一个码率上限；对于后者，需要对编码施加多码率控制。
3. **实时流媒体** 同2的场景很类似，但对视频的编码速度要求较高。
4. **刻录** 视频需要被刻录到 DVD 或蓝光影碟，所以最终的文件需要满足给定的大小。

了解应用场景有助于选择码率控制的方式。

# 码率控制的各种套路

现在可以详细地了解一下 [FFmpeg][ffmpeg] 中视频编码的各种套路了。本文只涉及 H.264 的编码器 [x264][x264] 和 H.265 的编码器 [x265][x265]，以及 [libvpx][libvpx] 编码器。更多的编码器说明请参见此[文档][ffmpeg_libx264]。

要使用 [x265][x265] 编码器，[FFmpeg][ffmpeg] 需要在编译时启用 `--enable-libx265` 参数；[libvpx][libvpx] 需要启用 `--enable-libvpx` 参数。另外，部分 [x265][x265] 的参数不能直接通过 [FFmpeg][ffmpeg] 的命令行传入，需要借助于 `-x265-params` 选项。

请注意：[x264][x264] 之类的编码器在工作时无法遵循固定的码率参数，这意味着在编码简单的帧时，其会使用比指定的码率更低的比特位。无需过分关注此特性，只需记住，编码器不会给你机会来浪费码率。

## 固定量化参数 (Constant Quantization Parameter, CQP)

量化参数可以控制一个视频帧中宏块(macroblock)的压缩量。较大的量化参数值意味着更多的压缩和更低的质量；较小的值则反之。H.264 编码中量化参数的取值范围为 \[0, 51]。

[x264][x264] 和 [x265][x265] 编码器可以很容易地设置视频的整个编码过程中使用固定量化参数，而 [libvpx][libvpx] 则不支持固定量化参数。

```
ffmpeg -i <input> -c:v libx264 -qp 23 <output>
ffmpeg -i <input> -c:v libx265 -x265-params qp=23 <output>
```

如果不害怕数学公式的化，可以继续阅读这篇[指南](https://www.vcodex.com/h264avc-4x4-transform-and-quantization/)详细了解量化参数的原理。

如无必要， **不要使用本方法** 。固定的量化参数会使得不同复杂度的场景编码出来的比特率相差悬殊，而且会导致编码低效。你无法控制输出的最终码率，而且会浪费空间。

- *适用于* 视频编码研究
- *不适用于* 几乎所有场景

注意到 [Netflix 建议说使用固定量化参数](https://medium.com/netflix-techblog/dynamic-optimizer-a-perceptual-video-encoding-optimization-framework-e19f1e3a277f) 可以优化每场景编码 (per-shot encoding)，取得更好的编码效果。但是这是在大量预处理和多场景精细编排的基础上取得的效果，并不是一种银弹；你只有在实现了整个框架的基础上才能考虑使用它。

## 平均码率 (Average Bitrate, ABR)

我们可以指定一个目标码率，然后让编码器自己按照算法趋近之。

```
ffmpeg -i <input> -c:v libx264 -b:v 1M <output>
ffmpeg -i <input> -c:v libx265 -b:v 1M <output>
ffmpeg -i <input> -c:v libvpx-vp9 -b:v 1M <output>
```

**不要使用本方法**

[x264][x264] 编码器的一个官方开发人员 [强调千万不要使用这种方法](https://mailman.videolan.org/pipermail/x264-devel/2010-February/006934.html)。原因在于编码器在工作时并不知道后续的视频帧的细节，因而需要做出某些假定以达到目标码率。这意味着在区间内开始阶段或达到目标码率时，编码的码率会变化剧烈（译注：码率是单位时间内的视频大小，如果码率控制以固定区间间隔进行，在区间的开始阶段，编码器有富余的比特位进行编码，在区间的后半段，剩余的比特位可能会限制编码器)。尤其对于 HAS 类型的流媒体编码，本方法有可能会导致多个短视频片段的质量差异悬殊。

本方法不是所谓的固定码率套路。尽管 ABR 从技术上可以被称之为动态码率，但实际上并不比固定码率好多少，因为其不能有效地保证视频质量。

- *适用于* 快速且对视频质量要求不高的场景
- *不适用于* 几乎所有场景

## 固定码率 (CBR)

可以通过 `nal-hrd` 选项控制编码器使用固定的码率进行编码，如果真的有需求的话。

```
ffmpeg -i <input> -c:v libx264 -x264-params "nal-hrd=cbr:force-cfr=1" -b:v 1M -minrate 1M -maxrate 1M -bufsize 2M <output>
```

输出文件的格式必须要是 `.ts` 格式(即 MPEG-2 TS)，因为 MP4 不支持 NAL 填充 (NAL stuffing)。

**本方法会导致带宽的浪费！**

如果原视频很容易被编码，固定码率的设定会导致带宽的浪费，参见更详细的[说明](https://brokenpipe.wordpress.com/2016/10/07/ffmpeg-h-264-constant-bitrate-cbr-encoding-for-iptv/)。本方法适应于某些场景，但此时你可能需要设置尽量小的目标码率。

对于 VP9 编码，请使用：

```
ffmpeg -i <input> -c:v libvpx-vp9 -b:v 1M -maxrate 1M -minrate 1M <output>
```

- *适用于* 保证固定码率；流媒体视频服务
- *不适用于* 归档；需要优化带宽使用的场景

## 两路平均码率 (2-pass Average Bitrate, 2-Pass ABR)

如果可以对原视频进行两次（或多次）编码，编码器可以对编码内容进行更好的估计(译注：而不用进行某些可能是错误的假定)。编码器可以在第一次编码时计算编码每一帧的代价，从而在第二次编码时更有效地分配可用的比特位。这使得编码器可以在给定的码率条件下取得最好的视频质量。

H.264 编码：

```
ffmpeg -i <input> -c:v libx264 -b:v 1M -pass 1 -f null /dev/null
ffmpeg -i <input> -c:v libx264 -b:v 1M -pass 2 <output>.mp4
```

H.265 编码：

```
ffmpeg -i <input> -c:v libx264 -b:v 1M -x265-params pass=1 -f null /dev/null
ffmpeg -i <input> -c:v libx264 -b:v 1M -x265-params pass=2 <output>.mp4
```

VP9 编码：

```
ffmpeg -i <input> -c:v libvpx-vp9 -b:v 1M -pass 1 -f null /dev/null
ffmpeg -i <input> -c:v libvpx-vp9 -b:v 1M -pass 2 <output>.webm
```

对流媒体编码来说这是最简单的方式。

本方法有两个不足：

1. 无法确定指定码率下的输出视频质量。所以你需要进行足够的测试保证目标码率即使对于复杂视频也能保证质量。
2. 存在一些码率的波峰，使得传输的比特位超出客户端的能力。

目标码率可以参考[YouTube 的视频上传规范建议](https://support.google.com/youtube/answer/1722171?hl=en)，但注意该建议的目的是上传高质量的视频，存在着一定的下调空间。

- *适用于* 尽量达到目标码率；刻录
- *不适用于* 快速转码 (如实时流媒体)

## 固定质量或固定码率因子 (Constant Quality or Constant Rate Factor, CQ/CRF)

我已经在 [Constant Rate Factor](https://slhck.info/articles/crf) 这篇文章中详细地介绍过固定码率因子。其可以控制在整个编码过程中保持
固定的编码质量。设置 CRF 是一个很简单的事情，只需要指定一个目标值然后让编码器来达到它。

```
ffmpeg -i <input> -c:v libx264 -crf 23 <output>
ffmpeg -i <input> -c:v libx265 -crf 28 <output>
ffmpeg -i <input> -c:v libvpx-vp9 -crf 30 -b:v 0 <output>
```

H.264 和 H.265 编码中 CRF 的取值范围为 \[0, 51] (同 QP)：[x264][x264] 的默认值是 23，[x265][x265] 的默认值是 28；值 18 (对于 H.265 来说是 24）可以达到视觉无损；更小的值基本上就是在浪费空间。CRF 取值每 +/- 6 会大概导致码率的减半或加倍。

VP9 编码中 CRF 的取值范围为 \[0, 63], 建议取值范围为 \[15,35]。

本方法的缺点在于无法预料输出文件的大小（码率）。

请注意，对于两路平均编码和固定码率因子来说，如果输出的视频码率相同，则视频的质量也基本一致。差别在于前者可以控制输出码率，后者可以控制输出视频质量。

- *适用于* 归档；指定目标质量
- *不适用于* 流媒体；指定目标质量

## 限制性编码 (Contrained Encoding, VBV)

[Video Buffering Verifer (VBV)][] 可以设定输出码率的上限，这对于流媒体服务很有用。VBV 可以配合两路平均码率方法或固定码率因子方法一起使用，后者通常被称为 "capped CRF"。

启用 VBV 需要联合使用 `-maxrate` 和 `-bufsize` 选项：

```
ffmpeg -i <input> -c:v libx264 -crf 23 -maxrate 1M -bufsize 2M <output>
ffmpeg -i <input> -c:v libx265 -crf 28 -x265-params vbv-maxrate=1000:vbv-bufsize=2000 <output>
```

在 VP9 编码中，该方法不叫 VBV，但是背后的原理相同。

```
ffmpeg -i <input> -c:v libvpx-vp9 -crf 30 -b:v 2M <output>
```

备注：[x264][x264]/[x265][x265] 编码器提供了 `-tune zerolatency` 和 `-preset ultrafast` 选项以优化流媒体服务场景中的编码速度：降低同码率下的视频质量，但显著提高编码速度。libvpx-vp9 编码器的对应物是 `quality realtime` 和 `-speed 5`。参阅 [H.264](http://trac.ffmpeg.org/wiki/Encode/H.264) 和 [VP9](http://trac.ffmpeg.org/wiki/Encode/VP9)。

两路平均码率的 VBV 方法如下：

```
ffmpeg -i <input> -c:v libx264 -b:v 1M -maxrate 1M -bufsize 2M -pass 1 -f null /dev/null
ffmpeg -i <input> -c:v libx264 -b:v 1M -maxrate 1M -bufsize 2M -pass 2 <output>
```

x265 版本：

```
ffmpeg -i <input> -c:v libx265 -b:v 1M -x265-params pass=1:vbv-maxrate=1000:vbv-bufsize=2000 -f null /dev/null
ffmpeg -i <input> -c:v libx265 -b:v 1M -x265-params pass=2:vbv-maxrate=1000:vbv-bufsize=2000 <output>
```

VP9 版本：

```
ffmpeg -i <input> -c:v libvpx-vp9 -b:v 1M -maxrate 1M -bufsize 2M -pass 1 -f null /dev/null
ffmpeg -i <input> -c:v libvpx-vp9 -b:v 1M -maxrate 1M -bufsize 2M -pass 2 <output>
```

据 x264 的开发者称，一路的平均码率也可以应用 VBV，而且[效果和两路的一样好](https://mailman.videolan.org/pipermail/x264-devel/2010-February/006944.html)，除了压缩效果可能会差一些。

那么问题来了，`bufsize` 该如何取值呢？答案是 `It depends`。一个比较好的值是设置为最大码率的两倍；如果客户端缓冲区较小（比如只有几秒钟），`bufsize` 可以设置为与最大码率相同；如果想尽量限制输出码率，可以将 `bufsize` 设置为最大码率的一半或更小。

当将 VBV 配合 CRF 使用时，最关键的是找一个合适的 crf 值，使得输出的平均码率接近最大码率，但不能超过。如果输出码率超出最大码率，crf 值可能选的过低；反之亦然。例如，假如单独使用 crf = 18 时的编码视频输出平均码率为 3.0 Mbit/s，而你期望目标输出码率为 1.5 Mbit/s，可以设置 crf 为 24。

- *适用于* 端宽限制的流媒体服务；实时流媒体服务(VBV + CRF)；点播流媒体服务(2-pass ABR + VBV)
- *不适用于* 归档

# 效果对比

(译注：上述各方法的效果对比请参见原文）

# 总结

事实上，要搞清楚所有的视频码率控制套路并非难事。而且，最简单的套路()结果最差，却反而最频繁地出现于网
络上的示例中，以讹传讹。

以下是简单的选择参考：

1. *压缩归档* 固定质量因子 (CRF)
2. *流媒体* 两路平均码率或固定质量因子配合 VBV (2-pass CRF or ABR with VBV)
3. *实时流媒体* 平均码率或固定质量因子配合 VBV (1-pass CRF or ARB with VBV)；固定码率 (CBR)
4. *刻录* 两路平均码率 (2-pass ABR)

参考资料来源：

- [Handbrake Wiki: Constant Quality vs Average Bit Rate](https://handbrake.fr/docs/en/latest/technical/video-cq-vs-abr.html)
- [FFmpeg H.264 Encoding Guide](http://trac.ffmpeg.org/wiki/Encode/H.264)
- [x264-devel Mailing List: Making sense out of x264 rate control modes](https://mailman.videolan.org/pipermail/x264-devel/2010-February/006933.html)
- [Video Encoding Settings for H.264 Excellence](http://www.lighterra.com/papers/videoencodingh264/)
- [A qualitative overview of x264’s ratecontrol methods](http://akuvian.org/src/x264/ratecontrol.txt)
- [Google: VP9 Bitrate Modes in Detail](https://developers.google.com/media/vp9/bitrate-modes/)
- [Streaming Learning Center: Saving on Encoding and Streaming: Deploy Capped CRF](https://streaminglearningcenter.com/blogs/saving-encoding-streaming-deploy-capped-crf.html)

[ffmpeg]: http://ffmpeg.org/ "FFmpeg A complete, cross-platform solution to record, convert and stream audio and video."
[x264]: http://www.videolan.org/developers/x264.html
[x265]: http://x265.org/
[libvpx]: https://www.webmproject.org/code/
[ffmpeg_libx264]: http://ffmpeg.org/ffmpeg-all.html#libx264_002c-libx264rgb
