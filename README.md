# Redis Caching Workshop: Telco Edition

You already know the theory: what a cache is, cache-aside, TTL, invalidation, Redis data structures.
In the next hour you'll **use** it. You'll fix three real problems in a small telecom backend. Each time
you'll decide *which* caching idea solves the problem, then implement it with real Redis operations.

By the end you should be able to explain, for anything you cache in your final project:
**why** you cache it, **what the key looks like**, **what TTL** it gets, **how it stays fresh**, and
**which Redis structure** holds it.

> The code is Java + Spring Boot, but the reasoning is the same in any language. Every exercise ends with
> a short **"Same idea in your Nest project"** note.

---

## Contents

- [Before the session](#before-the-session-10-min-at-home) (do this at home!)
- [How this workshop works](#how-this-workshop-works)
- [Exercise 1: The slow plan catalogue](#exercise-1--the-slow-plan-catalogue-15-min) (cache-aside, keys, TTL)
- [Exercise 2: Prices change, the cache lies](#exercise-2--prices-change-the-cache-lies-15-min) (consistency, write strategies)
- [Exercise 3: The account that changes one field at a time](#exercise-3--the-account-that-changes-one-field-at-a-time-15-min) (strings vs hashes)
- [Wrap-up: apply it to your final project](#wrap-up-apply-it-to-your-final-project-12-min)
- [Bonus A: Most popular plans leaderboard](#bonus-a--most-popular-plans-leaderboard-sorted-sets) (sorted sets)
- [Bonus B: Memory fills up](#bonus-b--memory-fills-up-eviction) (eviction)
- [Optional extras](#optional-extras-reading-only)
- [Redis ops cheat sheet](#redis-ops-cheat-sheet)
- [Troubleshooting](#troubleshooting)

---

## Before the session (10 min, at home)

The workshop is only 60 minutes. Downloading Docker images and Maven dependencies on conference Wi-Fi can
eat 15 of them, so do it now.

**You need:**

| Tool | Version | Check with |
|---|---|---|
| JDK | **25** | `java -version` |
| Docker Desktop (or Docker Engine + Compose v2) | any recent | `docker compose version` |
| Git | any | `git --version` |
| An IDE | IntelliJ IDEA recommended (it runs `requests.http` out of the box) | 
| RedisInsight desktop app (the one from the lecture) | any recent | it opens |

No JDK 25? Install [Eclipse Temurin 25](https://adoptium.net/temurin/releases/?version=25), or use
`brew install --cask temurin@25` on macOS or `sdk install java 25-tem` with SDKMAN. Maven is **not** needed:
the repo has the Maven wrapper (`./mvnw`).

No RedisInsight? `brew install --cask redis-insight` on macOS, or download it from <https://redis.io/insight/>.

**Then run these, in the repo folder, with Docker running:**

```bash
git clone <this-repo-url> telco-cache-workshop
```

```bash
cd telco-cache-workshop
```

```bash
docker compose pull
```

```bash
./mvnw test -Dtest=SetupCheckTest
```

(On Windows use `mvnw.cmd` instead of `./mvnw` everywhere.)

The last command downloads every dependency, starts a throwaway Redis in Docker and prints:

```
  Setup OK: Java 25, Spring Boot, Docker and Redis all work.
```

**Last step: start the app once and connect RedisInsight.**

```bash
./mvnw spring-boot:run
```

This also starts the workshop Redis for you. When you see `Started TelcoCacheWorkshopApplication`, open
RedisInsight, click **Add Redis database** and enter host `localhost`, port `6379`, database alias
`telco-workshop`. RedisInsight saves it, so it's ready for the session. Then stop the app with Ctrl+C.
Redis keeps running in the background.

If you saw `Setup OK`, the app started, and the `telco-workshop` database opens in RedisInsight, you're ready.
If not, check [Troubleshooting](#troubleshooting) or bring the error to the session.

---

## How this workshop works

### The scenario

You work on the backend of a mobile provider. The system has:

- a **plan catalogue**: mobile plans with a monthly price (`basic`, `unlimited`, `unlimited-premium`, `family-4`, `prepaid-15`)
- **customer accounts**: name, address, current plan, data used this month (`acc-1001`, `acc-1002`, `acc-1003`)
- **plan sign-ups**, used by the Bonus A leaderboard

The "database" is deliberately slow: **every read takes about 1 second**, and every write about 300 ms.
You'll *feel* what caching saves.

### Time plan

| Time | What |
|---|---|
| 0–3 min | Start the app |
| 3–18 min | Exercise 1: The slow plan catalogue |
| 18–33 min | Exercise 2: Prices change, the cache lies |
| 33–48 min | Exercise 3: The account that changes one field at a time |
| 48–60 min | Wrap-up: apply it to your final project |
| Homework / fast finishers | Bonus A (leaderboard), Bonus B (eviction), optional extras |

### The repo

One folder per exercise. Inside each one, the usual Spring MVC layers:

```
src/main/java/com/tdp/telco/
  common/                  shared by every exercise (provided)
    entity/                  Plan, Account, Signup
    repository/              the slow "database", the source of truth (don't edit)
    exception/               NotFoundException -> HTTP 404
    config/                  CacheConfig (one TODO, in Exercise 1), request timing log
  ex1/                     Exercise 1: the slow plan catalogue
    controller/              PlanCatalogController   /ex1/...
    service/                 ManualPlanService, AnnotatedPlanService     <- your TODOs
  ex2/                     Exercise 2: prices change
    controller/  dto/
    service/                 PlanPriceService, AnnotatedPlanPriceService <- your TODOs
  ex3/                     Exercise 3: strings vs hashes
    controller/  dto/
    service/                 HashAccountCache <- your TODOs  (StringAccountCache to compare)
  bonusa/                  Bonus A: leaderboard
    controller/  dto/
    service/                 LeaderboardService      <- your TODOs
  bonusb/                  Bonus B: eviction
    service/                 SessionStore            <- your TODO
src/test/java/com/tdp/telco/
  ex1/ ex2/ ex3/ bonusa/ bonusb/   one test class per step, e.g. Ex1Step2CacheAsideTest
requests.http                      every request, one click each
```

Your TODOs are always in the exercise's `service/` folder. That's where caching belongs: the controller doesn't
know there's a cache, and neither does the repository. The service decides "Redis first, database on a miss".

- **Every exercise stands on its own.** Didn't finish Exercise 1? Go straight to Exercise 2. Nothing breaks.
- Your work is marked with `// TODO (Exercise N, step X)`. Search for `TODO (Exercise 1` to find them all.
- **Each step has a test that fails until you do the step correctly.** The failure message is a hint.
- The `solutions` branch has every exercise solved, with comments explaining the decisions.
  See [Stuck?](#stuck) in each exercise.

### Start the app (0–3 min)

```bash
./mvnw spring-boot:run
```

Spring Boot starts the Redis from `docker-compose.yml` for you. When you see `Started TelcoCacheWorkshopApplication`,
try it:

```bash
curl localhost:8080/ex1/plans/basic
```

Or click the first request in `requests.http`.

The response is just JSON. The **timing is printed in the app console** (the terminal running `./mvnw spring-boot:run`),
one line per request:

```
GET /ex1/plans/basic -> 200 in 1004 ms
```

**Look inside Redis**, either way:

- **redis-cli**: open a second terminal and run `docker compose exec redis redis-cli`. You get a `127.0.0.1:6379>` prompt
  where you can type the commands from this README.
- **RedisInsight** (visual): open the desktop app and click the **telco-workshop** database (`localhost:6379`)
  that you added before the session. Its built-in CLI (bottom of the window) runs the same commands as `redis-cli`.

> You changed code? Stop the app (Ctrl+C) and start it again. The tests don't need the app running:
> they start their own Redis.

---

## Exercise 1: The slow plan catalogue (15 min)

> **Learning goal:** by the end, you can explain the cache-aside flow (hit, miss, populate), design a clear
> namespaced key, choose and justify a TTL, and explain what `@Cacheable` does under the hood.

### 1. The problem

The plan page calls `GET /ex1/plans/{id}`, and it takes **about 1 second, every single time**. Traffic is
almost all reads: thousands of people look at plans, and prices change a few times a year. The database is
doing the same expensive work over and over to return the same answer.

Measure it: click the first request in `requests.http` three times and watch the app console:

```
GET /ex1/plans/basic -> 200 in 1004 ms
GET /ex1/plans/basic -> 200 in 1002 ms
GET /ex1/plans/basic -> 200 in 1003 ms
```

### 2. Think first

**Your goal:** add a caching layer between the service and the slow database. Keep a copy of each plan in Redis,
so repeated reads are served from Redis instead of the database.

Caching forces you to make two design decisions. Don't write code yet: think about them for a couple of
minutes and write your answers down. You'll use them in the steps below.

**Decision 1: the cache key** (you'll code it in Step 1)

Redis stores every value under a key, like `user:123` in the lecture. You need a key for each plan.

- Open [`common/entity/Plan.java`](src/main/java/com/tdp/telco/common/entity/Plan.java), the thing you're caching.
  Which field identifies a plan?
- The real plan ids are in [`common/repository/PlanRepository.java`](src/main/java/com/tdp/telco/common/repository/PlanRepository.java):
  `basic`, `unlimited`, `family-4`, ...
- Accounts ([`common/entity/Account.java`](src/main/java/com/tdp/telco/common/entity/Account.java)) have ids too.
  If a key were just the id, a plan and an account with the same id would overwrite each other.
- Redis has no tables: all keys live side by side in one flat list. At work, one Redis is often shared by
  several services. Your laptop's Redis only holds your data, but design the key as if it were shared.

→ **Under what key would you store plan `basic`, so it can never clash with an account, or with another service's key?**

**Decision 2: the TTL** (you'll set it in Step 2)

A TTL (time to live) makes Redis delete the cached copy after a while, so a stale copy can't live forever.
Plan prices change only a few times a year.

→ **How long should a cached plan live? What do you gain with a longer TTL, and what do you risk?**

### 3. Concept recap

**Cache-aside** (lazy loading) is the pattern you'll reach for most often. The *application* talks to both
the cache and the database:

```
read(id):
  value = cache.get(key)          -> HIT: return it
  if missing:                     -> MISS:
     value = db.load(id)             go to the source of truth
     cache.set(key, value, ttl)      populate the cache for the next reader
  return value
```

The cache is **a copy, not the source of truth**. A good key is **namespaced and readable**
(`<app>:<entity>:<id>`, like `user:123` in the lecture), so different kinds of data never collide and you can
find things in `redis-cli`. A **TTL** bounds how long a copy can be stale. Long TTL: higher hit rate, staler
data. Short TTL: fresher data, more database traffic.

### 4. Tasks

**Step 1: the key.** Turn Decision 1 into code. In [`ex1/service/ManualPlanService.java`](src/main/java/com/tdp/telco/ex1/service/ManualPlanService.java),
make `planKey(id)` return your key, and put your reasoning in the comment above it.

```bash
./mvnw test -Dtest=Ex1Step1KeyDesignTest
```

**Before Step 2: meet `StringRedisTemplate`.** In the lecture you talked to Redis with `redis-cli`. In Java you
use Spring's `StringRedisTemplate`: one object that sends Redis commands for you. It's already injected into
`ManualPlanService` as the field `redis`, so you don't need to create or configure anything.

Its methods mirror the commands you know, grouped by data type:

| redis-cli | `StringRedisTemplate` |
|---|---|
| `GET telco:plan:basic` | `redis.opsForValue().get("telco:plan:basic")` → the value, or `null` if the key doesn't exist |
| `SET telco:plan:basic "..." EX 600` | `redis.opsForValue().set("telco:plan:basic", "...", Duration.ofMinutes(10))` |
| `DEL telco:plan:basic` | `redis.delete("telco:plan:basic")` |
| `TTL telco:plan:basic` | `redis.getExpire("telco:plan:basic")` |

`opsForValue()` is for strings (Exercises 1 and 2), `opsForHash()` for hashes (Exercise 3) and `opsForZSet()`
for sorted sets (Bonus A).

The "String" in the name means keys and values are plain text. Redis can't store a Java `Plan` object, so you
turn it into JSON text before `set`, and back into a `Plan` after `get`. The `json` field (also injected)
does that: `json.writeValueAsString(plan)` and `json.readValue(text, Plan.class)`.

**Step 2: cache-aside by hand.** In the same file:

1. Set `PLAN_TTL` to your Decision 2 answer (e.g. `Duration.ofMinutes(...)`), with your reasoning in the comment above it.
2. Implement `getPlan(id)` with the cache-aside flow from the Concept recap, using the methods above:
   - Look up `planKey(id)` in Redis.
   - **Hit** (not `null`): turn the JSON back into a `Plan` and return it.
   - **Miss** (`null`): load the plan from `repository` (that line is already there), store it as JSON with
     `PLAN_TTL`, and return it.

```bash
./mvnw test -Dtest=Ex1Step2CacheAsideTest
```

Restart the app and send the same request a few times again. The first call is a miss, and every call after it
is a hit:

```
GET /ex1/plans/basic -> 200 in 1004 ms     <- miss: went to the slow database, then filled the cache
GET /ex1/plans/basic -> 200 in 3 ms        <- hit: came from Redis
```

**Step 3: let Spring do it.** Now the same thing with Spring's cache abstraction:

- **3a.** In [`ex1/service/AnnotatedPlanService.java`](src/main/java/com/tdp/telco/ex1/service/AnnotatedPlanService.java), annotate
  `getPlan` with `@Cacheable`, using the cache name `"plans"` and the plan id as the key (`key = "#id"`).
- **3b.** `@Cacheable` has no TTL attribute. In [`common/config/CacheConfig.java`](src/main/java/com/tdp/telco/common/config/CacheConfig.java),
  add `.entryTtl(...)` to the `"plans"` cache configuration.

```bash
./mvnw test -Dtest=Ex1Step3CacheableTest
```

**Step 4: see what the annotation actually did.** Restart the app, call both endpoints once, then in `redis-cli`:

```
KEYS *
GET plans::basic
TTL plans::basic
GET telco:plan:basic        (or whatever key you designed)
TTL telco:plan:basic
```

### 5. Verify

```bash
./mvnw test -Dtest='Ex1*'
```

All green? Then:

- `KEYS *` shows **your key** (e.g. `telco:plan:basic`) **and** Spring's key `plans::basic`. Spring's format is
  `<cacheName>::<key>`.
- `GET` on both shows JSON.
- `TTL` shows a number counting down. `-1` means "never expires", and `-2` means "no such key".

> `KEYS *` is fine on your laptop. Never run it in production: it blocks Redis while it scans every key. Use `SCAN`.

### 6. Reflect

1. Look at both keys in RedisInsight. Which one would a teammate understand at 3 a.m. during an incident? Is
   Spring's `plans::basic` namespaced enough if five services share this Redis? (Hint: `RedisCacheConfiguration.prefixCacheNameWith(...)`.)
2. `CacheConfig` stores plans as JSON. Without that line, Spring would use Java serialization. What would
   `GET plans::basic` show then, and why would a Node.js service sharing this Redis care?

### 7. Same idea in your Nest project

- **By hand** (like `ManualPlanService`), with `ioredis`: `const hit = await redis.get(key)`, then
  `await redis.set(key, JSON.stringify(plan), 'EX', 600)`.
- **Like `@Cacheable`**: `@nestjs/cache-manager` with a Redis store (`@keyv/redis`). `@UseInterceptors(CacheInterceptor)`
  on a controller caches GET responses, and `@CacheKey()` / `@CacheTTL()` tune them.
- ⚠️ `CacheInterceptor` keys by **request URL**, not by a key you design. In cache-manager v5+, the **TTL is in milliseconds**.
- For full control, inject `CACHE_MANAGER` and call `cache.get(key)` / `cache.set(key, value, ttlMs)` yourself.

### 8. Stuck?

```bash
git diff main origin/solutions -- '*/ManualPlanService.java' '*/AnnotatedPlanService.java' '*/CacheConfig.java'
```

That shows exactly what changes. To take the whole solution for this exercise:

```bash
git checkout origin/solutions -- '*/ManualPlanService.java' '*/AnnotatedPlanService.java' '*/CacheConfig.java'
```

<details>
<summary>Hint: what does a hit look like in code?</summary>

```java
String cached = redis.opsForValue().get(key);
if (cached != null) {
    return json.readValue(cached, Plan.class);
}
```
</details>

---

## Exercise 2: Prices change, the cache lies (15 min)

> **Learning goal:** by the end, you can explain why cached data goes stale, implement both invalidate-on-write
> and write-through, and choose between them (and write-behind) for a given use case.

### 1. The problem

Marketing drops the price of the Unlimited plan. The admin tool calls `PUT /ex2/plans/{id}/price`, and the
database is updated. But customers **keep seeing the old price**, for up to 10 minutes (the TTL). Support
gets angry calls: *"Your site says $65, you charged me $59.99!"*

Reproduce it (click through section "Exercise 2" in `requests.http`):

```bash
curl localhost:8080/ex2/plans/unlimited
```

```bash
curl -X PUT localhost:8080/ex2/plans/unlimited/price -H 'Content-Type: application/json' -d '{"monthlyPrice": 59.99}'
```

```bash
curl localhost:8080/ex2/plans/unlimited
```

The last call still says `65.00`. The cache is lying.

### 2. Think first

**Your goal:** keep the cache correct when data changes. After a price update, the very next read must return
the new price, not the cached old one.

1. The read path is cache-aside and is already done for you (`getPlan`). **Which code path knows the data changed?**
2. Two options: throw the cached copy away, or overwrite it with the new value. **Which one makes the *next*
   read slower? Which one does more work on *every* write?**

### 3. Concept recap

The lecture's quote: *there are only two hard things in computer science: cache invalidation and naming things.*

- **TTL** is time-based eviction. The data becomes *eventually* consistent, at the latest after the TTL.
- **Explicit invalidation** is event-based: when the source of truth changes, delete the stale key right away. The next
  read misses and refills it via cache-aside.
- **Write-through**: the write path updates the cache too, so reads are always hits with fresh data, but every
  write pays for a cache write. (In the lecture's diagram the cache writes to the database itself. Redis can't
  write to your database, so in practice **the application does both, in the same request, database first**.
  That's what you'll implement, and what `@CachePut` does.)
- **Write-behind** (write-back): write to the cache now, flush to the database later, asynchronously, in batches.
  Very fast writes, but you can **lose data** if the cache dies before the flush. Good for likes, view counters
  and metrics, not for prices or payments. *You won't implement it today.*

Real systems combine them: **explicit invalidation or write-through, plus a TTL as a safety net** for the write
path someone forgot.

### 4. Tasks

All in [`ex2/service/PlanPriceService.java`](src/main/java/com/tdp/telco/ex2/service/PlanPriceService.java) and
[`ex2/service/AnnotatedPlanPriceService.java`](src/main/java/com/tdp/telco/ex2/service/AnnotatedPlanPriceService.java).

**Step 1: invalidate on write.** In `updatePriceAndEvict`: update the database (already there), **then** delete the cached copy:
`redis.delete(key)` → `DEL key`.

```bash
./mvnw test -Dtest=Ex2Step1EvictOnWriteTest
```

**Step 2: write-through.** In `updatePriceWriteThrough`: update the database, then **write the updated plan** into
the cache with `opsForValue().set(...)`. Read the TODO's warning about TTLs.

```bash
./mvnw test -Dtest=Ex2Step2WriteThroughTest
```

**Step 3: the annotation versions.** In `AnnotatedPlanPriceService`:

- **3a.** `@CacheEvict(cacheNames = "plans", key = "#id")` on `updatePriceAndEvict`
- **3b.** `@CachePut(cacheNames = "plans", key = "#id")` on `updatePriceWriteThrough`

```bash
./mvnw test -Dtest=Ex2Step3AnnotationsTest
```

### 5. Verify

```bash
./mvnw test -Dtest='Ex2*'
```

Then restart the app and watch it happen in `redis-cli` (second terminal):

```
GET telco:plan:unlimited                  -> after the first GET: JSON with 65.00
                                             now PUT ...?strategy=evict
EXISTS telco:plan:unlimited               -> 0: deleted, next read is a MISS (~1s in the app log)
                                             now PUT ...?strategy=write-through
GET telco:plan:unlimited                  -> JSON with the NEW price, next read is a HIT
TTL telco:plan:unlimited                  -> still counting down
```

### 6. Reflect

1. **The ordering race.** Why update the database *first* and delete the cache *second*? Walk through this with two
   requests: A deletes the key, B reads (miss) and loads the **old** price from the database, A writes the new price
   to the database, B puts the old price back in the cache. How long does the wrong price stay now?
   (Even DB-first has a tiny window, which is one more reason to keep a TTL.)
2. **Pick a strategy** for each, and say why: *plan prices*, *a user's permissions*, *"likes" on a post*,
   *the home-page "featured plans" list*. Which one would you never cache?

### 7. Same idea in your Nest project

- There's no `@CacheEvict` in Nest. After the DB update, call `await this.cacheManager.del(key)` (or `redis.del(key)`).
- For write-through: `await this.cacheManager.set(key, updated, ttlMs)` after the DB update.
- **EDA bonus points:** publish a `plan.price-changed` event and let a listener do the `del`. Every service that
  caches plans can then react, which fits the event-driven part of your architecture grade.

### 8. Stuck?

```bash
git diff main origin/solutions -- '*/PlanPriceService.java' '*/AnnotatedPlanPriceService.java'
```

<details>
<summary>Hint: why does my write-through test say the TTL is -1?</summary>

`SET key value` without `EX` **replaces the value and removes the TTL**. Pass `PLAN_TTL` as the third argument of `set(...)`.
</details>

---

## Exercise 3: The account that changes one field at a time (15 min)

> **Learning goal:** by the end, you can choose a Redis data structure from the data's shape and access pattern,
> use hash operations for partial updates and atomic increments, and say when a plain string is still the better choice.

### 1. The problem

A customer account has name, address, current plan and **data used this month**. The first fields almost never
change. Data usage changes **constantly**: every few minutes per customer, from many network events at once.

The current cache, [`ex3/service/StringAccountCache.java`](src/main/java/com/tdp/telco/ex3/service/StringAccountCache.java)
(provided and working), stores the whole account as **one JSON string**. To record 10MB of usage it has to
GET the whole JSON, parse it, change one number, serialize it and SET the whole thing back.

Measure the damage. This experiment sends 200 parallel usage updates to the cache:

```bash
./mvnw test -Dtest=Ex3CompareStringVsHashTest
```

```
  ============ 200 parallel usage updates of 10MB ============
  Database (source of truth) : 6200 MB
  String cache (JSON, GET+SET): 4340 MB   -> 186 updates lost
  Hash cache   (HINCRBY)      : 6200 MB   -> 0 updates lost
```

The string cache **loses updates**: two requests read the same old value, both add 10 and both write back,
so one update vanishes. (The exact number changes from run to run.)

The hash line only looks right because `HashAccountCache` doesn't cache anything yet: it goes to the slow
database on every call. Your job is to make it **fast *and* correct**.

### 2. Think first

**Your goal:** cache the account in a Redis structure that fits how it's used, so a usage update changes only
the one field that changed, inside Redis, without losing updates.

1. Which fields are read together, and which field is read or written **alone**?
2. Where does the "+10" happen in `StringAccountCache`: in Java or in Redis? Why does that matter when 16 threads do it at once?

### 3. Concept recap

Choose the Redis structure by the data's **shape** and **access pattern**, not by habit. From the lecture:

- **String:** one value, read and written as a whole. Great for a serialized document, especially nested data
  (a shopping cart with items), or anything you always read in full.
- **Hash:** a flat map of `field → value` under one key. **Field-level operations**: read one field (`HGET`),
  update one field (`HSET`), increment a number in place (`HINCRBY`). No serializing and deserializing the whole object.

Redis runs commands one at a time (single-threaded), so **`HINCRBY` is atomic**. Read, add and write all happen
inside Redis as one step, and no update can be lost.

### 4. Tasks

All in [`ex3/service/HashAccountCache.java`](src/main/java/com/tdp/telco/ex3/service/HashAccountCache.java). The conversion
to and from hash fields is done for you in `AccountFields`.

**Step 1: cache the account as a hash.** Cache-aside again, with different operations:

- `redis.opsForHash().entries(key)` → `HGETALL key`. It returns an **empty map** on a miss, not `null`.
- `redis.opsForHash().putAll(key, AccountFields.toHash(account))` → `HSET key f1 v1 f2 v2 ...`
- `redis.expire(key, ACCOUNT_TTL)` → `EXPIRE key seconds`. A hash has **one TTL for the whole key**.

```bash
./mvnw test -Dtest=Ex3Step1HashReadTest
```

**Step 2: read one field.** `getDataUsedMb` should ask Redis for only `dataUsedMb`:
`redis.opsForHash().get(key, field)` → `HGET key field`.

```bash
./mvnw test -Dtest=Ex3Step2SingleFieldReadTest
```

**Step 3: atomic partial update.** In `addUsage`, after the database update, increment the cached field in place:
`redis.opsForHash().increment(key, field, mb)` → `HINCRBY key field mb`. Read the TODO's "careful".

```bash
./mvnw test -Dtest=Ex3Step3AtomicIncrementTest
```

### 5. Verify

```bash
./mvnw test -Dtest='Ex3*'
```

The comparison now shows `0 updates lost` for the hash cache. Then restart the app, call
`GET /ex3/accounts/acc-1001` and `GET /ex3/accounts/acc-1001?model=string`, and compare in RedisInsight or `redis-cli`:

```
TYPE telco:account:acc-1001           -> hash
HGETALL telco:account:acc-1001        -> one field per line
HINCRBY telco:account:acc-1001 dataUsedMb 100
HGET telco:account:acc-1001 dataUsedMb
TYPE telco:account-json:acc-1001      -> string
GET telco:account-json:acc-1001       -> one JSON blob
```

### 6. Reflect

1. The plans from Exercise 1 are also objects. Why is a **string** still the better choice for them? (Think:
   how are plans read, how often do they change, and do you ever update one field?)
2. Your final project has a user with a nested `preferences` object and a list of `addresses`. Hash or string?
   What would you lose with each? (Redis also has a JSON type with path updates, where available.)

### 7. Same idea in your Nest project

- `@nestjs/cache-manager` only does get/set/del of whole values. **For data structures, use `ioredis` directly.**
- `await redis.hset(key, { name, planId, dataUsedMb })`, `await redis.hgetall(key)` (returns `{}` on a miss),
  `await redis.hget(key, 'dataUsedMb')`, `await redis.hincrby(key, 'dataUsedMb', 10)`, `await redis.expire(key, 1800)`.
- Hash values come back as **strings**: `Number(...)` them.

### 8. Stuck?

```bash
git diff main origin/solutions -- '*/HashAccountCache.java'
```

<details>
<summary>Hint: the "half-empty hash" test fails</summary>

`HINCRBY` on a key that doesn't exist **creates** the hash with just that one field. The next `getAccount`
finds a non-empty hash and returns an account with no name. Only increment when the account is already cached
(`redis.hasKey(key)`). If it isn't cached, do nothing: the next read loads the fresh value from the database.
</details>

---

## Wrap-up: apply it to your final project (12 min)

Take your project's architecture sketch. Alone or with your team (5 min), fill in the checklist below for
**at least two** things you'll cache. Then a few teams share (7 min).

### ✅ Final project caching checklist

- [ ] **What are you caching, and why?** Pick read-heavy, rarely-changing, expensive data. Name what you
      deliberately *won't* cache (balances, inventory, permissions that must be correct right now...).
- [ ] **Where does it live?** Local (in-process) or shared Redis? Remember Location, Scope, Topology and Authority
      from the lecture. Redis is remote, shared and *just a copy*.
- [ ] **Key naming scheme.** Write the pattern, e.g. `<app>:<entity>:<id>`, and one real example per type.
- [ ] **TTL per data type, and why.** A table: data type → TTL → reason.
- [ ] **Read pattern.** Cache-aside? (Almost always yes.)
- [ ] **Write / invalidation strategy.** Evict on write, write-through, or write-behind, per data type, and why.
      Which code paths (or events!) write this data?
- [ ] **Data structure.** String, hash, sorted set, list or set, chosen by access pattern.
- [ ] **Size and eviction.** Roughly how many keys, and how big? Which `maxmemory-policy`? Does every cache key have a TTL?
- [ ] **What happens when Redis is down?** Fall back to the database? Fail?
- [ ] **How will you show it?** Put Redis as its own box on your architecture diagram, label the arrows
      ("cache-aside read", "DEL on price change", "event → evict"), and add a small legend table:
      key pattern | structure | TTL | invalidation. Be ready to defend each choice: that's what the graders look for.

---

## Bonus A: Most popular plans leaderboard (sorted sets)

> **Learning goal:** by the end, you can use a sorted set for live ranking and explain why Redis structures can
> replace extra database queries, not just cache them.

**The problem.** The home page shows the "Top 3 most popular plans". Today that's a `GROUP BY` over all sign-ups
on every page view (`GET /bonus-a/plans/top-from-db`, about 1s), and it gets slower as sign-ups grow.

**Think first.** Instead of caching the *result* of the query (and invalidating it on every sign-up), could
Redis keep the *ranking itself* up to date?

**Concept recap.** A **sorted set** stores unique members, each with a score, always kept sorted by score.
`ZINCRBY` adds to a score atomically in O(log N), and `ZREVRANGE ... WITHSCORES` reads the top N. This isn't a
copy of a query result. It's a live structure, updated on every write. It has no TTL. If it's lost, rebuild it once from the database.

**Tasks.** In [`bonusa/service/LeaderboardService.java`](src/main/java/com/tdp/telco/bonusa/service/LeaderboardService.java):

1. `recordSignup`: `redis.opsForZSet().incrementScore(LEADERBOARD_KEY, planId, 1)` → `ZINCRBY key 1 planId`
2. `topPlans`: `redis.opsForZSet().reverseRangeWithScores(LEADERBOARD_KEY, 0, limit - 1)` → `ZREVRANGE key 0 N-1 WITHSCORES`

**Verify.**

```bash
./mvnw test -Dtest=BonusALeaderboardTest
```

Then restart the app, run `POST /bonus-a/signups/simulate?count=500` from `requests.http`, compare
`/bonus-a/plans/top` with `/bonus-a/plans/top-from-db`, and in `redis-cli`:

```
ZREVRANGE telco:leaderboard:plan-signups 0 2 WITHSCORES
ZSCORE telco:leaderboard:plan-signups unlimited
```

**Reflect.** The sorted set and the database can drift apart if one write fails. How would you detect and
repair that? Is that worth it for a "popular plans" widget? Would it be for a billing total?

**Same idea in your Nest project.** `await redis.zincrby(key, 1, planId)`, then
`await redis.zrevrange(key, 0, 2, 'WITHSCORES')` (it returns a flat `[member, score, member, score...]` array).

**Stuck?** `git diff main origin/solutions -- '*/LeaderboardService.java'`

---

## Bonus B: Memory fills up (eviction)

> **Learning goal:** by the end, you can explain what happens when the cache is full, compare eviction policies,
> and explain how TTLs interact with eviction.

**The problem.** Redis lives in RAM, and RAM runs out. This Redis holds two kinds of keys:

- **feature flags** (`telco:flag:*`): a few keys, needed by every request, deliberately stored **without** a TTL
- **login sessions** (`telco:session:*`): thousands of keys, each only useful for 30 minutes

The config [`redis/bonus-b.conf`](redis/bonus-b.conf) gives Redis only 4MB. What gets deleted when it's full?

**Think first.** Which of the two kinds would you sacrifice first? Does Redis know that, unless you tell it?

**Concept recap.** When `maxmemory` is reached, Redis applies the `maxmemory-policy`:

| Policy | Evicts | Which keys |
|---|---|---|
| `noeviction` | nothing: **new writes fail** | none |
| `allkeys-lru` | least **recently** used | any key |
| `allkeys-lfu` | least **frequently** used | any key |
| `volatile-lru` | least recently used | only keys **with a TTL** |
| `volatile-ttl` | closest to expiring | only keys **with a TTL** |

LRU/LFU in Redis is *approximate*: it samples a few keys instead of checking all of them, which keeps it O(1).

**Tasks.**

1. Run the lab. It floods a 4MB Redis with 200 flags and 20,000 sessions, then prints what survived:

   ```bash
   ./mvnw test -Dtest=BonusBEvictionLabTest
   ```

   With `allkeys-lru` you should see **Flags still there: 0 / 200**. Your feature flags are gone!
2. Change `maxmemory-policy` to `volatile-lru` in `redis/bonus-b.conf` and run the lab again. Now look at
   **Writes refused**. Why? (Hint: which keys have a TTL right now?) Then fix it: in
   [`bonusb/service/SessionStore.java`](src/main/java/com/tdp/telco/bonusb/service/SessionStore.java), give sessions their TTL
   (TODO step 2). Run the lab again.
3. Try `volatile-ttl`, `allkeys-lfu` and `noeviction`. Predict the result before each run.

**Verify.**

```bash
./mvnw test -Dtest=BonusBSessionTtlTest
```

Want to poke at it yourself? `docker compose --profile bonus-b up -d redis-small` starts the same tiny Redis on
port 6380 (`docker compose exec redis-small redis-cli`, then `INFO memory`, `INFO stats`, `CONFIG GET maxmemory-policy`).
You can also add it to RedisInsight as a second database: host `localhost`, port `6380`.

**Reflect.** Which policy would you pick for your final project's Redis, and what must then be true about your
keys? What's the risk of `allkeys-lru` if you also keep data in Redis that *isn't* a cache (like the Bonus A leaderboard)?

**Same idea in your Nest project.** It's Redis configuration, not code. In your `docker-compose.yml`:
`command: redis-server --maxmemory 100mb --maxmemory-policy volatile-lru`, and give every cache key a TTL
(`SET key value EX ...`, or `ttl` in cache-manager).

**Stuck?** `git diff main origin/solutions -- '*/SessionStore.java'`

---

## Optional extras (reading only)

Topics from the lecture appendix, for when you design your project:

- **Cache stampede.** A hot key expires, and 1,000 requests miss at the same moment and all hit the database.
  Mitigations: only one request reloads while the others wait (`@Cacheable(sync = true)` does this per instance),
  add **random jitter** to TTLs so keys don't all expire together, or refresh hot keys *before* they expire.
- **Cache warming.** Load the most important keys at startup (e.g. on Spring's `ApplicationReadyEvent`, or in
  Nest's `onApplicationBootstrap`) so the first users after a deploy don't all pay the miss.
- **Redis pub/sub as an EDA bridge.** When a price changes, `PUBLISH telco:events plan.price-changed:unlimited`.
  Every service instance subscribes and evicts its *local* in-memory copy. Spring: `redisTemplate.convertAndSend(...)`
  plus a `RedisMessageListenerContainer`. Nest: `ioredis` `subscribe`, or a Redis transport for microservices.
  Pub/sub is fire-and-forget: a subscriber that is offline misses the message, so for durable events look at Redis Streams.

---

## Redis ops cheat sheet

All Spring methods are on `StringRedisTemplate redis`. Try any command in `redis-cli` (`docker compose exec redis redis-cli`).

### Keys and TTL

| redis-cli | Spring Data Redis | ioredis (Nest) | Notes |
|---|---|---|---|
| `KEYS telco:*` | `redis.keys("telco:*")` | `redis.keys('telco:*')` | Dev only, blocks Redis. Use `SCAN` |
| `SCAN 0 MATCH telco:* COUNT 100` | `redis.scan(ScanOptions...)` | `redis.scan(0, 'MATCH', 'telco:*')` | Safe iteration |
| `EXISTS key` | `redis.hasKey(key)` | `redis.exists(key)` | |
| `DEL key` | `redis.delete(key)` | `redis.del(key)` | |
| `TYPE key` | `redis.type(key)` | `redis.type(key)` | string / hash / zset ... |
| `EXPIRE key 600` | `redis.expire(key, Duration.ofMinutes(10))` | `redis.expire(key, 600)` | |
| `TTL key` | `redis.getExpire(key)` | `redis.ttl(key)` | `-1` no TTL, `-2` no key |

### Strings: one value per key

| redis-cli | Spring Data Redis | ioredis (Nest) |
|---|---|---|
| `GET key` | `redis.opsForValue().get(key)` | `redis.get(key)` |
| `SET key value EX 600` | `redis.opsForValue().set(key, value, Duration.ofMinutes(10))` | `redis.set(key, value, 'EX', 600)` |
| `SET key value NX EX 600` | `redis.opsForValue().setIfAbsent(key, value, ttl)` | `redis.set(key, value, 'EX', 600, 'NX')` |
| `INCRBY key 5` | `redis.opsForValue().increment(key, 5)` | `redis.incrby(key, 5)` |

### Hashes: field → value under one key

| redis-cli | Spring Data Redis | ioredis (Nest) |
|---|---|---|
| `HSET key f1 v1 f2 v2` | `redis.opsForHash().putAll(key, map)` | `redis.hset(key, obj)` |
| `HSET key field value` | `redis.opsForHash().put(key, field, value)` | `redis.hset(key, field, value)` |
| `HGET key field` | `redis.opsForHash().get(key, field)` | `redis.hget(key, field)` |
| `HGETALL key` | `redis.opsForHash().entries(key)` | `redis.hgetall(key)` |
| `HINCRBY key field 10` | `redis.opsForHash().increment(key, field, 10)` | `redis.hincrby(key, field, 10)` |
| `HDEL key field` | `redis.opsForHash().delete(key, field)` | `redis.hdel(key, field)` |

### Sorted sets: unique members ranked by score

| redis-cli | Spring Data Redis | ioredis (Nest) |
|---|---|---|
| `ZINCRBY key 1 member` | `redis.opsForZSet().incrementScore(key, member, 1)` | `redis.zincrby(key, 1, member)` |
| `ZADD key 42 member` | `redis.opsForZSet().add(key, member, 42)` | `redis.zadd(key, 42, member)` |
| `ZREVRANGE key 0 9 WITHSCORES` | `redis.opsForZSet().reverseRangeWithScores(key, 0, 9)` | `redis.zrevrange(key, 0, 9, 'WITHSCORES')` |
| `ZSCORE key member` | `redis.opsForZSet().score(key, member)` | `redis.zscore(key, member)` |
| `ZREVRANK key member` | `redis.opsForZSet().reverseRank(key, member)` | `redis.zrevrank(key, member)` |

### Spring cache annotations

| Annotation | Does | Redis ops |
|---|---|---|
| `@EnableCaching` | turns the annotations on (in `CacheConfig`) | |
| `@Cacheable(cacheNames="plans", key="#id")` | cache-aside: return cached value, else run the method and store the result | `GET plans::id`, on a miss `SET plans::id ... PX ttl` |
| `@CacheEvict(cacheNames="plans", key="#id")` | run the method, then delete the entry | `DEL plans::id` |
| `@CachePut(cacheNames="plans", key="#id")` | always run the method, store the **return value** | `SET plans::id ... PX ttl` |
| `RedisCacheConfiguration.entryTtl(...)` | TTL per cache name (annotations have no TTL) | |

---

## Troubleshooting

| Symptom | Fix |
|---|---|
| `Could not find a valid Docker environment` | Start Docker Desktop and wait until it says "running". |
| `release version 25 not supported` / `invalid target release` | Maven is using an older JDK. `java -version` must say 25. Set `JAVA_HOME` to your JDK 25. |
| `Port 6379 is already allocated` | Another Redis is running. Stop it (`brew services stop redis`, or `docker ps` and stop the container). |
| App starts but `Connection refused` to Redis | Run `docker compose up -d redis` yourself and restart the app. |
| RedisInsight can't connect | Check Redis is running: start the app, or run `docker compose up -d redis` to start only Redis. Use host `localhost`, port `6379`, and no username or password. |
| Changed the code, nothing changed | Restart the app. Tests always use the latest code. |
| A test passes in the IDE but you want the Maven output | `./mvnw test -Dtest=ClassName` |
| Old cached data confuses you | In `redis-cli`: `FLUSHALL` (deletes everything in your local Redis). |
