# Reservation Platform

Reservation Platform is a microservice-based system for booking places such as courts, rooms and desks.
User can book an available resource.

## Tech stack

Java 21, Spring Boot 4, PostgreSQL, Spring Data JPA/Hibernate, Maven, Lombok, Flyway, Docker Compose, Testcontainers.

## Status

Resource service is completed. Booking service is planned.

## Getting started

Requirements: Java 21, Docker, Maven.

- Download or clone repository `git clone https://github.com/kamiliano1/reservation.git`
- Copy `.env.example` as `.env` set `POSTGRES_PASSWORD` and `POSTGRES_USER` in root directory, `copy .env.example .env`
  in Windows, `cp .env.example .env` in Linux/Mac,
- Start docker `docker compose up -d` during first start `docker/postgres/init.sql` creates `booking_db`
- Set up environmental variables for Windows `$env:POSTGRES_USER="..." $env:POSTGRES_PASSWORD="..."`, Linux/Mac
  `export POSTGRES_USER=... export POSTGRES_PASSWORD=...` the same as in root directory
- In resource folder run `mvn spring-boot:run`
- API works on http://localhost:8081

## API

Base URL: http://localhost:8081

### Endpoints

#### GET /api/resources

Get all resources

##### Example Request

```bash
curl -X GET http://localhost:8081/api/resources
```

##### Example Response

```json
[
  {
    "id": 62,
    "name": "Tennis Court 1",
    "type": "TENNIS_COURT",
    "hourlyPrice": 100,
    "openTime": "11:00:00",
    "closeTime": "15:00:00"
  }
]
```

##### Status Codes

| Status | Meaning         |
|--------|-----------------|
| 200    | Resources found |

#### POST /api/resources

Create a new resource.

##### Request Body

| Parameter   | Type      | Required | Description                                            |
|-------------|-----------|----------|--------------------------------------------------------|
| name        | string    | Yes      | Full display name                                      |
| type        | enum      | Yes      | `TENNIS_COURT`, `MEETING_ROOM`, `DESK`, `FITNESS_ROOM` |
| hourlyPrice | Decimal   | Yes      | Price for one hour                                     |
| openTime    | LocalTime | Yes      | Opening time for resource                              |
| closeTime   | LocalTime | Yes      | Closing time for resource                              |

##### Example Request

```bash
curl -X POST http://localhost:8081/api/resources \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Tennis court 1",
    "type": "TENNIS_COURT",
    "hourlyPrice": 100,
    "openTime":"11:00",
    "closeTime":"15:00"
  }'
```

##### Example Response

```http
Location: http://localhost:8081/api/resources/62
```

```json
{
  "id": 62,
  "name": "Tennis Court 1",
  "type": "TENNIS_COURT",
  "hourlyPrice": 100,
  "openTime": "11:00:00",
  "closeTime": "15:00:00"
}
```

##### Status Codes

| Status | Meaning             |
|--------|---------------------|
| 201    | Resource created    |
| 400    | Wrong data provided |

```json
{
  "detail": "Opening time must be before close time",
  "instance": "/api/resources",
  "status": 400,
  "title": "Bad Request"
}
{
  "detail": "Validation failed",
  "instance": "/api/resources",
  "status": 400,
  "title": "Bad Request",
  "errors": {
    "name": "must not be blank"
  }
}
```

#### GET /api/resources/{id}

Get resource

##### Example Request

```bash
curl -X GET http://localhost:8081/api/resources/62
```

##### Example Response

```json
{
  "id": 62,
  "name": "Tennis Court 1",
  "type": "TENNIS_COURT",
  "hourlyPrice": 100,
  "openTime": "11:00:00",
  "closeTime": "15:00:00"
}
```

##### Status Codes

| Status | Meaning            |
|--------|--------------------|
| 200    | Resource found     |
| 404    | Resource not found |

#### PUT /api/resources/{id}

Update an existing resource.

##### Request Body

| Parameter   | Type      | Required | Description                                            |
|-------------|-----------|----------|--------------------------------------------------------|
| name        | string    | Yes      | Full display name                                      |
| type        | enum      | Yes      | `TENNIS_COURT`, `MEETING_ROOM`, `DESK`, `FITNESS_ROOM` |
| hourlyPrice | Decimal   | Yes      | Price for one hour                                     |
| openTime    | LocalTime | Yes      | Opening time for resource                              |
| closeTime   | LocalTime | Yes      | Closing time for resource                              |

##### Example Request

```bash
curl -X PUT http://localhost:8081/api/resources/62 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Tennis court 1",
    "type": "TENNIS_COURT",
    "hourlyPrice": 100,
    "openTime":"11:00",
    "closeTime":"15:00"
  }'
```

##### Example Response

```json
{
  "id": 62,
  "name": "Tennis Court 1",
  "type": "TENNIS_COURT",
  "hourlyPrice": 100,
  "openTime": "11:00:00",
  "closeTime": "15:00:00"
}
```

##### Status Codes

| Status | Meaning             |
|--------|---------------------|
| 200    | Resource updated    |
| 400    | Wrong data provided |
| 404    | Resource not found  |

#### DELETE /api/resources/{id}

Soft delete of existing resource

##### Example Request

```bash
curl -X DELETE http://localhost:8081/api/resources/62
```

##### Status Codes

| Status | Meaning            |
|--------|--------------------|
| 204    | Resource deleted   |
| 404    | Resource not found |

## Design decisions

- **Database-level protection.** Data is protected in the database as well, not only in the API. Required columns are
  `NOT NULL`, and `hourly_price` must be greater than or equal to 0 (a price of 0 is allowed, for example for promotions
  or events). The constraint `CONSTRAINT chk_hours CHECK (close_time > open_time)` rejects invalid opening hours. The
  test `throwWhenResourceSaveUsingRepositoryWithWrongHours` confirms that the database itself rejects such data, even
  when the API is bypassed.
- **Soft delete.** A deleted resource is not removed from the database. Its `active` column is changed from `true` to
  `false`. Data related to the resource, such as historical reservations, is kept.
- **The client cannot set the id.** `ResourceRequest` has no `id` field, so the client cannot choose it. The database
  generates it. This prevents overwriting an existing record. The `createdAt` and `updatedAt` timestamps are also
  generated automatically.
- **Tests use a real PostgreSQL database.** Testcontainers starts a PostgreSQL container, and the database is cleaned
  before each test. This gives more confidence that the code behaves the same way on a real database.
- **`spring.jpa.hibernate.ddl-auto=validate`.** At every startup Hibernate checks that table and column names and types
  match the entities. If they do not, the application does not start, so the problem is visible immediately and not
  later, when data is being saved. Hibernate only validates the schema, while Flyway changes it through migrations.

## Tests

To run the tests, execute `mvn test` in the `resource-service` directory. Docker must be running, because Testcontainers
starts a PostgreSQL container for the tests. No environment variables or `.env` file are needed to run the tests

Test groups:

- **Create and read:** a resource is created and can be found by its id.
- **Hours validation:** invalid opening hours return an error, both when creating and when updating a resource.
- **Soft delete:** a deleted resource is not returned by `findById` and does not appear in the list of resources.
- **Update:** the data of an existing resource is updated, and updating a missing or deleted resource returns an error.
- **Database constraints:** saving invalid data directly through the repository (bypassing the service) is rejected by
  the database.
