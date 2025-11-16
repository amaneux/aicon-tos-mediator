# Use an official Tomcat runtime as a parent images
FROM tomcat:10-jdk21

# Set environment variables
ENV WAR_FILE_NAME=aicon-tos-mediator-1.0-SNAPSHOT.war
ENV TOMCAT_WEBAPPS_DIR=/usr/local/tomcat/webapps
ENV TOMCAT_CONF_DIR=/usr/local/tomcat/conf

# Clean default webapps (optional, good for clean deploy)
RUN rm -rf ${TOMCAT_WEBAPPS_DIR}/*

# Deploy WAR as ROOT
COPY target/${WAR_FILE_NAME} ${TOMCAT_WEBAPPS_DIR}/ROOT.war

# Replace server.xml
COPY tomcat/conf/server.xml ${TOMCAT_CONF_DIR}/server.xml


# Expose the Tomcat port
EXPOSE 8080

# Start Tomcat server
CMD ["catalina.sh", "run"]
