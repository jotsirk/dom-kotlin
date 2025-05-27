# DOM - Kotlin Game Solver

## Requirements

- **Java 21** (e.g. Amazon Corretto 21)
- **Kotlin 1.9.25**
- **Spring Boot 3.4.5**
- **Docker** (optional, but supported for containerized deployment)

## Introduction

This Kotlin-based application automatically launches a new game session upon startup and begins solving it based on a strategy that prioritizes:

- **Easiest ads** with the **highest calculated worth**
- **Smart item purchases** if the reward justifies the investment
- Continuous decision-making until the game ends or the player loses

Logs and game events are printed to the console for real-time tracking. When a game finishes, the result can be queried using a REST endpoint provided by the app.

## Booting Up

You can run the application in one of two ways:

### 1. Using Docker

Navigate to the docker-compose file found in **/docker-compose/local** and then run:

```bash
  docker-compose up --build
```

Or if you wish to run it through the IDEA editor's compose file then build the application with Gradle first (build -> build)

### 2.Using Gradle

```bash
  ./gradlew bootRun
```

Make sure the required Java version (21) is available on your system.

## Game Monitoring
Once the game starts automatically, a background task begins processing and logs the actions step-by-step.

You can check the final state of the game using the following endpoint:

```bash
  GET http://localhost:8090/dom/game/game-result?taskId={task-id}
```
Replace {task-id} with the actual ID returned when the game was started (you’ll see it in the logs if logging is enabled).

There is also a possibility to start a new game when the application is running

```bash
  POST http://localhost:8090/dom/game/start-new
```

This will add a new **game solving** task to the tasks query. The query will run 4 games at once all the time if the queue \
has more than 4 games waiting

```bash
  POST http://localhost:8090/dom/game/all-task-ids
```

Will return all the tasks ids in the queue

## Configuration
You can configure whether the game starts automatically by default in application.properties:

```properties
dom.start.manually=false
```

### Different branches

### 1. feature/initial-runner-with-tracker

This branch will add MoveLogs with ads to the Champion object to show what moves were made during the calculate move phase\
if there is a wish to see what options were available

### 2. data-miner

This branch will have a postgres db with a few entities. Was initially made to log game moves with certain rulesets, but \
not recommended to run because hard to understand what is going on there

