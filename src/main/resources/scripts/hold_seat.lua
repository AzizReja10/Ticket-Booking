-- KEYS[1] = stock, KEYS[2] = users set, KEYS[3] = holds zset
-- ARGV[1] = userId, ARGV[2] = ttl in milliseconds

if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
    return -2   -- user already holds / owns a seat
end

local stock = tonumber(redis.call('GET', KEYS[1]))
if stock == nil then
    return -3   -- event not initialised
end
if stock <= 0 then
    return -1   -- sold out
end

local t = redis.call('TIME')
local now = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)
local expiresAt = now + tonumber(ARGV[2])

redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])
redis.call('ZADD', KEYS[3], expiresAt, ARGV[1])
return expiresAt