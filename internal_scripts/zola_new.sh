#!/usr/bin/env bash

#
# Enable strict bash mode
set -euo pipefail
# IFS=$'\t\n'

SCRIPT_PATH=`readlink -f $0`
SCRIPT_DIR=`dirname ${SCRIPT_PATH}`
ROOT_DIR=`dirname ${SCRIPT_DIR}`

POSTS_DIR="${ROOT_DIR}/content/posts"

post_title="$*"
post_title="${post_title:-Demo Draft}"

post_date="`date +%FT%T`+08:00"
post_name=`date +%F`
for each_arg in ${post_title}; do
    post_name="${post_name}-${each_arg}"
done
post_name="${post_name}.md"

draft_path="${POSTS_DIR}/${post_name}"

cat - <<EOF > "${draft_path}"
+++
title = "$post_title"
description = "$post_title"
date = "$post_date"
draft = false
# template = "page.html"
[taxonomies]
categories =  ["category here"]
tags = ["tag 1", "tag 2", "tag 3"]
+++

Content goes here.

EOF

echo "Create new post: \"${draft_path}\""

# vim:nu:tabstop=4:shiftwidth=4:ft=sh
