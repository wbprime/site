#!/usr/bin/env -S awk -f


#
# Change lines like:
#
# ---
# title: "Jvm Memory Model"
# date: 2018-02-05T16:47:59+08:00
# categories: "Notes"
# tags: ["jvm", "memory model"]
# description: "Note on JVM memory model"
# draft: false
# ---
#
# To
#
# +++
# title = "Jvm Memory Model"
# description = "Note on JVM memory model"
# date = 2018-02-05T16:47:59+08:00
# draft = false
# [taxonomies]
# categories = "Notes"
# tags = ["jvm", "memory model"]
# +++
#

BEGIN {beg = 0; end = 0; FS="";} 

{
    if (beg == 1) {
        if (end == 0) {
            vars[$1] = $2;
        } else {
            print $0;
        }
    }
}

/^---$/ { 
    if (beg == 0) {
        beg = 1;

        FS = " : ";

        print "+++";
    } else {
        end = 1;

        var_name = "title";
        print var_name, "=", vars[var_name];
        var_name = "description";
        if ( vars[var_name] != "" ) {
            print var_name, "=", vars[var_name];
        } else {
            print var_name, "=", vars["title"];
        }
        var_name = "date";
        print var_name, "=", vars[var_name];
        var_name = "draft";
        print var_name, "= false";

        print "[taxonomies]";
        var_name = "categories";
        print var_name, "= ", vars[var_name];
        var_name = "tags";
        print var_name, "=", vars[var_name];

        print "+++";

        FS = ":::::::::::::::::::::::::::::::::::::::::::::::::::::";
    }
}

