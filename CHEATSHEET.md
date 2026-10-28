# Redis cheat sheet

Each Redis command with its Spring (`StringRedisTemplate redis`) and Nest (`ioredis`) equivalent.
Try any command in RedisInsight's CLI, or in a terminal with `docker compose exec redis redis-cli`.

## Keys and TTL

| redis-cli | Spring Data Redis | ioredis (Nest) | Notes |
|---|---|---|---|
| `KEYS telco:*` | `redis.keys("telco:*")` | `redis.keys('telco:*')` | Dev only: blocks Redis. Use `SCAN` |
| `SCAN 0 MATCH telco:* COUNT 100` | `redis.scan(ScanOptions...)` | `redis.scan(0, 'MATCH', 'telco:*')` | Safe iteration |
| `EXISTS key` | `redis.hasKey(key)` | `redis.exists(key)` | |
| `DEL key` | `redis.delete(key)` | `redis.del(key)` | |
| `TYPE key` | `redis.type(key)` | `redis.type(key)` | string / hash / zset ... |
| `EXPIRE key 600` | `redis.expire(key, Duration.ofMinutes(10))` | `redis.expire(key, 600)` | |
| `TTL key` | `redis.getExpire(key)` | `redis.ttl(key)` | `-1` no TTL, `-2` no key |

## Strings: one value per key

| redis-cli | Spring Data Redis | ioredis (Nest) |
|---|---|---|
| `GET key` | `redis.opsForValue().get(key)` | `redis.get(key)` |
| `SET key value EX 600` | `redis.opsForValue().set(key, value, Duration.ofMinutes(10))` | `redis.set(key, value, 'EX', 600)` |
| `SET key value NX EX 600` | `redis.opsForValue().setIfAbsent(key, value, ttl)` | `redis.set(key, value, 'EX', 600, 'NX')` |
| `INCRBY key 5` | `redis.opsForValue().increment(key, 5)` | `redis.incrby(key, 5)` |

## Hashes: field → value under one key

| redis-cli | Spring Data Redis | ioredis (Nest) |
|---|---|---|
| `HSET key f1 v1 f2 v2` | `redis.opsForHash().putAll(key, map)` | `redis.hset(key, obj)` |
| `HSET key field value` | `redis.opsForHash().put(key, field, value)` | `redis.hset(key, field, value)` |
| `HGET key field` | `redis.opsForHash().get(key, field)` | `redis.hget(key, field)` |
| `HGETALL key` | `redis.opsForHash().entries(key)` | `redis.hgetall(key)` |
| `HINCRBY key field 10` | `redis.opsForHash().increment(key, field, 10)` | `redis.hincrby(key, field, 10)` |
| `HDEL key field` | `redis.opsForHash().delete(key, field)` | `redis.hdel(key, field)` |

## Sorted sets: unique members ranked by score

| redis-cli | Spring Data Redis | ioredis (Nest) |
|---|---|---|
| `ZINCRBY key 1 member` | `redis.opsForZSet().incrementScore(key, member, 1)` | `redis.zincrby(key, 1, member)` |
| `ZADD key 42 member` | `redis.opsForZSet().add(key, member, 42)` | `redis.zadd(key, 42, member)` |
| `ZREVRANGE key 0 9 WITHSCORES` | `redis.opsForZSet().reverseRangeWithScores(key, 0, 9)` | `redis.zrevrange(key, 0, 9, 'WITHSCORES')` |
| `ZSCORE key member` | `redis.opsForZSet().score(key, member)` | `redis.zscore(key, member)` |
| `ZREVRANK key member` | `redis.opsForZSet().reverseRank(key, member)` | `redis.zrevrank(key, member)` |

## Spring cache annotations

| Annotation | Does | Redis ops |
|---|---|---|
| `@EnableCaching` | turns the annotations on (in `CacheConfig`) | |
| `@Cacheable(cacheNames="books", key="#isbn")` | return the cached value; on a miss, run the method and store the result | `GET books::<isbn>`, on a miss `SET` |
| `@CacheEvict(cacheNames="books", key="#isbn")` | run the method, then delete the entry | `DEL books::<isbn>` |
| `@CachePut(cacheNames="books", key="#isbn")` | always run the method, store the **return value** | `SET books::<isbn>` |
| `RedisCacheConfiguration.entryTtl(...)` | TTL per cache name (annotations have no TTL) | |
