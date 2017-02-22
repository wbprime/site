#
# Author: Elvis Wang <mail#wbprime#me>
#

SUBDIRS = # No subdirs

export PROJECT_RELATIVE_DIR := $(PROJECT_RELATIVE_DIR)posts

HTML_LIST = $(subst .md,.html,$(SRC_LIST))
SRC_LIST := \
	2013-05-16-2012.md \
	2013-05-16-cangjie.md \
	2013-05-16-chunzhen.md \
	2013-05-16-fengkuangshijie.md \
	2013-05-16-houqingchunqi-de-shi.md \
	2013-05-16-jiuhaoqiu.md \
	2013-05-16-milaoshu.md \
	2013-05-16-nuoya-fangzhou.md \
	2013-05-16-shengmingzhong-you-yizhong-juedui.md \
	2013-05-16-shiguangji.md \
	2013-05-16-turan-hao-xiangni.md \
	2013-05-16-women.md \
	2013-05-16-woxinzhong-shangwei-benghuai-de-difang.md \
	2013-05-16-xiaoshihou.md \
	2013-05-16-xingkong.md \
	2013-05-16-xiyiji.md \
	2013-05-16-yikepingguo.md \
	2013-05-16-zhizu.md \
	2014-05-16-jianchizhu-waipo.md \
	2014-05-16-turan-xiangqi-zhushi-lai.md \
	2015-05-16-baiyun-yijiu-zai.md \
	2015-05-16-c-mantan.md \
	2015-05-16-danan.md \
	2015-05-16-feichangliujiayi.md \
	2015-05-16-Hello-World.md \
	2015-05-16-Ji-Huairou-Shaokao.md \
	2015-05-16-kan-dianying.md \
	2015-05-16-man-man-zou.md \
	2015-05-16-qiangjinjiu.md \
	2015-05-16-Qilai-live.md \
	2015-05-16-Qusuodui.md \
	2015-05-16-shuohaode-anjingde-meinanzi-ne.md \
	2015-05-16-Suoban-qushi.md \
	2015-05-16-xiaowang-ersanshi.md \
	2015-05-25-JDK-installation-on-Archlinux.md \
	2015-05-28-Spring-At-First-Glance.md \
	2015-06-02-Spring-XML-based-configuration.md \
	2015-06-08-Spring-Annotation-based-configuration.md \
	2016-03-30-java-concurrency-countdownlatch.md \
	2016-03-30-java-concurrency-cyclicbarrier.md \
	2016-03-30-java-concurrent-semaphore.md \
	2016-03-31-Java-concurrency-content.md \
	2016-04-01-java-concurrency-synchronized.md \
	2016-04-06-learning-java-concurrency-wait-notify.md \
	2016-04-07-learning-java-concurrency-reentrantlock-condition.md \
	2016-04-07-learning-java-concurrency-reentrantreadwritelock.md \
	2016-04-09-spring-mvc-testing-content.md \
	2016-04-09-spring-mvc-testing-integration-testing-configuration.md \
	2016-04-09-spring-mvc-testing-integration-testing-controllers.md \
	2016-04-09-spring-mvc-testing-integration-testing-forms.md \
	2016-04-09-spring-mvc-testing-integration-testing-rest-api.md \
	2016-04-09-spring-mvc-testing-unit-testing-configuration.md \
	2016-04-09-spring-mvc-testing-unit-testing-normal-controllers.md \
	2016-04-09-spring-mvc-testing-unit-testing-rest-api.md \
	2016-04-11-learning-java-concurrency-thread-runnable.md \
	2016-04-13-learning-java-concurrency-futuretask-callable.md \
	2016-04-15-learning-java-concurrency-executors-1-executorservice.md \

DIR_LIST = \
	2016-03-30-java-concurrency-countdownlatch.dir \
	2016-03-30-java-concurrency-cyclicbarrier.dir \
	2016-03-30-java-concurrent-semaphore.dir \
	2016-04-06-learning-java-concurrency-wait-notify.dir \
	2016-04-07-learning-java-concurrency-reentrantlock-condition.dir \
	2016-04-07-learning-java-concurrency-reentrantreadwritelock.dir \
	2016-04-11-learning-java-concurrency-thread-runnable.dir \
	2016-04-13-learning-java-concurrency-futuretask-callable.dir \

all: $(HTML_LIST)

CLEAN_PREFIX := clean_
CLEAN_ALL = $(addprefix $(CLEAN_PREFIX), $(SRC_LIST))
.PHONY: clean $(CLEAN_ALL)
clean: $(CLEAN_ALL)
	echo Clean in src/posts

$(CLEAN_ALL): $(CLEAN_PREFIX)%.md : %.md
	rm -rf $*.html

INSTALL_PREFIX := install_
INSTALL_ALL = $(addprefix $(INSTALL_PREFIX), $(SRC_LIST))
.PHONY: install mk_dest_dir $(INSTALL_ALL)
install: mk_dest_dir $(INSTALL_ALL)
	echo Install in src/css

mk_dest_dir:
	mkdir -p $(OUTPUT_ROOT_DIR)/$(PROJECT_RELATIVE_DIR)

$(INSTALL_ALL): $(INSTALL_PREFIX)%.md : %.html
	cp -f $< $(OUTPUT_ROOT_DIR)/$(PROJECT_RELATIVE_DIR)

$(HTML_LIST): %.html: %.md
	$(MARKDOWN_TO_HTML_CMD) $(MARKDOWN_TO_HTML_OPTS) -o $@ $<

include $(MAKEINCLUDE)
