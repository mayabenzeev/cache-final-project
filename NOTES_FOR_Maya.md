# Notes for Maya

Open questions, assumptions, and the decisions I made while building the workshop.
Things that need **your decision** are marked 🟡.

## What's in the repo

| Branch | Contents |
|---|---|
| `main` | Start point for trainees: README + exercise pages, Docker Compose, Exercises 1–3 as TODOs. 18 of 25 tests fail on purpose, each with a hint. |
| `solutions` | Exercises 1–3 solved, with comments explaining each decision. All 25 tests pass. |
| `bonus` | `main` + Bonus A (leaderboard) and Bonus B (eviction) as TODOs, with their pages in `exercises/`. 23 of 31 tests fail on purpose. |
| `bonus-solutions` | Everything solved, bonuses included. All 31 tests pass. |

At your request the bonuses are off `main`, so the workshop branch has only what fits in the hour.
`bonus` is built as `main` plus one commit that adds the bonuses back, so merging `main` into it later keeps
them. After changing `main`, merge `main` into `solutions` and `bonus`, then `bonus` into `bonus-solutions`.

### Why one branch instead of a branch per checkpoint

You agreed to this before I started building. Recorded here so the reasoning stays with the repo:

- **Exercises are independent.** Each exercise has its own service classes, with starting code that works
  without the previous exercise being finished. A trainee stuck on Exercise 1 at minute 18 moves on to
  Exercise 2 with no git commands, no stashing and no lost work.
- **"Stuck?" is one command:** `git diff main origin/solutions -- '*/ManualPlanService.java' ...` shows exactly
  the lines to change, and `git checkout origin/solutions -- <same files>` takes the solution for one exercise only.
- **Tests per step** (`Ex1Step2CacheAsideTest`, ...) give the same "checkpoint" feedback the branches would have.

### Layout: one folder per exercise, layers inside

At your request: `ex1/`, `ex2/`, `ex3/` (and `bonusa/`, `bonusb/` on the bonus branches) each contain their own `controller/`, `dto/`
and `service/` folders, and `common/` holds what all exercises share (`entity/`, `repository/`, `exception/`,
`config/`). A trainee opens one folder per exercise, and their TODOs are always in its `service/` folder,
plus one in `common/config/CacheConfig` for Exercise 1. That also teaches where caching belongs: in the service
layer, invisible to controllers and repositories.

- **No separate `dao` package.** In Spring a `@Repository` *is* the DAO (data access object), so having both
  would be two names for the same role. `common/repository` is the slow database, the source of truth.
- **`CacheConfig` stays in `common/config`** even though Exercise 1 edits it, because Exercise 2's annotations
  use the same `"plans"` cache configuration.
- **Tests mirror the exercise folders** (`ex1/`, `ex2/`, ...). The few members the tests need from the
  `service` subpackages (e.g. `planKey`, `LEADERBOARD_KEY`) are `public` for that reason.

### Docs: one page per exercise, details collapsed

The single README was 6,100 words, about 3,400 of them needed during the hour itself. That's 15–20 minutes of
reading. It's now split by when each page is read:

| File | Read when |
|---|---|
| `README.md` | at home (setup) and at the start (how it works, start the app) |
| `exercises/ex1.md`, `ex2.md`, `ex3.md` | while doing that exercise |
| `exercises/wrap-up.md` | minutes 48–60 |
| `exercises/bonus-a.md`, `bonus-b.md` (on `bonus`) | fast finishers, homework |
| `CHEATSHEET.md` | kept open in a tab |

Each exercise page shows the goal, the problem, "Think first" and the numbered steps with their test commands.
Everything else is in collapsible sections: the lecture recaps, the `StringRedisTemplate` / `@Cacheable`
introductions, hints, solution commands and the Nest mapping. A trainee who opens nothing reads about 1,650 words
in the hour.

