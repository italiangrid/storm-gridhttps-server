mavenrepo=/tmp/m2-repository
release=1
version:=$(shell mvn -s settings.xml -Dmaven.repo.local=$(mavenrepo) org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression="project.version" | grep -v '\[')
all: bin_rpm

src_rpm: build_sources
	mkdir -p rpmbuild/BUILD rpmbuild/RPMS rpmbuild/SOURCES rpmbuild/SPECS rpmbuild/SRPMS
	cp target/storm-gridhttps-server-${version}-src.tar.gz rpmbuild/SOURCES/storm-gridhttps-server-${version}.tar.gz
	rpmbuild -bs --define "_topdir ${PWD}/rpmbuild" target/spec/storm-gridhttps-server.spec

build_sources: clean
	mvn -s settings.xml -Dmaven.repo.local=$(mavenrepo) -Drelease=$(release) process-sources

bin_rpm: src_rpm
	rpmbuild --rebuild --define "_topdir ${PWD}/rpmbuild" rpmbuild/SRPMS/storm-gridhttps-server-${version}-$(release).src.rpm

clean:
	rm -rf rpmbuild
	mvn -s settings.xml -Dmaven.repo.local=$(mavenrepo) clean
