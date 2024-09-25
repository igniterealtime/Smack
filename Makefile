GRADLE ?= ./gradlew

.PHONY: all
all: check jacocoRootReport javadocAll sinttest

.PHONY: codecov
codecov:
	$(GRADLE) smack-java8-full:testCodeCoverageReport
	echo "Report available at smack-java8-full/build/reports/jacoco/testCodeCoverageReport/html/index.html"

.PHONY: check
check:
	$(GRADLE) $@

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
