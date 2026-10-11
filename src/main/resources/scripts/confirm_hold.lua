-- KEYS[1] = holds zset
-- ARGV[1] = userId

local score = redis.call('ZSCORE', KEYS[1], ARGV[1])
if not score then
    return -1   -- no hold (never held, or already reaped)
end

local t = redis.call('TIME')
local now = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)
if tonumber(score) <= now then
    return -2   -- hold expired (reaper will release the seat)
end

redis.call('ZREM', KEYS[1], ARGV[1])   -- hold converted; seat stays taken
return 1