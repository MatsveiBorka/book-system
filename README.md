# Resource API & Logging Service

This project consists of two Spring Boot microservices:
- **[Resource API](http://localhost:8080/swagger-ui/index.html#/)**
  (runs on port `8080`)
- **[Logging Service](http://localhost:8081/swagger-ui/index.html#/)**
  (runs on port `8081`)

Both services are containerized with Docker and orchestrated using Docker Compose.

---

## Running the Application Locally

To start the application using Docker Compose, run:

```bash
docker-compose up -d
