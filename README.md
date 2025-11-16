# Aicon-Tos-Mediator

## 🛠️ How to Build

Builds are managed through Jenkins.

👉 Go to: [Aicon-Tos-Mediator Jenkins Job](http://avlino-jenkins.avlino.az:8081/job/Aicon-Tos-Mediator/)

- The job automatically checks out the latest code from the `develop` branch
- Build triggers on new commits (via polling every 2 minutes)
- Outputs a versioned Docker image and pushes it to Docker Hub

---

## 🚀 How to Deploy

This application supports client-specific configuration using Docker Compose.  
Each client has its own configuration folder under:

src/main/resources/client_configurations/**_CLIENT_**


### 🔧 Deployment

To deploy the application for a specific client (e.g., `DCT`), run the following command:

```bash
git pull
CLIENT=DCT docker-compose -f docker-compose-deploy.yml up -d
