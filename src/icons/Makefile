#
# Author: Elvis Wang <mail#wbprime#me>
#

SUBDIRS = # No subdirs

export PROJECT_RELATIVE_DIR := $(PROJECT_RELATIVE_DIR)icons

ALL_LIST := favicon.ico logo220.png

all: 
	echo All in icons

clean: 
	echo Clean in icons

INSTALL_ALL = $(addprefix install_, $(ALL_LIST))
.PHONY: install mk_dest_dir $(INSTALL_ALL)
install: mk_dest_dir $(INSTALL_ALL)
	echo Install in src/css

mk_dest_dir:
	mkdir -p $(OUTPUT_ROOT_DIR)/$(PROJECT_RELATIVE_DIR)

$(INSTALL_ALL):
	cp -f $(subst install_,,$@) $(OUTPUT_ROOT_DIR)/$(PROJECT_RELATIVE_DIR)

include $(MAKEINCLUDE)
