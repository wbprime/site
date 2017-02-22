#
# Author: Elvis Wang <mail#wbprime#me>
#

SUBDIRS = src

PREFIX                      ?= $(PWD)
export WORKING_DIR          := $(PWD)
export OUTPUT_ROOT_DIR      := $(PREFIX)/htmls
export PROJECT_RELATIVE_DIR := # Empty value

export MAKEINCLUDE := $(PWD)/conf/config.mk

#
# Markdown to html
#
# Currently use pandoc to generate htmls from markdown files
# See http://pandoc.org for more details
export MARKDOWN_TO_HTML_CMD  := /usr/bin/pandoc
export MARKDOWN_TO_HTML_OPTS :=    \
	--highlight-style=tango        \
	--toc                          \
	--id-prefix wbid_ 			   \
	--title-prefix wbprime 		   \
	--template wbprime			   \
	-M "author=Elvis Wang"		   \
	-f markdown -t html5           \
	--data-dir=$(WORKING_DIR)/data \
	-c $(WORKING_DIR)/css/panam.css 

all: subdirs

clean: clean_subdirs
	rm -rf $(OUTPUT_ROOT_DIR)

install: install_subdirs

include $(MAKEINCLUDE)
