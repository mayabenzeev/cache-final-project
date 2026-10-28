# Exercise 2: Prices change, the cache lies (15 min)

**You'll learn:** why cached data goes stale, and how to keep it fresh with invalidate-on-write or write-through.

**The problem.** Marketing drops the Unlimited plan's price with `PUT /ex2/plans/{id}/price`. The database is
updated, but customers **keep seeing the old price** until the cached copy expires. Reproduce it with the
"Exercise 2" requests in [`requests.http`](../requests.http): view the plan, change the price, view it again.
It still says `65.00`.

**Your goal:** after a price update, the very next read must return the new price.

## Think first

1. Reads are already cached for you (`getPlan`). **Which code path knows the data changed?**
2. You can throw the cached copy away, or overwrite it with the new value. **Which makes the next read slower?
   Which does more work on every write?**

<details>
<summary>Recap from the lecture: TTL, invalidation and write strategies</summary>

- **TTL:** time-based. The copy is fresh again at the latest when it expires.
- **Explicit invalidation (evict on write):** when the data changes, delete the key. The next read misses and reloads it.
- **Write-through:** the write path also updates the cache, so the next read is a hit with fresh data. Redis can't
  write to your database, so the application does both: database first, then cache.
- **Write-behind:** write the cache now and the database later, in batches. Very fast, but data is lost if the cache
  dies first. Good for likes or view counts, not for prices. *Not implemented today.*

Real systems combine one of these with a TTL as a safety net.
</details>

## Step 1: invalidate on write

In [`ex2/service/PlanPriceService.java`](../src/main/java/com/tdp/telco/ex2/service/PlanPriceService.java),
`updatePriceAndEvict` updates the database. After that, delete the cached copy with `redis.delete(key)`.

```bash
./mvnw test -Dtest=Ex2Step1EvictOnWriteTest
```

## Step 2: write-through

In the same file, `updatePriceWriteThrough`: after the database update, write the **updated** plan into the cache
instead of deleting it.

```bash
./mvnw test -Dtest=Ex2Step2WriteThroughTest
```

## Step 3: the annotation versions

In [`AnnotatedPlanPriceService.java`](../src/main/java/com/tdp/telco/ex2/service/AnnotatedPlanPriceService.java):

- **3a.** Add `@CacheEvict` to `updatePriceAndEvict`. It must point at the same cache entry `getPlan` reads:
  look at `getPlan`'s annotation.
- **3b.** Add `@CachePut` to `updatePriceWriteThrough`, pointing at that same entry.

<details>
<summary>New to <code>@CacheEvict</code> and <code>@CachePut</code>? Read this first</summary>

They're the write-side partners of `@Cacheable`, and they do what you wrote by hand in Steps 1 and 2. Same library
app as before:

```java
@Cacheable(cacheNames = "books", key = "#isbn")      // read: cached under books::<isbn>
public Book findBook(String isbn) { ... }

@CacheEvict(cacheNames = "books", key = "#isbn")     // invalidate on write
public Book renameBook(String isbn, String title) { ... }

@CachePut(cacheNames = "books", key = "#isbn")       // write-through
public Book repriceBook(String isbn, BigDecimal price) { ... }
```

- **`@CacheEvict`** runs the method, then deletes that cache entry: Step 1's `redis.delete(...)`. It deletes *after*
  the method by default, so the database is updated first.
- **`@CachePut`** always runs the method, then stores **what the method returns** in the cache: Step 2's `set(...)`.
  That's why the method must return the updated object, not `void`.
- The cache name and key must match the `@Cacheable` read exactly. Otherwise you evict or update a different
  entry, and the read still finds the old one.
</details>

```bash
./mvnw test -Dtest=Ex2Step3AnnotationsTest
```

## Step 4: watch it in Redis

Restart the app. View `unlimited`, change its price with `?strategy=evict`, then with `?strategy=write-through`
(all in `requests.http`), checking Redis after each:

```
EXISTS telco:plan:unlimited   -> after evict: 0. The next read is a miss (~1 s in the app console)
GET telco:plan:unlimited      -> after write-through: the new price. The next read is a hit
```

All done? `./mvnw test -Dtest='Ex2*'` should be all green. **Next: [Exercise 3](ex3.md).**

<details>
<summary>Hint: my write-through test says the TTL is -1</summary>

`SET key value` without a TTL replaces the value **and removes the TTL**. Pass `PLAN_TTL` as the third argument of
`set(...)`.
</details>

<details>
<summary>Stuck? See or take the solution</summary>

```bash
git diff main origin/solutions -- '*/PlanPriceService.java' '*/AnnotatedPlanPriceService.java'
```

```bash
git checkout origin/solutions -- '*/PlanPriceService.java' '*/AnnotatedPlanPriceService.java'
```
</details>

<details>
<summary>Same idea in your Nest project</summary>

- There's no `@CacheEvict` in Nest. After the database update, call `await this.cacheManager.del(key)`.
- Write-through: `await this.cacheManager.set(key, updated, ttlMs)` after the database update.
- **Event-driven:** publish a `plan.price-changed` event and let a listener do the `del`, so every service that
  caches plans can react. That fits the EDA part of your project grade.
</details>
