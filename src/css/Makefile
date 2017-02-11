#
# Author: Elvis Wang <mail#wbprime#me>
#

SUBDIRS = # No subdirs

export PROJECT_RELATIVE_DIR := $(PROJECT_RELATIVE_DIR)css

ALL_LIST := github-pandoc.css panam.css

all: 
	echo All in src/css

clean:
	echo Clean in src/css

INSTALL_ALL = $(addprefix install_, $(ALL_LIST))
.PHONY: install mk_dest_dir $(INSTALL_ALL)
install: mk_dest_dir $(INSTALL_ALL)
	echo Install in src/css

mk_dest_dir:
	mkdir -p $(OUTPUT_ROOT_DIR)/$(PROJECT_RELATIVE_DIR)

$(INSTALL_ALL):
	cp -f $(subst install_,,$@) $(OUTPUT_ROOT_DIR)/$(PROJECT_RELATIVE_DIR)

include $(MAKEINCLUDE)
