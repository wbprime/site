#!/usr/bin/env bash

#
# Author: mail __AT__ wangbo __DOT__ im
#

#
# Enable strict bash mode
set -euo pipefail
IFS=$'\t\n'

input_uri="${1:?No input uri}"

input_filename="${input_uri##*/}"
input_basename="${input_filename%.*}"

segment_filename_0="hls.${input_basename}%d.0.ts"
output_filename_0="hls.${input_basename}.0.m3u8"
segment_filename_1="hls.${input_basename}%d.1.ts"
output_filename_1="hls.${input_basename}.1.m3u8"

ffmpeg -hide_banner -loglevel warning \
    -ss 10 -t 20 \
 	-i "${input_uri}" \
    -filter_complex "[v:0]split=2[vtemp001][vout002];[vtemp001]scale=w=960:h=540[vout001]" \
    -g 30 \
    -sc_threshold 0 \
    -flags +cgop \
    -c:a aac -b:a 128k -ac 2 \
    -f hls \
    -hls_time 4 \
    -hls_playlist_type event \
    -hls_segment_filename "${segment_filename_0}" \
    -hls_list_size 0 \
    -map "[vout001]" -c:v:0 libx264 -b:v:0 2000k \
    -map a:0 \
    "${output_filename_0}" \
    -g 30 \
    -sc_threshold 0 \
    -flags +cgop \
    -c:a aac -b:a 128k -ac 2 \
    -f hls \
    -hls_time 4 \
    -hls_playlist_type event \
    -hls_segment_filename "${segment_filename_1}" \
    -hls_list_size 0 \
    -map "[vout002]" -c:v:1 libx264 -b:v:1 6000k \
    -map a:0 \
    "${output_filename_1}"

# vim:nu:tabstop=4:shiftwidth=4:ft=sh
