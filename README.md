The StoRM Gridhttps Server
===============================

StoRM GridHTTPs Server exposes a standard WebDAV interface on top of StoRM BackEnd Service to 
provide WebDAV access to a Grid Storage Element. This component behaves also as a pure HTTP(s) transfer 
server when used in conjunction with StoRM SRM interface.

## Supported platforms

* Scientific Linux 5 x86_64
* Scientific Linux 6 x86_64

## Building
Required packages:

* git
* java-1.6.0-openjdk
* java-1.6.0-openjdk-devel
* maven

> Maven is a Java tool, so you must have Java installed in order to proceed. More precisely, you need a Java Development Kit (JDK), the Java Runtime Environment (JRE) is not sufficient.

Download source files:
<pre>
git clone https://github.com/italiangrid/storm-gridhttps-server.git
cd storm-gridhttps-server
</pre>

Build commands:
<pre>
wget --no-check-certificate https://raw.github.com/italiangrid/build-settings/master/maven/cnaf-mirror-settings.xml -O mirror-settings.xml
mvn -s mirror-settings.xml clean
mvn -s mirror-settings.xml -U package
</pre>

>It could be necessary to set JAVA_HOME environment variable, for example:
<pre>
export JAVA_HOME="/usr/lib/jvm/java"
</pre>

# Contact info

If you have problems, questions, ideas or suggestions, please contact us at
the following URLs

* GGUS (official support channel): http://www.ggus.eu
