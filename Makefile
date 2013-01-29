.PHONY: all clean test-unit eclipse

all: build-smack .settings

# Can not use 'build' as target name, because there is a
# directory called build
build-smack:
	cd build && ant

clean:
	cd build && ant clean

test-unit:
	cd build && ant test-unit

eclipse: .settings .classpath .project

.settings:
	ln -s build/eclipse/settings .settings

.classpath:
	ln -s build/eclipse/classpath .classpath

.project:
	ln -s build/eclipse/project .project
