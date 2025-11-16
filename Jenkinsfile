pipeline {
    agent any
    environment {
        DOCKER_IMAGE = "avlino/aicon-tos-mediator"
        IMAGE_TAG = "0.1.Snapshot.${BUILD_NUMBER}"  // Versioned tag using BUILD_NUMBER
        WAR_FILE_NAME = "aicon-tos-mediator-1.0-SNAPSHOT.war"
    }// Poll every 2 minutes
    stages {
        stage('Clean Workspace') {
            steps {
                deleteDir()
            }
        }
        stage('Checkout') {
            steps {
                checkout scmGit(branches: [[name: 'test-phase']], extensions: [], userRemoteConfigs: [[credentialsId: 'avlino_builder_personal_access_token', url: 'https://github.com/amaneux/Aicon-Tos-Mediator.git']])
            }
        }

        stage('Build WAR') {
            steps {
                script {
                    docker.image('maven:3.9.4-eclipse-temurin-21').inside {
                        // Run Maven to generate the WAR file and skip tests
                        sh 'mvn clean package -DskipTests'
                        sh 'ls -lh'
                    }
                }
            }
        }
        stage('Build Docker Image') {
            when {
                expression { currentBuild.result == null || currentBuild.result == 'SUCCESS' }
            }
            steps {
                // Build Docker image using the Dockerfile in the repo with versioned tag
                sh """
                COMMIT_ID=\$(git rev-parse --short HEAD)
                FULL_COMMIT_ID=\$(git rev-parse HEAD)
                COMMIT_DATE=\$(git log -1 --format=%ci)
                COMMIT_AUTHOR=\$(git log -1 --format="%an")
                COMMIT_TIME=\$(git log -1 --format=%cr)
                COMMIT_TIMESTAMP=\$(git log -1 --format=%ct)
                BUILD_NUMBER="${BUILD_NUMBER}"

                echo "Generating version info file..."
                mkdir -p src/main/resources

                cat <<EOV > src/main/resources/version.properties
                version.commit=\${COMMIT_ID}
                version.fullCommit=\${FULL_COMMIT_ID}
                version.date=\${COMMIT_DATE}
                version.build=\${BUILD_NUMBER}
                version.author=\${COMMIT_AUTHOR}
                version.timeAgo=\${COMMIT_TIME}
                version.commitTimestamp=\${COMMIT_TIMESTAMP}
                version.host=\${REMOTE_HOST}
EOV

                docker build --no-cache --label build.number=${BUILD_NUMBER} -t ${DOCKER_IMAGE}:${IMAGE_TAG} ."""
            }
        }
        stage('Push Docker Image') {
            when {
                expression { currentBuild.result == null || currentBuild.result == 'SUCCESS' }
            }
            steps {
                // Push both the versioned and latest Docker images to Docker Hub
                sh "docker push ${DOCKER_IMAGE}:${IMAGE_TAG}"
                sh "docker tag ${DOCKER_IMAGE}:${IMAGE_TAG} ${DOCKER_IMAGE}:latest"
                sh "docker push ${DOCKER_IMAGE}:latest"
            }
        }
    }
    post {
        failure {
            echo 'Build failed. Docker image will not be pushed.'
        }
        success {
            echo 'Build and push completed successfully.'
        }
    }
}
