#
# Author: Elvis Wang <mail#wbprime#me>
#

.PHONY: subdirs $(SUBDIRS)
subdirs: $(SUBDIRS)
$(SUBDIRS): 
	$(MAKE) -C $@ all

CLEAN_SUBDIRS = $(addprefix clean_,$(SUBDIRS))
.PHONY: clean_subdirs $(CLEAN_SUBDIRS)
clean_subdirs: $(CLEAN_SUBDIRS)
$(CLEAN_SUBDIRS):
	$(MAKE) -C $(subst clean_,,$@) clean

INSTALL_SUBDIRS = $(addprefix install_,$(SUBDIRS))
.PHONY: install_subdirs $(INSTALL_SUBDIRS)
install_subdirs: $(INSTALL_SUBDIRS)
$(INSTALL_SUBDIRS):
	$(MAKE) -C $(subst install_,,$@) install
