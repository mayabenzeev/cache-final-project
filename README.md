# Redis Caching Workshop: Telco Edition

You know the caching theory from the lecture. In this hour you'll **use** it: fix three real problems in a small
telecom backend, deciding each time which caching idea solves the problem, then building it with Redis.

## Before the session (at home, 10 min)

Downloads on shared Wi-Fi can eat 15 minutes of the workshop, so do this at home.

**Install:** JDK **25**, Docker Desktop, Git, an IDE (IntelliJ recommended), and the **RedisInsight** app from
the lecture.

<details>
<summary>Missing something?</summary>

- **JDK 25:** [Eclipse Temurin 25](https://adoptium.net/temurin/releases/?version=25), or `brew install --cask temurin@25` (macOS)
- **RedisInsight:** `brew install --cask redis-insight` (macOS), or <https://redis.io/insight/>
- **Maven** isn't needed: the repo has the Maven wrapper (`./mvnw`, or `mvnw.cmd` on Windows).
</details>

**Then, with Docker running:**

```bash
git clone <this-repo-url> telco-cache-workshop
cd telco-cache-workshop
docker compose pull
./mvnw test -Dtest=SetupCheckTest
```

You should see `Setup OK`.

**Last step:** run `./mvnw spring-boot:run`, which also starts Redis. When you see `Started TelcoCacheWorkshopApplication`,
add a database in RedisInsight: host `localhost`, port `6379`, alias `telco-workshop`. Then stop the app with
Ctrl+C. Redis keeps running.

Something failed? See [Troubleshooting](#troubleshooting).

## The workshop

| Time | Page |
|---|---|
| 0–3 min | Start the app (below) |
| 3–18 min | [Exercise 1: The slow plan catalogue](exercises/ex1.md) |
| 18–33 min | [Exercise 2: Prices change, the cache lies](exercises/ex2.md) |
| 33–48 min | [Exercise 3: The account that changes one field at a time](exercises/ex3.md) |
| 48–60 min | [Wrap-up: apply it to your final project](exercises/wrap-up.md) |
| Later | Bonus exercises: on the `bonus` branch (see the [wrap-up](exercises/wrap-up.md)) |

Keep the **[cheat sheet](CHEATSHEET.md)** open in a tab: every Redis command with its Spring and Nest equivalent.

**How it works:**

- The system is a mobile provider's backend with a deliberately slow database: **every read takes about 1 second**.
- Each exercise has its own folder, e.g. `src/main/java/com/tdp/telco/ex1/`. Your TODOs are in its `service/` folder.
- **Every step has a test.** Run it after the step; if it fails, the message tells you what's missing.
- **Exercises are independent.** Stuck? Take the solution (each page shows how) and move on.

### Start the app

```bash
./mvnw spring-boot:run
```

Click the first request in [`requests.http`](requests.http). The response is plain JSON. The **timing is printed
in the app console**:

```
GET /ex1/plans/basic -> 200 in 1004 ms
```

**Look inside Redis** with RedisInsight (the `telco-workshop` database), or in a terminal with
`docker compose exec redis redis-cli`.

Changed the code? Restart the app. The tests don't need the app running.

<details>
<summary>The repo layout</summary>

```
src/main/java/com/tdp/telco/
  common/         shared by every exercise (entities, the slow repositories, config)
  ex1/ ex2/ ex3/  one folder per exercise: controller/, dto/, service/  <- your TODOs are in service/
src/test/java/com/tdp/telco/
  ex1/ ex2/ ex3/  one test class per step, e.g. Ex1Step2CacheAsideTest
requests.http     every request, one click each
```

Caching lives in the **service** layer: the controller doesn't know there's a cache, and neither does the
repository.
</details>

## Troubleshooting

| Symptom | Fix |
|---|---|
| `Could not find a valid Docker environment` | Start Docker Desktop and wait until it says "running". |
| `release version 25 not supported` | Maven is using an older JDK. `java -version` must say 25; set `JAVA_HOME` to your JDK 25. |
| `Port 6379 is already allocated` | Another Redis is running. Stop it (`brew services stop redis`, or stop the container in `docker ps`). |
| App or RedisInsight can't reach Redis | Start Redis on its own with `docker compose up -d redis`. In RedisInsight: host `localhost`, port `6379`, no password. |
| Changed the code, nothing changed | Restart the app. Tests always use the latest code. |
| Old cached data confuses you | In `redis-cli`: `FLUSHALL` (deletes everything in your local Redis). |
