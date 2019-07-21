#!/usr/bin/env bash

#
# Author: Elvis Wang <mail _AT_ wangbo _DOT_ im>
#
# Typical usage: ./vsync_ffprobe.sh path1_to_video.mp4 path2_to_video.mp4 [...]
#

#
# Enable strict bash mode
set -euo pipefail
IFS=$'\t\n'

# Uncomment this line to print command args instead of executing it
# set -x

FFPROBE=(
    "ffprobe"
    "-hide_banner"
    "-loglevel" "warning"
    "-show_streams"
    "-show_format"
    "-of" "json=compact=1"
)

JQ=(
    "jq"
)

#
# Main entry
#

for each_file in "$@"; do
    output_filename_prefix="${each_file}"
    output_filename_prefix="${output_filename_prefix%.*}"
    output_filename_prefix="${output_filename_prefix##*/}"

    ffprobe_cmds=()

    for each_arg in "${FFPROBE[@]}"; do
        ffprobe_cmds[${#ffprobe_cmds[@]}]="${each_arg}"
    done

    ffprobe_cmds[${#ffprobe_cmds[@]}]="${each_file}"

    output_file="${output_filename_prefix}.probe"

    echo "==> Probe \"${each_file}\""
    "${ffprobe_cmds[@]}"
    # "${ffprobe_cmds[@]}" | "${JQ[@]}" '.streams[] | select(.codec_type == "video") | .r_frame_rate'
done

# vim:nu:tabstop=4:shiftwidth=4:ft=sh
