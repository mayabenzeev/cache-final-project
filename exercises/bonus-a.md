# Bonus A: Most popular plans leaderboard

**You'll learn:** to keep a live ranking in a Redis sorted set, instead of re-running a database query.

**The problem.** The home page shows the top 3 most popular plans. Today that's a `GROUP BY` over all sign-ups
on every view (`GET /bonus-a/plans/top-from-db`, about 1 s), and it gets slower as sign-ups grow.

**Think first.** Instead of caching the query's *result* and invalidating it on every sign-up, could Redis keep
the *ranking itself* up to date?

<details>
<summary>Recap from the lecture: sorted sets</summary>

A sorted set holds unique members, each with a score, always kept in score order. `ZINCRBY` adds to a score
atomically, and `ZREVRANGE ... WITHSCORES` reads the top N. This isn't a copy of a query result but live data,
updated on every sign-up, so it has no TTL. If it's lost, rebuild it once from the database.
</details>

## Steps

In [`bonusa/service/LeaderboardService.java`](../src/main/java/com/tdp/telco/bonusa/service/LeaderboardService.java):

1. `recordSignup`: add 1 to the plan's score with `redis.opsForZSet().incrementScore(...)` (`ZINCRBY`).
2. `topPlans`: read the top `limit` plans, highest first, with `redis.opsForZSet().reverseRangeWithScores(...)`
   (`ZREVRANGE ... WITHSCORES`). Ranges are inclusive.

```bash
./mvnw test -Dtest=BonusALeaderboardTest
```

Then restart the app, run `POST /bonus-a/signups/simulate?count=500` from `requests.http`, and compare
`/bonus-a/plans/top` with `/bonus-a/plans/top-from-db`. In Redis:

```
ZREVRANGE telco:leaderboard:plan-signups 0 2 WITHSCORES
```

**Reflect:** the sorted set and the database can drift apart if one write fails. How would you repair it? Is it
worth it for a widget? For a billing total?

<details>
<summary>Stuck? See the solution</summary>

```bash
git diff main origin/solutions -- '*/LeaderboardService.java'
```
</details>

<details>
<summary>Same idea in your Nest project</summary>

`await redis.zincrby(key, 1, planId)`, then `await redis.zrevrange(key, 0, 2, 'WITHSCORES')`, which returns a flat
`[member, score, member, score, ...]` array.
</details>
