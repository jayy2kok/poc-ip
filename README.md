# Instant Payments API (Proof of Concept)

This project is a Proof of Concept (PoC) for an instant payment processing system built with Spring Boot, PostgreSQL, ActiveMQ Artemis, and Debezium (CDC).

## Architecture & Testing

- **Architecture Details:** See [`docs/architecture.md`](docs/architecture.md) for a comprehensive overview of the system architecture, component interactions, and the data models.
- **Testing Scenarios:** See [`docs/e2e_testing_matrix.md`](docs/e2e_testing_matrix.md) for the complete list of supported End-to-End (E2E) testing scenarios across domain services.

## Local Environment Setup

### Prerequisites
- Docker & Docker Compose
- Java 17+
- Maven 3.8+

### How to Run Locally

1. **Build the project and start the infrastructure**
   You can easily build the Maven modules and start all necessary Docker containers (PostgreSQL, ActiveMQ Artemis, Kafka, Debezium, Zookeeper) using the provided batch script:
   ```bash
   ./bd.bat
   ```
   *(Alternatively: Run `mvn clean install -DskipTests` followed by `docker-compose up -d`)*

2. **Run the Microservices**
   Start each of the three microservices in your IDE or via terminal:
   - **Payment Processing System (PPS):** Runs on port `8081`
     ```bash
     cd pps && mvn spring-boot:run
     ```
   - **Broker System (BS):** Runs on port `8082`
     ```bash
     cd bs && mvn spring-boot:run
     ```
   - **Audit Service (AS):** Runs on port `8084`
     ```bash
     cd as && mvn spring-boot:run
     ```

## Swagger UI URLs

Once the services are running, you can access their respective Swagger UI dashboards to interact with the REST APIs:

- **Payment Processing System (PPS):** [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
- **Broker System (BS):** [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html)
- **Audit Service (AS):** [http://localhost:8084/swagger-ui.html](http://localhost:8084/swagger-ui.html)

## Real-time Notifications Demo (WebSocket)

The Payment Processing System (PPS) pushes real-time payment status updates via WebSockets. To see this in action:

1. Open the [websocket-demo.html](websocket-demo.html) file directly in your web browser. (You don't need a local web server to host it, just double-click the file in your file explorer).
2. The page connects to `ws://localhost:8081/ws/payments` by default (you can configure the URL at the top of the page). Wait until the indicator turns green ("Connected").
3. Use the **PPS Swagger UI** (`http://localhost:8081/swagger-ui.html`) to submit a new payment via `POST /api/v1/payments`.
4. Watch the `websocket-demo.html` page to see the real-time status updates injected directly into the table!
