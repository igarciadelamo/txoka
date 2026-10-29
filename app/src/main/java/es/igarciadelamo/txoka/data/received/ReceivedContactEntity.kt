package es.igarciadelamo.txoka.data.received

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "received_contacts")
data class ReceivedContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String?,
    val email: String?,
    val phoneKey: String,
    val emailKey: String,
    val note: String,
    val receivedAt: Long,
    val accentHex: String,
    val accent2Hex: String,
)
