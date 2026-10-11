-- KEYS[1] = stock, KEYS[2] = users set, KEYS[3] = holds zset

local t = redis.call('TIME')
local now = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)

local expired = redis.call('ZRANGEBYSCORE', KEYS[3], '-inf', now)
for _, user in ipairs(expired) do
    redis.call('ZREM', KEYS[3], user)
    redis.call('SREM', KEYS[2], user)
    redis.call('INCR', KEYS[1])
end
return #expired