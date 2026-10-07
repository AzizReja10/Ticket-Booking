-- KEYS[1] = stock key        (event:{id}:stock)
-- KEYS[2] = booked users set (event:{id}:users)
-- ARGV[1] = userId

if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
    return -2   -- this user already has a seat
end

local stock = tonumber(redis.call('GET', KEYS[1]))
if stock == nil then
    return -3   -- event not initialised in Redis
end

if stock <= 0 then
    return -1   -- sold out
end

redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])
return stock - 1   -- remaining seats after this reservation