# Tic Tac Toe Game

Tic Tac Toe game built with
- Java 17
- Spring Boot 3
- Spring Data JPA
- H2 Database
- Alpine.js

## Running the Application

1. Build the project:
```bash
./mvnw clean install
```

2. Run the application:
```bash
./mvnw spring-boot:run
```

3. Open your browser and navigate to:
```
http://localhost:8080
```

## Database Console

The H2 database console is available at:
```
http://localhost:8080/h2-console
```

Use these connection details:
- JDBC URL: `jdbc:h2:mem:tictactoe`
- Username: `sa`
- Password: (leave empty) 

## Swagger
```
http://localhost:8080/swagger-ui/index.html
```