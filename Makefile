GRADLE ?= ./gradlew

.PHONY: all
all: check codecov eclipse javadocAll inttestFull show-dependency-updates

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

.PHONY: inttestFull
inttestFull:
	$(GRADLE) $@

.PHONY: javadocAll
javadocAll:
	$(GRADLE) $@
	echo "javadoc available at file://$(PWD)/build/javadoc/index.html"

.PHONY: show-dependency-updates
show-dependency-updates:
	$(GRADLE) dependencyUpdates

.PHONY: jmh
jmh:
	$(GRADLE) smack-core:jmh
