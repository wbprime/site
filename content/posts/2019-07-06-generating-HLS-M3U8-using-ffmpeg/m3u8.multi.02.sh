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

segment_filename="hls.${input_basename}%d.%v.ts"
output_filename="hls.${input_basename}.%v.m3u8"
master_output_filename="hls.${input_basename}.master.m3u8"

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
    -hls_segment_filename "${segment_filename}" \
    -hls_list_size 0 \
    -map "[vout001]" -c:v:0 libx264 -b:v:0 2000k \
    -map "[vout002]" -c:v:1 libx264 -b:v:1 6000k \
    -map a:0 \
    -map a:0 \
    -var_stream_map "v:0,a:0 v:1,a:1" \
    -master_pl_name "${master_output_filename}" \
    "${output_filename}"

# vim:nu:tabstop=4:shiftwidth=4:ft=sh
