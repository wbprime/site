#!/usr/bin/env bash

#
# Enable strict bash mode
set -euo pipefail
IFS=$'\t\n'

awk_script="${0%.sh}.awk"
for each_f in "${1}"/*.md;  do
    if [ -f "${each_f}" ]; then
        alternate_f="${each_f}.save"

        cp -f "${each_f}" "${alternate_f}"
        awk -f "${awk_script}" "${alternate_f}" > "${each_f}"
    fi
done

for each_dir in "${1}"/*;  do
    if [ -d "${each_dir}" ]; then
        echo "${each_dir}"
    fi
done

# vim:nu:tabstop=4:shiftwidth=4:ft=sh
