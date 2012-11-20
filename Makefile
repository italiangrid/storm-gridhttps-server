mavenrepo=/tmp/m2-repository
release=1
version:=$(shell mvn -s settings.xml -Dmaven.repo.local=$(mavenrepo) org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression="project.version" | grep -v '\[')

# mvn settings mirror conf url
mirror_conf_url=https://raw.github.com/italiangrid/build-settings/master/maven/cnaf-mirror-settings.xml

# name of the mirror settings file
mirror_conf_name=mirror-settings.xml

mvn_settings=-s $(mirror_conf_name)
all: bin_rpm

src_rpm: build_sources
	mkdir -p rpmbuild/BUILD rpmbuild/RPMS rpmbuild/SOURCES rpmbuild/SPECS rpmbuild/SRPMS
	cp target/storm-gridhttps-server-${version}-src.tar.gz rpmbuild/SOURCES/storm-gridhttps-server-${version}.tar.gz
	rpmbuild -bs --define "_topdir ${PWD}/rpmbuild" target/spec/storm-gridhttps-server.spec

build_sources: prepare clean
	mvn ${mvn_settings} -Dmaven.repo.local=$(mavenrepo) -Drelease=$(release) -Dmaven.settings=$(mvn_settings) process-sources

bin_rpm: src_rpm
	rpmbuild --rebuild --define "_topdir ${PWD}/rpmbuild" rpmbuild/SRPMS/storm-gridhttps-server-${version}-$(release).src.rpm

clean: prepare
	rm -rf rpmbuild
	mvn ${mvn_settings} -Dmaven.repo.local=$(mavenrepo) clean
	rm -f ${mirror_conf_name}

prepare:
	wget $(mirror_conf_url) -O $(mirror_conf_name)