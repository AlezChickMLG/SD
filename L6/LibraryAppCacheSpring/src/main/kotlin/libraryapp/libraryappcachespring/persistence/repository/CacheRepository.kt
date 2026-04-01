package libraryapp.libraryappcachespring.persistence.repository

import libraryapp.libraryappcachespring.business.models.Cache
import libraryapp.libraryappcachespring.persistence.interfaces.ICacheRepository
import libraryapp.libraryappcachespring.persistence.rowMapper.CacheRowMapper
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Repository
class CacheRepository (
    private val jdbcTemplate: JdbcTemplate,
    private val rowMapper: CacheRowMapper
) : ICacheRepository {
    override fun createTable() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS CACHE(" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "timestamp INTEGER," +
                        "query TEXT UNIQUE," +
                        "result TEXT)"
            )
        } catch (e: DataAccessException) {
            println("[createTable] Error: $e")
        }
    }

    override fun updateCache(cache: Cache) {
        try {
            jdbcTemplate.update(
                "UPDATE CACHE" +
                        " SET timestamp = ?, result = ? " +
                        " WHERE query = ?", cache.timestamp, cache.result, cache.query
            )
        } catch (e: DataAccessException) {
            println("[updateCache] Error: $e")
        }
    }

    override fun addCache(cache: Cache) {
        try {
            jdbcTemplate.update(
                "INSERT INTO CACHE(timestamp, query, result) VALUES (?, ?, ?)",
                cache.timestamp, cache.query, cache.result
            )
        } catch (e: DataAccessException) {
            println("[addCache] Error: $e")
        }
    }

    override fun deleteCache(cache: Cache) {
        try {
            jdbcTemplate.update("DELETE FROM CACHE WHERE query = ?", cache.query)
        } catch (e: DataAccessException) {
            println("[deleteCache] Error: $e")
        }
    }

    override fun getCacheByQuery(query: String): Cache? {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT * FROM CACHE " +
                        "WHERE query = ?", rowMapper, query
            )
        } catch (e: DataAccessException) {
            println("[${this::class::simpleName}] Error: $e")
        }

        return null
    }

    override fun getAllCaches(): List<Cache>? {
        try {
            return jdbcTemplate.query("SELECT * FROM CACHE", rowMapper)
        } catch (e: DataAccessException) {
            println("[${this::class::simpleName}] Error: $e")
        }

        return null
    }
}