#!/usr/bin/env bash

#
# Author: Elvis Wang <mail _AT_ wangbo _DOT_ im>
#

#
# Enable strict bash mode
set -euo pipefail
IFS=$'\t\n'

OUTPUT_FORMATS=(
    "jpg"
    "png"
    "gif"
    "webp"
)

speed="2" # for animation output, frames per second
select_filter="gte(t,selected_n\\*5\\+start_t)" # select frames every 5 seconds

OP_TYPE="interval"

FFMPEG=(
    # "echo" # Uncomment this line to print command args instead of executing it
    "ffmpeg"
    "-hide_banner"
    "-loglevel"
    "warning"
    "-y"
)

format_shared_input_args=(
    "-ss" "30"
    "-t" "30"
)

format_shared_output_args=(
    "-frames:v"
    "100"
)

jpg_format_specified_args=(
    "-filter" "select='${select_filter}'"
    "-vsync" "vfr"
    "-f" "image2"
    "-q:v" "2"
)
png_format_specified_args=(
    "-filter" "select='${select_filter}'"
    "-vsync" "vfr"
    "-f" "image2"
)
gif_format_specified_args=(
    "-filter" "select='${select_filter}',split[x1][x2];[x1]palettegen[p];[x2][p]paletteuse,setpts=expr=N/(${speed}*TB)"
    "-r" "${speed}"
    "-f" "gif"
    "-loop" "0"
)
webp_format_specified_args=(
    "-filter" "select='${select_filter}',setpts=expr=N/(${speed}*TB)"
    "-r" "${speed}"
    "-f" "webp"
    "-c:v" "libwebp"
    "-loop" "0"
    "-lossless" "0"
)

jpg_format_filename_suffix="%d.${OP_TYPE}.jpg"
png_format_filename_suffix="%d.${OP_TYPE}.png"
gif_format_filename_suffix="${OP_TYPE}.gif"
webp_format_filename_suffix="${OP_TYPE}.webp"

#
# Main entry
#

input_video_filepath="${1:?No video file specified}"
shift

output_filename_prefix="${input_video_filepath}"
output_filename_prefix="${output_filename_prefix%.*}"
output_filename_prefix="${output_filename_prefix##*/}"

for each_format in "${@}"; do
    ffmpeg_cmds=()

    for each_arg in "${FFMPEG[@]}"; do
        ffmpeg_cmds[${#ffmpeg_cmds[@]}]="${each_arg}"
    done

    for each_arg in "${format_shared_input_args[@]}"; do
        ffmpeg_cmds[${#ffmpeg_cmds[@]}]="${each_arg}"
    done

    ffmpeg_cmds[${#ffmpeg_cmds[@]}]="-i"
    ffmpeg_cmds[${#ffmpeg_cmds[@]}]="${input_video_filepath}"

    for each_arg in "${format_shared_output_args[@]}"; do
        ffmpeg_cmds[${#ffmpeg_cmds[@]}]="${each_arg}"
    done

    specified_args_var="${each_format}_format_specified_args[@]"
    for each_arg in "${!specified_args_var}"; do
        ffmpeg_cmds[${#ffmpeg_cmds[@]}]="${each_arg}"
    done

    output_filename_var="${each_format}_format_filename_suffix"
    ffmpeg_cmds[${#ffmpeg_cmds[@]}]="${output_filename_prefix}.${!output_filename_var}"

    echo "==>> Process \"${OP_TYPE}\" for format \"${each_format}\":"
    for each_arg in "${ffmpeg_cmds[@]}"; do
        echo "===>>> Arg: \"${each_arg}\""
    done

    "${ffmpeg_cmds[@]}"
done

# vim:nu:tabstop=4:shiftwidth=4:ft=sh
