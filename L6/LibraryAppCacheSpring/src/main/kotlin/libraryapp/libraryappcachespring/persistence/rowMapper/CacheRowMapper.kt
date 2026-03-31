package libraryapp.libraryappcachespring.persistence.rowMapper

import libraryapp.libraryappcachespring.business.models.Cache

import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component
import java.sql.ResultSet

@Component
class CacheRowMapper : RowMapper<Cache> {
    override fun mapRow(
        rs: ResultSet,
        rowNum: Int
    ): Cache {
        return Cache(
            rs.getInt("timestamp"),
            rs.getString("query"),
            rs.getString("result")
        )
    }
}