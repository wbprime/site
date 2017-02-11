#
# Author: Elvis Wang <mail#wbprime#me>
#

OUTPUT_ROOT_DIR := htmls

#
# Markdown to html
#
# Currently use pandoc to generate htmls from markdown files
# See http://pandoc.org for more details
MARKDOWN_TO_HTML_CMD   := /usr/bin/pandoc
MARKDOWN_TO_HTML_OPTS  := --highlight-style=tango --toc -f markdown -t html5

%.html: %.md
	$(MARKDOWN_TO_HTML_CMD) $(MARKDOWN_TO_HTML_OPTS) -o $@ $<

.PHONY: subdirs $(SUBDIRS)
subdirs: $(SUBDIRS)
$(SUBDIRS): 
	$(MAKE) -C $@ all

CLEAN_SUBDIRS = $(addprefix clean_,$(SUBDIRS))
.PHONY: clean_subdirs $(CLEAN_SUBDIRS)
clean_subdirs: $(CLEAN_SUBDIRS)
$(CLEAN_SUBDIRS):
	$(MAKE) -C $@ clean

INSTALL_SUBDIRS = $(addprefix install_,$(SUBDIRS))
.PHONY: install_subdirs $(INSTALL_SUBDIRS)
install_subdirs: $(INSTALL_SUBDIRS)
$(INSTALL_SUBDIRS):
	$(MAKE) -C $@ install
