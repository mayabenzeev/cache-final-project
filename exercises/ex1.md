# Exercise 1: The slow plan catalogue (15 min)

**You'll learn:** the cache-aside flow, how to design a key, how to choose a TTL, and what `@Cacheable` does for you.

**The problem.** `GET /ex1/plans/{id}` takes about 1 second, every time. Thousands of people view plans, and
prices change a few times a year: the database does the same slow work over and over. Click the first request in
[`requests.http`](../requests.http) a few times and watch the app console:

```
GET /ex1/plans/basic -> 200 in 1004 ms
GET /ex1/plans/basic -> 200 in 1002 ms
```

**Your goal:** add a caching layer. Keep a copy of each plan in Redis, so repeated reads skip the slow database.

## Think first: two decisions

Don't code yet. Take two minutes and write your answers down.

**Decision 1: the key** (used in Step 1). Redis stores each value under a key, like `user:123` in the lecture.

- Which field identifies a plan? See [`Plan.java`](../src/main/java/com/tdp/telco/common/entity/Plan.java).
  The real ids are in [`PlanRepository.java`](../src/main/java/com/tdp/telco/common/repository/PlanRepository.java).
- Accounts ([`Account.java`](../src/main/java/com/tdp/telco/common/entity/Account.java)) have ids too, and at work
  one Redis is often shared by several services. Redis has no tables: all keys sit in one flat list.

→ **What key would you store plan `basic` under, so it can't clash with an account or another service's key?**

**Decision 2: the TTL** (used in Step 2). A TTL makes Redis delete the copy after a while, so a stale copy can't
live forever.

→ **How long should a cached plan live? What do you gain with a longer TTL, and what do you risk?**

<details>
<summary>Recap from the lecture: cache-aside</summary>

```
read(id):
  value = cache.get(key)        -> HIT: return it
  if missing:                   -> MISS:
     value = db.load(id)           go to the source of truth
     cache.set(key, value, ttl)    store a copy for the next reader
  return value
```

The cache is a copy, not the source of truth. Long TTL: more hits, staler data. Short TTL: fresher data, more
database load.
</details>

## Step 1: the key

In [`ex1/service/ManualPlanService.java`](../src/main/java/com/tdp/telco/ex1/service/ManualPlanService.java),
`planKey(id)` currently returns just the id. Make it return your Decision 1 key, with your reasoning in the comment.

```bash
./mvnw test -Dtest=Ex1Step1KeyDesignTest
```

## Step 2: cache-aside by hand

In the same file:

1. Set `PLAN_TTL` to your Decision 2 answer, e.g. `Duration.ofMinutes(...)`.
2. Implement `getPlan(id)`: look up `planKey(id)` in Redis. **Hit** (not `null`): return the cached plan.
   **Miss:** load it from `repository`, store it with `PLAN_TTL`, return it.

<details>
<summary>New to <code>StringRedisTemplate</code>? Read this first</summary>

It's Spring's object for sending Redis commands, already injected as the field `redis`. Its methods mirror
`redis-cli`:

| redis-cli | Java |
|---|---|
| `GET key` | `redis.opsForValue().get(key)` → the value, or `null` |
| `SET key value EX 600` | `redis.opsForValue().set(key, value, Duration.ofMinutes(10))` |
| `DEL key` | `redis.delete(key)` |

Redis stores text, not Java objects. Turn the plan into JSON with `json.writeValueAsString(plan)`, and back with
`json.readValue(text, Plan.class)`. `json` is also injected.
</details>

```bash
./mvnw test -Dtest=Ex1Step2CacheAsideTest
```

Restart the app and send the request a few times: the first call is a miss, the rest are hits.

```
GET /ex1/plans/basic -> 200 in 1004 ms     <- miss
GET /ex1/plans/basic -> 200 in 3 ms        <- hit
```

## Step 3: let Spring do it

- **3a.** In [`AnnotatedPlanService.java`](../src/main/java/com/tdp/telco/ex1/service/AnnotatedPlanService.java),
  add `@Cacheable` to `getPlan`. Cache name: `"plans"`. Key: the plan's id.
- **3b.** In [`CacheConfig.java`](../src/main/java/com/tdp/telco/common/config/CacheConfig.java), add
  `.entryTtl(...)` to the `"plans"` cache, with the same TTL as Decision 2.

<details>
<summary>New to <code>@Cacheable</code>? Read this first</summary>

It writes the cache-aside flow from Step 2 for you. Example from a library app:

```java
@Cacheable(cacheNames = "books", key = "#isbn")
public Book findBook(String isbn) {
    return bookRepository.findByIsbn(isbn);   // only the database part
}
```

On each call Spring builds the key `books::<isbn>` (`#isbn` = the `isbn` parameter). **Hit:** it returns the cached
value and the method doesn't run. **Miss:** it runs the method and stores the result.

- There's no TTL on the annotation. It's set per cache name in `CacheConfig`, and `"plans"` has none yet (Step 3b).
- `@EnableCaching` in `CacheConfig` switches the annotations on.
- It only works when called from another class: a method calling its own `getPlan` skips the cache.
</details>

```bash
./mvnw test -Dtest=Ex1Step3CacheableTest
```

## Step 4: look inside Redis

Restart the app, call `/ex1/plans/basic` and `/ex1/annotated/plans/basic` once each, then in RedisInsight or
`redis-cli`:

```
KEYS *                  -> your key, e.g. telco:plan:basic, and Spring's plans::basic
GET plans::basic        -> the plan as JSON
TTL plans::basic        -> seconds left (-1 = never expires, -2 = no such key)
```

All done? `./mvnw test -Dtest='Ex1*'` should be all green. **Next: [Exercise 2](ex2.md).**

<details>
<summary>Hint: what does a hit look like in code?</summary>

```java
String cached = redis.opsForValue().get(key);
if (cached != null) {
    return json.readValue(cached, Plan.class);
}
```
</details>

<details>
<summary>Stuck? See or take the solution</summary>

From the repo folder, see what changes:

```bash
git diff main origin/solutions -- '*/ManualPlanService.java' '*/AnnotatedPlanService.java' '*/CacheConfig.java'
```

Or take it and move on:

```bash
git checkout origin/solutions -- '*/ManualPlanService.java' '*/AnnotatedPlanService.java' '*/CacheConfig.java'
```
</details>

<details>
<summary>Same idea in your Nest project</summary>

- **By hand**, with `ioredis`: `await redis.get(key)`, then `await redis.set(key, JSON.stringify(plan), 'EX', 600)`.
- **Like `@Cacheable`:** `@nestjs/cache-manager` with a Redis store. `@UseInterceptors(CacheInterceptor)` caches GET
  responses, keyed by **request URL**. In cache-manager v5+, **TTLs are in milliseconds**.
</details>
