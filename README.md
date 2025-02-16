# Bill-Splitting Application

This project is a **bill-splitting service** built with **Java Spring Boot**, designed to handle user registration, expense creation, expense retrieval, and group management. It emphasizes **robustness**, **performance**, and **maintainability** through **comprehensive testing** and **Redis caching**.

---

## Table of Contents

1. [Features](#features)
2. [Tech Stack](#tech-stack)
3. [Architecture Overview](#architecture-overview)
4. [Caching Strategy (Redis)](#caching-strategy-redis)
5. [Performance & Test Coverage](#performance--test-coverage)
6. [Running Locally](#running-locally)
7. [Enabling/Disabling Redis](#enablingdisabling-redis)
8. [Contributing](#contributing)
9. [License](#license)

---

## Features

- **User Registration & Login** (with email verification)
- **Group Creation & Management** (adding/removing members, group ownership)
- **Expense Creation** with multiple split strategies:
    - **EQUAL** - Evenly divided among participants
    - **EXACT** - Specific amounts for each participant
    - **PERCENTAGE** - Percentage split adding up to 100%
- **Expense Retrieval** with a **read-through caching** approach in Redis for faster repeated queries
- **Comprehensive Testing** with over **80% coverage** across services
- **Secure Access Control** (via Spring Security) for group membership and expense ownership checks

---

## Tech Stack

- **Java 17**
- **Spring Boot 3**
    - Spring MVC (REST APIs)
    - Spring Data JPA (PostgreSQL)
    - Spring Data Redis (Lettuce)
    - Spring Security (basic auth or JWT readiness)
- **PostgreSQL** (primary database)
- **Redis** (caching, using a read-through strategy)
- **JUnit 5 & Mockito** (testing)
- **Lombok** (boilerplate reduction)
- **Maven** (build & dependency management)

---

## Architecture Overview

1. **Controller Layer**
    - Receives REST requests and validates DTO inputs (via `@Valid`).
    - Delegates business logic to the service layer.

2. **Service Layer**
    - Core business logic (managing expenses, groups, and user flows).
    - Integrates with **PostgreSQL** via **Spring Data JPA**.
    - Utilizes **Redis caching** to avoid repeated DB queries.

3. **Caching (Redis)**
    - **Read-through approach**: If the data is in Redis, return it immediately. Otherwise, fetch from DB and store in Redis.

4. **Persistence Layer (Repositories)**
    - CRUD operations on **User**, **Group**, **Expense** entities.
    - Powered by **Spring Data JPA**.

---

## Caching Strategy (Redis)

### Read-Through Caching

1. **Check Redis** for expense data by key (`expense:{id}`).
2. If **cache miss**, fetch from the database and **store** it in Redis.
3. If **cache hit**, return data **instantly** from Redis, significantly speeding up repeated requests.
4. **Update/Delete** operations invalidate or delete the cache entry to keep data fresh.

**Example**
- **Initial request** to retrieve expense `#1` → ~**250 ms** (database fetch)
- **Subsequent requests** for the same expense → ~**50 ms** (from Redis, ~80% faster)

---

## Performance & Test Coverage

- **Performance**
    - **Initial DB fetch**: ~250ms
    - **Cached fetch**: ~50ms
    - ~80% improvement in repeated queries

- **Test Coverage**
    - Achieved **80%+** coverage across **service layers** using **JUnit 5** and **Mockito**.
    - Tests cover both **unit** (isolated business logic) and **integration** (database & caching) scenarios.

---

## Running Locally

1. **Clone the Repo**:

    ```bash
    git clone https://github.com/your-username/bill-split-service.git
    cd bill-split-service
    ```

2. **Configure Database & Redis** in `application.properties`:

    ```properties
    spring.datasource.url=jdbc:postgresql://localhost:5432/billSplitDb
    spring.datasource.username=postgres
    spring.datasource.password=postgres_password

    spring.redis.host=localhost
    spring.redis.port=6379
    spring.redis.password=redis_password

    # Toggle Redis caching
    cache.expense.enabled=true
    ```

3. **Build & Run**:

    ```bash
    mvn clean install
    mvn spring-boot:run
    ```

4. **Test via Postman or cURL**:

    ```bash
    curl http://localhost:8080/api/expenses/1
    ```

---

## Enabling/Disabling Redis

- **Dynamic Toggle**: In `application.properties`:

    ```properties
    cache.expense.enabled=true   # or false
    ```

    - **true** → Redis caching is active.
    - **false** → Always fetch from the database.

- **Runtime Refresh** (Optional): If using **Spring Cloud Config** + `@RefreshScope`, the property can be changed without restarting.

---

## Contributing

1. **Fork** the repository
2. **Create a feature branch**
3. **Open a Pull Request** (PR) with your changes
4. Ensure new code has **tests** and passes all **CI** checks

---

## License

This project is licensed under the **MIT License**. See [LICENSE](./LICENSE) for more details.

---

**Happy bill splitting & caching!** For any questions or issues, please open an Issue or contact the maintainers.
