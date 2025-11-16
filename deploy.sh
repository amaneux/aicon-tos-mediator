#!/bin/bash
#############################################################################
# WARNING: This is a DRAFT deployment script based on reverse engineering
# the application's deployment process. It may require adjustments to work
# correctly in your environment. Consider this a starting point if you need
# to manually deploy the application while we develop an automated process.
#############################################################################



set -e




# Set variables
DOCKER_IMAGE="avlino/aicon-tos-mediator"
IMAGE_TAG="latest"
DEPLOYMENT_DIR="/opt/tomcat-deployment"
JENKINS_ARTIFACT_URL="http://avlino-jenkins.avlino.az:8081/job/Aicon-Tos-Mediator/lastSuccessfulBuild/artifact/deployment.tar.gz"
USE_JENKINS_ARTIFACT=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --tag)
      IMAGE_TAG="$2"
      shift 2
      ;;
    --jenkins-artifact)
      USE_JENKINS_ARTIFACT=true
      shift
      ;;
    --jenkins-url)
      JENKINS_ARTIFACT_URL="$2"
      shift 2
      ;;
    --help)
      echo "Usage: ./deploy.sh [OPTIONS]"
      echo "Deploy the Aicon-Tos-Mediator application"
      echo ""
      echo "Options:"
      echo "  --tag TAG             Specify Docker image tag (default: latest)"
      echo "  --jenkins-artifact    Download deployment artifact from Jenkins"
      echo "  --jenkins-url URL     Specify Jenkins artifact URL"
      echo "  --help                Show this help message"
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      exit 1
      ;;
  esac
done

echo "=== Aicon-Tos-Mediator Deployment Script ==="
echo "Docker Image: $DOCKER_IMAGE:$IMAGE_TAG"
echo "Deployment Directory: $DEPLOYMENT_DIR"
echo ""

# Create deployment directory if it doesn't exist
mkdir -p "$DEPLOYMENT_DIR/webapps" "$DEPLOYMENT_DIR/config"

# If using Jenkins artifact, download and extract it
if [ "$USE_JENKINS_ARTIFACT" = true ]; then
  echo "=== Downloading deployment artifact from Jenkins ==="
  cd "$DEPLOYMENT_DIR"
  curl -L -o deployment.tar.gz "$JENKINS_ARTIFACT_URL"
  tar -xzf deployment.tar.gz
  rm deployment.tar.gz
else
  # Otherwise, ensure all necessary files are in place
  echo "=== Verifying deployment files ==="

  # Check if ROOT.war exists or prompt for location
  if [ ! -f "$DEPLOYMENT_DIR/webapps/ROOT.war" ]; then
    read -p "Enter path to WAR file: " WAR_PATH
    if [ -f "$WAR_PATH" ]; then
      cp "$WAR_PATH" "$DEPLOYMENT_DIR/webapps/ROOT.war"
      echo "WAR file copied to $DEPLOYMENT_DIR/webapps/ROOT.war"
    else
      echo "ERROR: WAR file not found at $WAR_PATH"
      exit 1
    fi
  fi

  # Check if server.xml exists
  if [ ! -f "$DEPLOYMENT_DIR/config/server.xml" ]; then
    echo "WARNING: server.xml not found in $DEPLOYMENT_DIR/config/"
    read -p "Enter path to server.xml: " SERVER_XML_PATH
    if [ -f "$SERVER_XML_PATH" ]; then
      cp "$SERVER_XML_PATH" "$DEPLOYMENT_DIR/config/server.xml"
      echo "server.xml copied to $DEPLOYMENT_DIR/config/server.xml"
    else
      echo "ERROR: server.xml not found at $SERVER_XML_PATH"
      exit 1
    fi
  fi

  # Check if aicon-connections.xml exists
  if [ ! -f "$DEPLOYMENT_DIR/config/aicon-connections.xml" ]; then
    echo "WARNING: aicon-connections.xml not found in $DEPLOYMENT_DIR/config/"
    read -p "Enter path to aicon-connections.xml: " CONNECTIONS_XML_PATH
    if [ -f "$CONNECTIONS_XML_PATH" ]; then
      cp "$CONNECTIONS_XML_PATH" "$DEPLOYMENT_DIR/config/aicon-connections.xml"
      echo "aicon-connections.xml copied to $DEPLOYMENT_DIR/config/aicon-connections.xml"
    else
      echo "ERROR: aicon-connections.xml not found at $CONNECTIONS_XML_PATH"
      exit 1
    fi
  fi

  # Create docker-compose.yml if it doesn't exist
  if [ ! -f "$DEPLOYMENT_DIR/docker-compose.yml" ]; then
    cat > "$DEPLOYMENT_DIR/docker-compose.yml" << EOF
version: '3.8'
services:
  tomcat:
    image: ${DOCKER_IMAGE}:${IMAGE_TAG}
    container_name: tomcat_server
    environment:
      - CATALINA_OPTS=-Djava.awt.headless=true -Xms512m -Xmx1024m
      - KAFKA_BOOTSTRAP_SERVERS=\${KAFKA_BOOTSTRAP_SERVERS}
    ports:
      - "8081:8080"
    volumes:
      - ./webapps:/usr/local/tomcat/webapps
      - ./config/server.xml:/usr/local/tomcat/conf/server.xml
      - ./config/aicon-connections.xml:/usr/local/tomcat/../conf/aicon-connections.xml
    networks:
      - internal_network
networks:
  internal_network:
    driver: bridge
EOF
    echo "Created docker-compose.yml in $DEPLOYMENT_DIR"
  fi
fi

# Pull the latest Docker image
echo "=== Pulling Docker image: $DOCKER_IMAGE:$IMAGE_TAG ==="
docker pull "$DOCKER_IMAGE:$IMAGE_TAG"

# Stop and remove existing container if it exists
echo "=== Stopping existing container ==="
docker-compose -f "$DEPLOYMENT_DIR/docker-compose.yml" down || true

# Start the new container
echo "=== Starting container with new image ==="
cd "$DEPLOYMENT_DIR"
KAFKA_BOOTSTRAP_SERVERS=${KAFKA_BOOTSTRAP_SERVERS:-localhost:9092} docker-compose up -d

# Check if the container is running
echo "=== Checking container status ==="
sleep 5
if docker ps | grep tomcat_server; then
  echo "=== Deployment successful ==="
  echo "Container is running. Logs can be viewed with: docker logs tomcat_server"
else
  echo "=== ERROR: Container failed to start ==="
  echo "Logs:"
  docker logs tomcat_server
  exit 1
fi