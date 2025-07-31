package cache

import (
	"context"
	"encoding/json"
	"fmt"
	"go.uber.org/zap"
	"time"

	"github.com/go-redis/redis/v8"
)

type RedisCache struct {
	client *redis.Client
	ctx    context.Context
}

type CacheConfig struct {
	Host     string
	Port     string
	Username string
	Password string
	DB       int
}

func NewRedisCache(config CacheConfig, log *zap.SugaredLogger) *RedisCache {
	rdb := redis.NewClient(&redis.Options{
		Addr:     fmt.Sprintf("%s:%s", config.Host, config.Port),
		Password: config.Password,
		Username: config.Username,
		DB:       config.DB,
	})

	ctx := context.Background()

	_, err := rdb.Ping(ctx).Result()
	if err != nil {
		log.Fatalf("failed to connect to Redis: %v", err)
	}

	log.Infof("Redis connection established successfully")

	return &RedisCache{
		client: rdb,
		ctx:    ctx,
	}
}

func (r *RedisCache) Set(key string, value interface{}, expiration time.Duration) error {
	jsonData, err := json.Marshal(value)
	if err != nil {
		return fmt.Errorf("failed to marshal data: %w", err)
	}

	return r.client.Set(r.ctx, key, jsonData, expiration).Err()
}

func (r *RedisCache) Get(key string, dest interface{}) error {
	val, err := r.client.Get(r.ctx, key).Result()
	if err != nil {
		return err
	}

	return json.Unmarshal([]byte(val), dest)
}

func (r *RedisCache) Delete(key string) error {
	return r.client.Del(r.ctx, key).Err()
}

func (r *RedisCache) DeletePattern(pattern string) error {
	keys, err := r.client.Keys(r.ctx, pattern).Result()
	if err != nil {
		return err
	}

	if len(keys) > 0 {
		return r.client.Del(r.ctx, keys...).Err()
	}

	return nil
}

func (r *RedisCache) Exists(key string) bool {
	result, _ := r.client.Exists(r.ctx, key).Result()
	return result > 0
}

func (r *RedisCache) Invalidate(key string) error {
	result, _ := r.client.Exists(r.ctx, key).Result()
	if result > 0 {
		return r.Delete(key)
	}

	return nil
}

func (r *RedisCache) Close() error {
	return r.client.Close()
}
