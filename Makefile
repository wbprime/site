#
# Author: Elvis Wang <mail#wbprime#me>
#

OUTPUT_DIR := html
CSS_DIR := css
POSTS_DIR := posts

CSS_LIST := panam.css
MARKDOWN_TO_HTML_CMD := pandoc --highlight-style=tango --toc -f markdown -t html5

POSTS_SOURCE_FILE_LIST := $(wildcard $(POSTS_DIR)/*.md)
CSS_SOURCE_FILE_LIST := $(addprefix $(CSS_DIR)/,$(CSS_LIST))

POSTS_OUTPUT_DIR := $(OUTPUT_DIR)/$(POSTS_DIR)
POSTS_OUTPUT_FILE_LIST := $(addprefix $(POSTS_OUTPUT_DIR)/,$(patsubst %.md,%.html,$(notdir $(POSTS_SOURCE_FILE_LIST))))

CSS_FILES_OPTIONS := $(addprefix -c /_css/,$(CSS_LIST))

all: update
	@echo "Source posts: " $(POSTS_SOURCE_FILE_LIST)
	@echo "Dest posts: " $(POSTS_OUTPUT_FILE_LIST)
	@echo "Source css: " $(CSS_SOURCE_FILE_LIST)

update: 
# update: generate_posts copy_posts_rc generate_pages copy_pages_rc

generate_posts: $(POSTS_OUTPUT_FILE_LIST)

$(POSTS_OUTPUT_FILE_LIST): $(POSTS_OUTPUT_DIR)/%.html : $(POSTS_DIR)/%.md
	$(MARKDOWN_TO_HTML_CMD) $(CSS_FILES_OPTIONS) $< -o $@

clean:
	rm -rf $(OUTPUT_DIR)

.PHONY: all update generate_posts clean
