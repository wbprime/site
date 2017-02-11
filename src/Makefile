#
# Author: Elvis Wang <mail#wbprime#me>
#

SUBDIRS = css js icons pages posts

export PROJECT_RELATIVE_DIR := $(PROJECT_RELATIVE_DIR)src/

all: subdirs
	echo $(SUBDIRS)
	echo $(INSTALL_SUBDIRS)
	echo $(CLEAN_SUBDIRS)

clean: clean_subdirs

install: install_subdirs

include $(MAKEINCLUDE)
