---
title: JDK installation on Archlinux
date: 2015-05-25 11:07:13
updated: 2015-05-25 11:07:13
categories: ["Tech"]
tags: [java, JDK, archlinux]
description: "Step by step guide to install a JDK 7/8 on Archlinux."
---

Basically, [Archlinux](https://www.archlinux.org) provide OpenJDK 7/8 in [official repository](https://wiki.archlinux.org/index.php/Java).  Java JDK environment will be setup in quite a little minutes follwing Archliux official guide. 

However, some java programmes does not work well on OpenJDK, and achives better performance on Sun/Oracle JDK.  For example, [Intellij Idea](https://www.archlinux.org/packages/community/any/intellij-idea-community-edition/) in Archlinux official repository produces warning message blaming OpenJDK.  So Sun/Oracle JDK maybe welcomed by some users who wanted a clean installation of Intellij Idea.

1. Choose and download the JDK version you want to install.  

    Offical supported [JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html) version is 1.7 and 1.8.  This post covers both of them.

        JDK 1.7 : jdk-7u79-linux-x64.tar.gz
        JDK 1.8 : jdk-8u45-linux-x64.tar.gz

    Note: according to your platform (i686 or X86_64), the subversion string (7u79 or 8u45) and (x64) may differ in file names.

2. Extract JDK files into installation directory.

        tar zxvf jdk-7u79-linux-x64.tar.gz -C /opt

    or 

        tar zxvf jdk-8u45-linux-x64.tar.gz -C /opt

    Suppose your jdk is extracted into /opt/jdk.

3. Setup JAVA_HOME variable and add executables into PATH.

        touch /etc/profile.d/java.sh

    Edit java.sh.

        #!/bin/bash
        export JAVA_HOME=/opt/jdk
        export PATH=$PATH:$JAVA_HOME/jre/bin
        
    Finally, set java.sh executable.

        chmod u+x /etc/profile.d/java.sh

4. Test your installation.

        java -version

    Output may look like:

        java version "1.7.0_79"
        Java(TM) SE Runtime Environment (build 1.7.0_79-b15)
        Java HotSpot(TM) 64-Bit Server VM (build 24.79-b02, mixed mode)

5. Install a java IDE and start you java developing trip.

    [Eclipse](http://www.eclipse.org) is most widely used by Java developers.

    [Intellij Idea](https://www.jetbrains.com/idea) works more geek.

    [NetBeans](https://netbeans.org) is a product of Oracle.

    I used Intellij Idea right now, and it works well.

6. Enjoy yourself.
