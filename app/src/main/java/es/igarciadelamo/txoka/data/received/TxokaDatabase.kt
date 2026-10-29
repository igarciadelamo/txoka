package es.igarciadelamo.txoka.data.received

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ReceivedContactEntity::class], version = 1, exportSchema = false)
abstract class TxokaDatabase : RoomDatabase() {
    abstract fun receivedContactDao(): ReceivedContactDao
}
