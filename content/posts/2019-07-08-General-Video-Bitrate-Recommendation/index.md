+++
title = "[Note] General Video Bitrate Recommendation"
description = "在视频转码的过程中，比较关键的是需要确定目标码率（或最大码率）。在网上找了一下大型互联网公司的推荐标准，聊为参考。"
date = 2019-07-08T10:48:41+08:00
draft = false
template = "page.html"
[taxonomies]
categories =  ["Streaming"]
tags = ["bitrate", "youtube", "apple"]
+++

在视频转码的过程中，比较关键的是需要确定目标码率（或最大码率）。

在网上找了一下大型互联网公司的推荐标准，聊为参考。

- [YouTube](#youtube)
- [Apple](#apple)
- [阿里云云栖社区博客](#a-li-yun-yun-qi-she-qu-mou-bo-ke)

<!-- more -->

# YouTube

[Recommended upload encoding settings](https://support.google.com/youtube/answer/1722171?hl=en)

- Recommended video bitrates for SDR uploads

| Type      | Video Bitrate Standard Frame Rate (24, 25,30) | Video Bitrate High Frame Rate (48,50,60) |
|-----------|-----------------------------------------------|------------------------------------------|
| 2160p(4k) | 35-45 Mbps                                    | 53-68 Mbps                               |
| 1440p(2k) | 16 Mbps                                       | 24 Mbps                                  |
| 1080p     | 8 Mbps                                        | 12 Mbps                                  |
| 720p      | 5 Mbps                                        | 7.5 Mbps                                 |
| 480p      | 2.5 Mbps                                      | 4 Mbps                                   |
| 360p      | 1 Mbps                                        | 1.5 Mbps                                 |

- Recommended video bitrates for HDR uploads

| Type      | Video Bitrate Standard Frame Rate (24, 25,30) | Video Bitrate High Frame Rate (48,50,60) |
|-----------|-----------------------------------------------|------------------------------------------|
| 2160p(4k) | 44-56 Mbps                                    | 66-85 Mbps                               |
| 1440p(2k) | 20 Mbps                                       | 30 Mbps                                  |
| 1080p     | 10 Mbps                                       | 15 Mbps                                  |
| 720p      | 6.5 Mbps                                      | 9.5 Mbps                                 |
| 480p      | Not supported                                 | Not supported                            |
| 360p      | Not supported                                 | Not supported                            |

在翻墙不便的情况下，可以参考图片(PNG)版本。

![Recommended upload encoding settings](bitrate_by_youtube.png)

或者 PDF 版本。

[Recommended upload encoding settings (PDF)](bitrate_by_youtube.pdf)

# Apple

[HLS Authoring Specification for Apple Devices](https://developer.apple.com/documentation/http_live_streaming/hls_authoring_specification_for_apple_devices)

- Video average bit rate (kb/s)

| 16:9 aspect ratio | H.264/AVC | Frame rate                  |
|-------------------|-----------|-----------------------------|
| 416 x 234         | 145       | <span>&#8804;</span> 30 fps |
| 640 x 360         | 365       | <span>&#8804;</span> 30 fps |
| 768 x 432         | 730       | <span>&#8804;</span> 30 fps |
| 768 x 432         | 1100      | <span>&#8804;</span> 30 fps |
| 960 x 540         | 2000      | same as source              |
| 1280 x 720        | 3000      | same as source              |
| 1280 x 720        | 4500      | same as source              |
| 1920 x 1080       | 6000      | same as source              |
| 1920 x 1080       | 7800      | same as source              |

在翻墙不便的情况下，可以参考图片(PNG)版本。

![HLS Authoring Specification for Apple Devices](bitrate_by_apple.png)

或者 PDF 版本。

[HLS Authoring Specification for Apple Devices (PDF)](bitrate_by_apple.pdf)

# Others

## 阿里云云栖社区某博客

[阿里云云栖社区博客 - H.264 码率设置](https://yq.aliyun.com/articles/243467)

| 视频大小 | 分辨率      | 推荐码率  |
|----------|-------------|-----------|
| 480P     | 720 x 480   | 1800 Kbps |
| 720P     | 1280 x 720  | 3500 Kbps |
| 1080P    | 1920 x 1080 | 8500 Kbps |

