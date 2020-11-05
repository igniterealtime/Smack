#!/usr/bin/env bash

git shortlog -s |\
	cut -f2- |\
	grep -v '(no author)' |\
	grep '\w \w.*' |\
	sort
