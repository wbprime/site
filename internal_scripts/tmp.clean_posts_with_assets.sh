#!/usr/bin/env bash

#
# Enable strict bash mode
set -euo pipefail
IFS=$'\t\n'

for each_dir in "${1}"/*;  do
    if [ -d "${each_dir}" ]; then
        for each_dir2 in "${each_dir}"/*; do
            echo "${each_dir2}"
            if [ "index.md" = "${each_dir2}" ]; then echo "${each_dir2}"; fi
            if [ "_index.md" = "${each_dir2}" ]; then echo "${each_dir2}"; fi
        done
    fi
done

# vim:nu:tabstop=4:shiftwidth=4:ft=sh
