# Exercise 3: The account that changes one field at a time (15 min)

**You'll learn:** how to choose a Redis structure by how the data is used, and how to update one field atomically.

**The problem.** An account has a name, address, plan and **data used this month**. Usage changes constantly; the
rest almost never. [`StringAccountCache`](../src/main/java/com/tdp/telco/ex3/service/StringAccountCache.java)
(provided) stores the whole account as one JSON string, so every usage update means: GET the JSON, parse it,
change one number, SET it all back. Run this experiment, which sends 200 parallel usage updates:

```bash
./mvnw test -Dtest=Ex3CompareStringVsHashTest
```

```
  String cache (JSON, GET+SET): 4340 MB   -> 186 updates lost
```

Two requests read the same old value, both add 10 and both write back: one update vanishes. (The hash line looks
right only because `HashAccountCache` doesn't cache anything yet: it reads the database every time.)

**Your goal:** cache the account so a usage update changes only that one field, inside Redis, with no lost updates.

## Think first

1. Which fields are read together, and which one is read or written **alone**?
2. In `StringAccountCache`, does the "+10" happen in Java or in Redis? Why does that matter with 16 threads?

<details>
<summary>Recap from the lecture: strings vs hashes</summary>

- **String:** one value, read and written whole. Good for documents you always read in full, or nested data.
- **Hash:** a flat map of `field → value` under one key. Read one field (`HGET`), set one field (`HSET`), or add to a
  number in place (`HINCRBY`).
- Redis runs one command at a time, so `HINCRBY` is **atomic**: no update can be lost.
</details>

## Step 1: cache the account as a hash

In [`ex3/service/HashAccountCache.java`](../src/main/java/com/tdp/telco/ex3/service/HashAccountCache.java),
implement `getAccount` as cache-aside with a hash. `AccountFields` converts an account to and from hash fields.

| Do | Java | redis-cli |
|---|---|---|
| Read all fields | `redis.opsForHash().entries(key)`: an **empty map** on a miss | `HGETALL key` |
| Store all fields | `redis.opsForHash().putAll(key, AccountFields.toHash(account))` | `HSET key f1 v1 ...` |
| Set the TTL | `redis.expire(key, ACCOUNT_TTL)`: one TTL for the whole hash | `EXPIRE key 1800` |

```bash
./mvnw test -Dtest=Ex3Step1HashReadTest
```

## Step 2: read one field

`getDataUsedMb` should fetch only `dataUsedMb`: `redis.opsForHash().get(key, field)` (`HGET`).

```bash
./mvnw test -Dtest=Ex3Step2SingleFieldReadTest
```

## Step 3: update one field atomically

In `addUsage`, after the database update, add to the cached field in place:
`redis.opsForHash().increment(key, field, mb)` (`HINCRBY`). Read the "careful" in the TODO.

```bash
./mvnw test -Dtest=Ex3Step3AtomicIncrementTest
```

## Step 4: compare

Re-run the experiment: the hash cache now loses **0** updates. Restart the app, call
`/ex3/accounts/acc-1001` and `/ex3/accounts/acc-1001?model=string`, then compare in Redis:

```
HGETALL telco:account:acc-1001        -> one field per line
GET telco:account-json:acc-1001       -> one JSON blob
```

All done? `./mvnw test -Dtest='Ex3*'` should be all green. **Next: [Wrap-up](wrap-up.md).**

<details>
<summary>Hint: the "half-empty hash" test fails</summary>

`HINCRBY` on a missing key **creates** a hash with only that field, and the next `getAccount` returns an account
with no name. Only increment when the key exists (`redis.hasKey(key)`). Otherwise do nothing: the next read loads
fresh data from the database.
</details>

<details>
<summary>Stuck? See or take the solution</summary>

```bash
git diff main origin/solutions -- '*/HashAccountCache.java'
```

```bash
git checkout origin/solutions -- '*/HashAccountCache.java'
```
</details>

<details>
<summary>Same idea in your Nest project</summary>

`@nestjs/cache-manager` only stores whole values, so use `ioredis` for structures: `redis.hset(key, obj)`,
`redis.hgetall(key)` (`{}` on a miss), `redis.hget(key, 'dataUsedMb')`, `redis.hincrby(key, 'dataUsedMb', 10)`,
`redis.expire(key, 1800)`. Hash values come back as strings.
</details>
