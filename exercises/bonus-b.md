# Bonus B: Memory fills up

**You'll learn:** what Redis deletes when memory is full, and why TTLs decide what *can* be deleted.

**The problem.** Redis lives in RAM, and RAM runs out. This Redis holds:

- **feature flags** (`telco:flag:*`): few keys, needed by every request, stored **without** a TTL on purpose
- **login sessions** (`telco:session:*`): thousands of keys, each useful for 30 minutes

[`redis/bonus-b.conf`](../redis/bonus-b.conf) gives Redis only 4 MB. What gets deleted when it's full?

**Think first.** Which of the two would you sacrifice? Does Redis know that, unless you tell it?

<details>
<summary>Recap from the lecture: eviction policies</summary>

| Policy | When memory is full, it deletes | Among |
|---|---|---|
| `noeviction` | nothing: **new writes fail** | |
| `allkeys-lru` | the least recently used key | all keys |
| `allkeys-lfu` | the least frequently used key | all keys |
| `volatile-lru` | the least recently used key | only keys **with a TTL** |
| `volatile-ttl` | the key closest to expiring | only keys **with a TTL** |

Redis's LRU is approximate: it samples a few keys instead of scanning them all.
</details>

## Steps

1. Run the lab. It fills a 4 MB Redis with 200 flags and 20,000 sessions, and prints what survived:

   ```bash
   ./mvnw test -Dtest=BonusBEvictionLabTest
   ```

   With `allkeys-lru`: **Flags still there: 0 / 200**. Your feature flags are gone.
2. In `redis/bonus-b.conf`, set `maxmemory-policy volatile-lru` and run the lab again. Look at **Writes refused**.
   Why? Then give sessions their TTL in
   [`bonusb/service/SessionStore.java`](../src/main/java/com/tdp/telco/bonusb/service/SessionStore.java) and run it again.
3. Try `volatile-ttl`, `allkeys-lfu` and `noeviction`. Predict each result first.

```bash
./mvnw test -Dtest=BonusBSessionTtlTest
```

**Reflect:** which policy would you choose for your project's Redis, and what must then be true about your keys?

<details>
<summary>Poke at it yourself</summary>

`docker compose --profile bonus-b up -d redis-small` starts the same tiny Redis on port 6380. Add it to
RedisInsight (`localhost`, `6380`), or run `docker compose exec redis-small redis-cli` and try `INFO memory`,
`INFO stats` and `CONFIG GET maxmemory-policy`.
</details>

<details>
<summary>Stuck? See the solution</summary>

```bash
git diff main origin/solutions -- '*/SessionStore.java'
```
</details>

<details>
<summary>Same idea in your Nest project</summary>

It's Redis configuration, not code. In your `docker-compose.yml`:
`command: redis-server --maxmemory 100mb --maxmemory-policy volatile-lru`, and give every cache key a TTL.
</details>
