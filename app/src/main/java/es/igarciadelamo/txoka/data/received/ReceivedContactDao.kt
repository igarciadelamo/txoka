package es.igarciadelamo.txoka.data.received

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceivedContactDao {
    @Query("SELECT * FROM received_contacts ORDER BY receivedAt DESC")
    fun observeAll(): Flow<List<ReceivedContactEntity>>

    @Query("SELECT * FROM received_contacts WHERE id = :id")
    fun observe(id: Long): Flow<ReceivedContactEntity?>

    @Query(
        """
        SELECT * FROM received_contacts
        WHERE (:phoneKey <> '' AND phoneKey = :phoneKey)
           OR (:emailKey <> '' AND emailKey = :emailKey)
           OR (:linkedinKey <> '' AND LOWER(TRIM(linkedin)) = :linkedinKey)
           OR (:instagramKey <> '' AND LOWER(TRIM(instagram)) = :instagramKey)
        ORDER BY receivedAt DESC
        LIMIT 1
        """,
    )
    suspend fun findMatch(
        phoneKey: String,
        emailKey: String,
        linkedinKey: String,
        instagramKey: String,
    ): ReceivedContactEntity?

    @Insert
    suspend fun insert(contact: ReceivedContactEntity): Long

    /** Replaces every column of the row, so callers keep the local note. */
    @Update
    suspend fun updateCard(card: ReceivedContactEntity)

    @Query("UPDATE received_contacts SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String)

    @Query("DELETE FROM received_contacts WHERE id = :id")
    suspend fun delete(id: Long)
}
