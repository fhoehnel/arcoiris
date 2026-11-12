I recommend building arcoiris with maven using the provided "pom.xml".
The directory "maven-repository" contains some non-public libraries
that are required to build arcoiris. All other libraries that
arcioris depends on are automatically downloaded by maven from
public repositories.

To install the non-public libraries into your local maven repository use the following commands:
mvn install:install-file -Dfile=[your-path-to-project-arcoiris]/maven-repository/com/drew/metadata-extractor/2.3.1/metadata-extractor-2.3.1.jar -DgroupId=com.drew -DartifactId=metadata-extractor -Dversion=2.3.1 -Dpackaging=jar
mvn install:install-file -Dfile=[your-path-to-project-arcoiris]/maven-repository/com/keypoint/png-gif/1.0/png-gif-1.0.jar -DgroupId=com.keypoint -DartifactId=png-gif -Dversion=1.0 -Dpackaging=jar
mvn install:install-file -Dfile=[your-path-to-project-arcoiris]/maven-repository/mediachest/mediautil/1.0.0/mediautil-1.0.0.jar -DgroupId=mediachest -DartifactId=mediautil -Dversion=1.0.0 -Dpackaging=jar

Use the command
  maven install
to build the arcoiris-blog.war web application archive which can be deployed
in any servlet container.

You can import the project into the Eclipse IDE using the provided
project files (".project", ".classpath", ".setting/*").

The ant script "build-hotdeploy.xml" can be used to copy changed
files to the deploy target of the exploaded arcoiris webapp
in the development servlet container.

If you have problems getting the project to run please contact the author at
frank_hoehnel@hotmail.com !

Docker (Tomcat 10 + Java 17)
---------------------------
Build image:
docker build --no-cache -t arcoiris-blog:java17-tomcat10 .

Create Volume for user data:
docker volume create arcoiris-blog-data

Run container:
docker run -p 9753:8080 -d --name arcoiris-blog -v arcoiris-blog-data:/usr/local/tomcat/webapps/arcoiris-blog/WEB-INF/userhome -v c:\Projekte\arcoiris\docker-config:/usr/local/tomcat/webapps/arcoiris-blog/WEB-INF/config -v c:\Projekte\arcoiris\docker-log:/usr/local/tomcat/webapps/arcoiris-blog/WEB-INF/log arcoiris-blog:java17-tomcat10

Export the docker image:
docker save -o arcoiris-blog-java17-tomcat10.tar arcoiris-blog:java17-tomcat10

Podman (Tomcat 10 + Java 17)
----------------------------

podman machine start

podman build -t arcoiris-blog:java17-tomcat10 .

Create Volume for user data:
podman volume create arcoiris-blog-data

podman rm -f arcoiris-blog

podman run -p 9753:8080 -d --name arcoiris-blog -v arcoiris-blog-data:/usr/local/tomcat/webapps/arcoiris-blog/WEB-INF/userhome -v c:\Projekte\arcoiris\docker-config:/usr/local/tomcat/webapps/arcoiris-blog/WEB-INF/config -v c:\Projekte\arcoiris\docker-log:/usr/local/tomcat/webapps/arcoiris-blog/WEB-INF/log arcoiris-blog:java17-tomcat10

podman stop arcoiris-blog

podman restart arcoiris-blog





