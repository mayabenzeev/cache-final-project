# Wrap-up: apply it to your final project (12 min)

## Discuss (5 min)

Pick two or three of these with the people around you:

- **Keys:** Spring's key `plans::basic` has no app name in it. Is that good enough if five services share one
  Redis? Which key would a teammate understand at 3 a.m. during an incident?
- **Ordering:** in Exercise 2 you updated the database *first* and the cache *second*. What goes wrong the other way
  round, when a read sneaks in between?
- **Strategy:** evict, write-through, or don't cache at all? Decide for *plan prices*, *a user's permissions*,
  *"likes" on a post*, and *a bank balance*.
- **Structure:** plans are objects too. Why is a string still right for them, while accounts became a hash?

## Your project checklist (7 min)

With your team, fill this in for **at least two** things you'll cache:

- [ ] **What and why.** Read-heavy, rarely changing, expensive to get. Also name what you *won't* cache.
- [ ] **Key pattern**, e.g. `<app>:<entity>:<id>`, with one real example.
- [ ] **TTL and why**, per data type.
- [ ] **Keeping it fresh:** evict on write, write-through, or write-behind? Which code paths, or events, change the data?
- [ ] **Structure:** string, hash, sorted set, list or set, chosen by how the data is used.
- [ ] **Size and eviction:** how many keys? Which `maxmemory-policy`? Does every cache key have a TTL?
- [ ] **Redis is down:** fall back to the database, or fail?
- [ ] **Show it:** draw Redis as its own box in your architecture diagram, label the arrows ("cache-aside read",
      "evict on price change") and add a table: key pattern, structure, TTL, invalidation. The graders will ask why.

<details>
<summary>Going further: stampede, warming, pub/sub</summary>

- **Cache stampede:** a hot key expires and 1,000 requests miss at once. Let one request reload while the others
  wait (`@Cacheable(sync = true)`), add random jitter to TTLs, or refresh hot keys before they expire.
- **Cache warming:** load the most important keys at startup, so the first users after a deploy don't all miss.
- **Pub/sub for EDA:** on a price change, `PUBLISH` an event; every instance subscribes and evicts its local copy.
  Pub/sub is fire-and-forget, so for durable events use Redis Streams.
</details>

Done early? Try [Bonus A](bonus-a.md) or [Bonus B](bonus-b.md).
