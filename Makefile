#
# Author: Elvis Wang <mail#wbprime#me>
#

OUTPUT_DIR := html

CSS_DIR := css
IMG_DIR := images
POSTS_DIR := posts

CSS_LIST := panam.css
MARKDOWN_TO_HTML_CMD := pandoc --highlight-style=tango --toc -f markdown -t html5

POSTS_SOURCE_FILE_LIST := $(wildcard $(POSTS_DIR)/*.md)
IMG_SOURCE_FILE_LIST := $(wildcard $(IMG_DIR)/*)
CSS_SOURCE_FILE_LIST := $(addprefix $(CSS_DIR)/,$(CSS_LIST))

POSTS_OUTPUT_DIR := $(OUTPUT_DIR)/$(POSTS_DIR)
POSTS_OUTPUT_FILE_LIST := $(addprefix $(POSTS_OUTPUT_DIR)/,$(patsubst %.md,%.html,$(notdir $(POSTS_SOURCE_FILE_LIST))))

IMG_OUTPUT_DIR := $(OUTPUT_DIR)/$(IMG_DIR)
IMG_OUTPUT_FILE_LIST := $(addprefix $(OUTPUT_DIR)/,$(IMG_SOURCE_FILE_LIST))

CSS_OUTPUT_DIR := $(OUTPUT_DIR)/$(CSS_DIR)
CSS_OUTPUT_FILE_LIST := $(addprefix $(CSS_OUTPUT_DIR)/,$(CSS_LIST))

CSS_FILES_OPTIONS := $(addprefix -c /_css/,$(CSS_LIST))

all: update
#all: update
#	@echo "Source posts: " $(POSTS_SOURCE_FILE_LIST)
#	@echo "Dest posts: " $(POSTS_OUTPUT_FILE_LIST)
#	@echo "Source css: " $(CSS_SOURCE_FILE_LIST)
#	@echo "Dest css: " $(CSS_OUTPUT_FILE_LIST)
#	@echo "Source images: " $(IMG_SOURCE_FILE_LIST)
#	@echo "Dest images: " $(IMG_OUTPUT_FILE_LIST)

update: prepare_output_dirs copy_posts_rc copy_css generate_posts copy_images

prepare_output_dirs: $(CSS_OUTPUT_DIR) $(POSTS_OUTPUT_DIR) $(IMG_OUTPUT_DIR)

$(POSTS_OUTPUT_DIR) $(CSS_OUTPUT_DIR) $(IMG_OUTPUT_DIR):
	mkdir -p $@

generate_posts: $(POSTS_OUTPUT_FILE_LIST)

$(POSTS_OUTPUT_FILE_LIST): $(POSTS_OUTPUT_DIR)/%.html : $(POSTS_DIR)/%.md
	$(MARKDOWN_TO_HTML_CMD) $(CSS_FILES_OPTIONS) $< -o $@

copy_posts_rc: 
	@cp -rf $(POSTS_DIR) $(OUTPUT_DIR)
	@rm -rf $(POSTS_OUTPUT_DIR)/*.md

copy_css: $(CSS_OUTPUT_FILE_LIST)

$(CSS_OUTPUT_FILE_LIST): $(CSS_OUTPUT_DIR)/%.css: $(CSS_DIR)/%.css
	cp -f $< $@

copy_images: $(IMG_OUTPUT_FILE_LIST)

$(IMG_OUTPUT_FILE_LIST): $(IMG_OUTPUT_DIR)/%: $(IMG_DIR)/%
	cp -f $< $@

clean:
	rm -rf $(OUTPUT_DIR)

.PHONY: all update prepare_output_dirs copy_posts_rc copy_css copy_images generate_posts clean
