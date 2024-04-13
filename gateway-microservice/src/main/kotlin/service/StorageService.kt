package service

import io.quarkus.redis.datasource.ReactiveRedisDataSource
import io.quarkus.redis.datasource.RedisDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

@ApplicationScoped
class StorageService {

    @Inject
    private lateinit var ds : RedisDataSource

    fun getGroups() : Set<String> {
        return ds.set(String::class.java).smembers("groups")
    }

    fun addGroup(id : String) {
        ds.set(String::class.java).sadd("groups", id)
    }

    fun groupExists(id : String) : Boolean {
        return ds.set(String::class.java).sismember("groups", id)
    }

    fun addToSet(key : String, set : Set<String>) {
        ds.set(String::class.java).sadd(key, *set.toTypedArray())
    }

    fun removeFromSet(key : String, set : Set<String>) {
        ds.set(String::class.java).srem(key, *set.toTypedArray())
    }

    fun getSet(key : String) : Set<String> {
        return ds.set(String::class.java).smembers(key)
    }

    fun keyExists(key : String) : Boolean {
        return ds.key().exists(key)
    }

    fun setString(key : String, value : String, expireSeconds : Long) {
        ds.string(String::class.java).setex(key, expireSeconds, value)
    }

    fun getString(key : String) : String? {
        return ds.string(String::class.java).get(key)
    }

    fun deleteKey(key : String) {
        ds.key().del(key)
    }

    fun deleteKeys(pattern : String) {
        val keys = ds.key().keys(pattern).toTypedArray()
        if (keys.isNotEmpty())
            ds.key().del(*keys)
    }
}