**Deviation from the handoff:** it asked for 8 parts per exercise. All 8 are still there, but "Reflect" moved
from each exercise into `wrap-up.md` as the group discussion, so trainees don't pause alone mid-exercise. The
bonus pages keep a one-line Reflect. The pre-split README is tagged `readme-before-trim`.

## Verified

- `main`: the app starts, and each step test fails with the intended message. Of the 25 tests, 18 fail. The 7
  that pass are `SetupCheckTest`, the string-vs-hash experiment, and 5 tests that only start to matter once an
  earlier step is done (e.g. "a write-through keeps the TTL").
- `solutions`: all 25 tests pass. I ran the full suite several times to check for flakiness (see the async cache writes below).
- `bonus`: 23 of 31 fail (main's 18 plus the 5 bonus step tests). `bonus-solutions`: all 31 pass.
- The real app on the Docker Compose Redis: ~1000 ms on a miss, ~5–10 ms on a hit, price updates visible right
  after the PUT, and hashes and sorted sets visible in `redis-cli`.
- Spring Boot's Docker Compose support starts `redis` automatically on `./mvnw spring-boot:run`.
- Trainees use the **RedisInsight desktop app** from the lecture (slide 114), connected to `localhost:6379`.
  There's no RedisInsight container, so `docker compose pull` downloads only the 159 MB Redis image.
- Timings on my machine (M-series Mac): a single-step test run takes ~10 s and an app restart ~9 s.

**Not verified:** Windows, and a corporate network or proxy. See the assumptions below.

## Versions (all pinned)

| | Version |
|---|---|
| Java | 25 (built and tested with Temurin 25.0.5) |
| Spring Boot | 4.1.2 (latest on Maven Central at build time) |
| Spring Data Redis | 4.1.2 (via Boot) |
| Jackson | 3.x (`tools.jackson`, Boot 4's default) |
| Redis | `redis:8.10.2-alpine` |
| RedisInsight | desktop app, any recent version (trainees install it themselves) |
| Testcontainers | 2.0.5 (via Boot) |
| Maven | 3.10.0 via the wrapper (no Maven install needed) |

## Timed dry run: estimated trainee time

I worked through each exercise against the exercise pages, counting reading, thinking, typing, a ~10 s test run per
step and one or two app restarts:

| Exercise | Estimate | Notes |
|---|---|---|
| 1 | 14–16 min | The densest one: key design, cache-aside, `@Cacheable` + TTL, then inspecting Redis. |
| 2 | 12–15 min | Four small edits. Most of the time goes on reproducing the bug and the reflection questions. |
| 3 | 13–16 min | Step 1 is the biggest edit (~8 lines). The string-vs-hash experiment is the "aha". |

🟡 **Suggestion:** at minute 18 and minute 33, say out loud "move on, take the solution with `git checkout
origin/solutions -- src/main/java/com/tdp/telco/exN`". Because the exercises are independent, nobody falls behind for good.

🟡 **Pairing:** consider pairs for the core exercises. The "Think first" and "Reflect" questions work much better
when spoken out loud, and pairs get stuck less.

## Lecture alignment

I extracted the deck's text and speaker notes and aligned the terminology:

- The four patterns are named as in slide 47: **Cache-Aside, Write-Through, Write-Behind, Read-Through**.
- "**Explicit invalidation** (event-based) vs **TTL** (time-based), with TTL as a safety net" follows slides 60–61.
- The Karlton quote (slide 54) opens the Exercise 2 recap.
- **Location / Scope / Topology / Authority** (slides 46 and 88) appear in the final-project checklist.
- "Flat fields → Hash, nested document → String", plus the Redis JSON nuance (slides 107–109), is the Exercise 3 recap and reflection.
- The eviction policies table (Bonus B) extends slide 97 with `allkeys-lfu` and `volatile-ttl`, and repeats the
  speaker note that Redis LRU is sampled and approximate.
- The key examples follow the `user:123` / `catalog:sku:8842` style from slide 27.
- Stampede, warming and pub/sub are in "Optional extras" (slides 81 and 70, the appendix).

🟡 **Questions or possible slide fixes:**

1. **Write-through definition.** The slide and notes describe write-through as *the cache* writing to the DB
   synchronously, with a framework managing it. The workshop implements the app-managed version: the
   application writes the DB and then the cache in the same request, which is also what `@CachePut` does. The
   README explains this in one sentence and points back to the lecture. Is that framing OK with you?
2. **Slide 48 notes** say "(write back)" next to the cache-aside *write* path (update the DB, delete the cache).
   "Write-back" usually means write-behind (slide 50), so trainees may mix them up. Did you mean "write-around"?
3. **Slide 122** shows `## (nil) = expired` for `TTL`. `TTL` never returns nil: an expired or missing key returns
   `-2`, as your own speaker note says. The README uses `-1` = no TTL and `-2` = no key.
4. Small typos: slide 83 "Fundumentals", and slide 93 "slower then RAM".

## Assumptions to confirm 🟡

1. **JDK 25 on trainee laptops.** It's the main setup risk. If corporate laptops can't easily install JDK 25,
   there are two options: (a) drop to Java 21 (one line in `pom.xml`; the code uses no Java 25-only features),
   or (b) switch to Gradle with toolchain auto-provisioning, so the JDK downloads itself.
2. **Docker Hub access** from the AT&T network. If pulls are blocked or rate-limited, point the images in
   `docker-compose.yml` and `TestcontainersConfiguration.REDIS_IMAGE` at an internal mirror.
3. **OS.** Everything works on Windows too (use `mvnw.cmd`). Timings come from the app console log, which is
   the same on every OS. The README's `curl` examples use bash quoting. On Windows, `requests.http` sends the
   same requests.
4. **RedisInsight desktop app.** I assume everyone still has it from the lecture. "Before the session" lists it
   with an install command and has trainees add the `telco-workshop` database (`localhost:6379`) at home.
5. **Domain.** I used the suggested telco domain: plans (`basic`, `unlimited`, ...), accounts (`acc-1001`, ...)
   and sign-ups. The names are invented.
6. **Repo hosting.** Nothing is pushed anywhere. The repo is local with two branches. Push it to wherever
   trainees will clone from, and replace `<this-repo-url>` in the README.

## Technical things worth knowing

- **Spring Data Redis 4 does cache writes asynchronously by default.** `@Cacheable` puts and `@CacheEvict` deletes
  are sent "fire and forget", so a read right after can miss. This made the tests flaky, and trainees inspecting
  `redis-cli` would have been confused too. `CacheConfig` turns on `immediateWrites()` and explains why in a comment.
  It's a nice discussion point if someone asks.
- **Jackson 3** (`tools.jackson.databind.json.JsonMapper`) throws unchecked exceptions, so the manual
  (de)serialization code needs no try/catch.
- **The tests use their own Redis** (Testcontainers), so running tests never wipes what a trainee is looking at
  in RedisInsight. Bonus B tests start their own tiny Redis.
- **Exercise 3's experiment** (`Ex3CompareStringVsHashTest`) consistently loses 150–190 of 200 updates with the
  JSON-string cache, and 0 with `HINCRBY`.
- **Bonus B lab results** on my machine (4 MB, 200 flags + 20,000 sessions):

  | Policy | Sessions without TTL (start) | Sessions with TTL (solution) |
  |---|---|---|
  | `allkeys-lru` | all 200 flags evicted | all 200 flags evicted |
  | `volatile-lru` | ~16,000 writes refused (nothing evictable) | flags survive, old sessions evicted |
  | `volatile-ttl` | not run | flags survive, sessions evicted |
  | `allkeys-lfu` | not run | all 200 flags evicted |
  | `noeviction` | ~16,000 writes refused | ~16,000 writes refused |

  The `volatile-lru` + no TTL → "writes refused" result is the strongest moment of Bonus B.

## Possible next steps (not done)

- A short Nest.js companion repo with the same three exercises, if trainees ask for it.
- A stampede exercise (`@Cacheable(sync = true)` plus jittered TTLs) as Bonus C.
