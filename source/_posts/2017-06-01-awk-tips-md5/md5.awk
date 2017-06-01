#!/usr/bin/awk -f

BEGIN {
    md5cmd = "md5sum";
}

{
    printf $2 |& md5cmd;
    close(md5cmd, "to");

    md5cmd |& getline md5result;
	close(md5cmd);

    print $1, substr(md5result, 1, 32);
}
