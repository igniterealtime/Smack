GRADLE ?= ./gradlew

.PHONY: all
all: check codecov eclipse javadocAll sinttest

.PHONY: codecov
codecov:
	$(GRADLE) smack-java11-full:testCodeCoverageReport
	echo "code coverage report available at file://$(PWD)/smack-java11-full/build/reports/jacoco/testCodeCoverageReport/html/index.html"

.PHONY: check
check:
	$(GRADLE) $@

.PHONY: eclipse
eclipse:
	$(GRADLE) $@

.PHONY: sinttest
sinttest:
	$(GRADLE) $@

.PHONY: javadocAll
javadocAll:
	$(GRADLE) $@
	echo "javadoc available at file://$(PWD)/build/javadoc/index.html"
