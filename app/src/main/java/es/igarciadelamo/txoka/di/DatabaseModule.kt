package es.igarciadelamo.txoka.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import es.igarciadelamo.txoka.data.received.ReceivedContactDao
import es.igarciadelamo.txoka.data.received.RoomReceivedContactRepository
import es.igarciadelamo.txoka.data.received.TxokaDatabase
import es.igarciadelamo.txoka.domain.received.ReceivedContactRepository
import javax.inject.Singleton

private const val DATABASE_NAME = "txoka.db"

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TxokaDatabase =
        Room.databaseBuilder(context, TxokaDatabase::class.java, DATABASE_NAME).build()

    @Provides
    fun provideReceivedContactDao(database: TxokaDatabase): ReceivedContactDao = database.receivedContactDao()

    @Provides
    @Singleton
    fun provideReceivedContactRepository(dao: ReceivedContactDao): ReceivedContactRepository =
        RoomReceivedContactRepository(dao)
}
