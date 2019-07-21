#!/usr/bin/env bash

#
# Author: Elvis Wang <mail _AT_ wangbo _DOT_ im>
#
# Typical usage: ./vsync_transcode.sh path_to_video.mp4 passthrough cfr vfr drop
#

#
# Enable strict bash mode
set -euo pipefail
IFS=$'\t\n'

# Input video fps -> 30
TARGET_FPS=(
    "45" # greater than source video fps
    "24" # less than source video fps
)

FFMPEG=(
    # "echo" # Uncomment this line to print command args instead of executing it
    "ffmpeg"
    "-y"
    "-hide_banner"
    "-loglevel"
    "warning"
    "-y"
)

shared_input_args=(
    "-ss" "10"
    "-t" "5"
)

shared_output_args=(
    "-c:a"
    "copy"
    "-c:v"
    "libx264"
    "-s"
    "1280x720"
)

#
# Main entry
#

input_video_filepath="${1:?No video file specified}"
shift

output_filename_prefix="${input_video_filepath}"
output_filename_prefix="${output_filename_prefix%.*}"
output_filename_prefix="${output_filename_prefix##*/}"

declare -A vsync_modes
vsync_modes["passthrough"]="passthrough"
vsync_modes["cfr"]="cfr"
vsync_modes["vfr"]="vfr"
vsync_modes["drop"]="drop"
vsync_modes["0"]="passthrough"
vsync_modes["1"]="cfr"
vsync_modes["2"]="vfr"

target_modes=()
for each_arg in "${@}"; do
    set +u
    each_mode=${vsync_modes["${each_arg}"]}
    set -u
    if [ -z "${each_mode}" ]; then
        echo "vsync mode \"${each_arg}\" not recoganized!"
        exit 1
    fi

    echo "Collected vsync mode \"${each_mode}\""
    target_modes[${#target_modes[@]}]="${each_mode}"
done

for each_mode in "${target_modes[@]}"; do
    ffmpeg_cmds=()

    for each_arg in "${FFMPEG[@]}"; do
        ffmpeg_cmds[${#ffmpeg_cmds[@]}]="${each_arg}"
    done

    for each_arg in "${shared_input_args[@]}"; do
        ffmpeg_cmds[${#ffmpeg_cmds[@]}]="${each_arg}"
    done

    ffmpeg_cmds[${#ffmpeg_cmds[@]}]="-i"
    ffmpeg_cmds[${#ffmpeg_cmds[@]}]="${input_video_filepath}"

    for each_arg in "${shared_output_args[@]}"; do
        ffmpeg_cmds[${#ffmpeg_cmds[@]}]="${each_arg}"
    done

    for each_fps in "${TARGET_FPS[@]}"; do
        final_cmds=()
        for each_arg in "${ffmpeg_cmds[@]}"; do
            final_cmds[${#final_cmds[@]}]="${each_arg}"
        done

        final_cmds[${#final_cmds[@]}]="-vsync"
        final_cmds[${#final_cmds[@]}]="${each_mode}"
        final_cmds[${#final_cmds[@]}]="-r"
        final_cmds[${#final_cmds[@]}]="${each_fps}"

        final_cmds[${#final_cmds[@]}]="output.${each_mode}.fps${each_fps}.mp4"

        echo "==> Process fps = \"${each_fps}\" within vsync mode \"${each_mode}\":"
        for each_arg in "${final_cmds[@]}"; do
            echo "===>>> Arg: \"${each_arg}\""
        done

        "${final_cmds[@]}"
    done
done

# vim:nu:tabstop=4:shiftwidth=4:ft=sh
