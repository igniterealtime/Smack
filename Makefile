.PHONY: all clean javadoc test-unit eclipse

export JAVA_TOOL_OPTIONS:='-Dfile.encoding=iso-8859-1'

all: build-smack

# Can not use 'build' as target name, because there is a
# directory called build
build-smack:
	cd build && ant

clean:
	cd build && ant clean

unit-test:
	cd build && ant test-unit

integration-test:
	cd build && ant test
	
javadoc:
	cd build && ant javadoc

eclipse: .settings .classpath .project

.settings:
	ln -s build/eclipse/settings .settings

.classpath:
	ln -s build/eclipse/classpath .classpath

.project:
	ln -s build/eclipse/project .project
