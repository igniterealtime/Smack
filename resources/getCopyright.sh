#!/bin/bash

SCRIPTDIR="$(dirname ${BASH_SOURCE[0]})"
BASEDIR=${SCRIPTDIR}/..

cd $BASEDIR
SUBPROJECTS=$(grep -oP "\'.*\'" settings.gradle | sed "s;';;g")
for p in $SUBPROJECTS; do
	echo "Copyright notices for $p"
	# Find all .java files in the project
	find $p/src -type f -name "*.java" -print0 | \
		# Get the project string
		xargs -0 grep -ohP '^.*\* Copyright \K.*' | \
		# Sort the output
		sort | \
		# Remove duplicates
		uniq | \
		# Split multi Copyright statemtents, e.g. "2001-2013 FooBar, 2014 Baz"
	    tr ',' '\n' | \
		# Remove whitespaces resulting from the previous split
		sed "s/^[ \t]*//" | \
		# Remove dots at the end and '©' at the beginning
		sed "s/^© //" | sed "s/\.$//" | sed "s/^(C) //"
	echo -ne "\n"
done
