package es.igarciadelamo.txoka.data.received

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Version 2 added the LinkedIn and Instagram handles. Both columns are nullable, so cards stored by
 * version 1 keep their data and simply have no handles.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE received_contacts ADD COLUMN linkedin TEXT")
        db.execSQL("ALTER TABLE received_contacts ADD COLUMN instagram TEXT")
    }
}
