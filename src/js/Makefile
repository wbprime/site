#
# Author: Elvis Wang <mail#wbprime#me>
#

SUBDIRS = # No subdirs

export PROJECT_RELATIVE_DIR := $(PROJECT_RELATIVE_DIR)js

ALL_LIST := # No js files

all: 
	echo All in src/js

clean:
	echo Clean in src/js

INSTALL_ALL = $(addprefix install_, $(ALL_LIST))
.PHONY: install mk_dest_dir $(INSTALL_ALL)
install: mk_dest_dir $(INSTALL_ALL)

mk_dest_dir:
	mkdir -p $(OUTPUT_ROOT_DIR)/$(PROJECT_RELATIVE_DIR)

$(INSTALL_ALL):
	cp -f $(subst install_,,$@) $(OUTPUT_ROOT_DIR)/$(PROJECT_RELATIVE_DIR)

include $(MAKEINCLUDE)
