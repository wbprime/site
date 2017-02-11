#
# Author: Elvis Wang <mail#wbprime#me>
#

SUBDIRS = src

PREFIX                      ?= $(PWD)
export WORKING_DIR          := $(PWD)
export OUTPUT_ROOT_DIR      := $(PREFIX)/htmls
export PROJECT_RELATIVE_DIR := # Empty value

export MAKEINCLUDE := $(PWD)/conf/config.mk

export MARKDOWN_TO_HTML_OPTS := $(MARKDOWN_TO_HTML_OPTS) -c $(WORKING_DIR)/css/panam.css

all: subdirs

clean: clean_subdirs

install: install_subdirs

include $(MAKEINCLUDE)
