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

segment_filename="hls.${input_basename}%d.ts"
output_filename="hls.${input_basename}.m3u8"

ffmpeg -hide_banner -loglevel warning \
    -ss 10 -t 20 \
 	-i "${input_uri}" \
    -c:v libx264 -crf 23 -preset veryfast \
    -g 30 \
    -c:a aac -b:a 128k -ac 2 \
    -f hls \
    -hls_time 4 \
    -hls_playlist_type vod \
    -hls_segment_filename "${segment_filename}" \
    -hls_list_size 0 \
    "${output_filename}"

# vim:nu:tabstop=4:shiftwidth=4:ft=sh
