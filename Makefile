GRADLE ?= ./gradlew

.PHONY: all
all: check format jacocoRootReport javadocAll sinttest

.PHONY: codecov
codecov:
	$(GRADLE) smack-java11-full:testCodeCoverageReport
	echo "Report available at smack-java11-full/build/reports/jacoco/testCodeCoverageReport/html/index.html"

.PHONY: check
check:
	$(GRADLE) $@

.PHONY: format
format:
	$(GRADLE) spotlessApply

.PHONY: eclipse
eclipse:
	$(GRADLE) $@

.PHONY: sinttest
sinttest:
	$(GRADLE) $@

.PHONY: jacocoRootReport
jacocoRootReport:
	$(GRADLE) $@

.PHONY: javadocAll
javadocAll:
	$(GRADLE) $@
	echo "Smack javadoc available at build/javadoc/index.html"
