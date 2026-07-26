package com.gerar.controlelectrico.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val name: String,
    val internalMeter: String,
    val isActive: Boolean,
    val isResidual: Boolean,
    val periodStatesJson: String,
    val notes: String
)

@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey val period: String,
    val externalReadingDate: String,
    val supplyNumber: String,
    val externalKwh: Double,
    val monthlyBill: Double,
    val priceKwhUpTo30: Double,
    val priceKwhOver30: Double,
    val fixedCharge: Double,
    val maintenance: Double,
    val publicLighting: Double,
    val ruralElectrification: Double,
    val notes: String
)

@Entity(tableName = "readings")
data class ReadingEntity(
    @PrimaryKey val id: String,
    val period: String,
    val userId: String,
    val isResidual: Boolean,
    val internalReadingDate: String,
    val previousReading: Double?,
    val currentReading: Double?,
    val notes: String
)

@Entity(tableName = "service_expenses")
data class ServiceEntity(
    @PrimaryKey val id: String,
    val period: String,
    val name: String,
    val amount: Double,
    val isActive: Boolean,
    val splitCost: Boolean,
    val participantCount: Int,
    val participantUserIdsJson: String,
    val notes: String
)

@Entity(
    tableName = "user_payments",
    indices = [androidx.room.Index(value = ["period", "userId"], unique = true)]
)
data class PaymentEntity(
    @PrimaryKey val id: String,
    val period: String,
    val userId: String,
    val status: String,
    val amountPaid: Double,
    val paymentDate: String,
    val notes: String
)

@Dao
interface ElectricDao {
    @Query("SELECT * FROM users ORDER BY userId")
    fun getUsers(): List<UserEntity>

    @Query("SELECT * FROM receipts ORDER BY period DESC")
    fun getReceipts(): List<ReceiptEntity>

    @Query("SELECT * FROM readings ORDER BY period DESC, userId")
    fun getReadings(): List<ReadingEntity>

    @Query("SELECT * FROM service_expenses ORDER BY period DESC, name")
    fun getServices(): List<ServiceEntity>

    @Query("SELECT * FROM user_payments ORDER BY period DESC, userId")
    fun getPayments(): List<PaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertReceipt(receipt: ReceiptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertReading(reading: ReadingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertService(service: ServiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertPayment(payment: PaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertUsers(users: List<UserEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertReceipts(receipts: List<ReceiptEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertReadings(readings: List<ReadingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertServices(services: List<ServiceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertPayments(payments: List<PaymentEntity>)

    @Query("DELETE FROM users WHERE userId = :userId")
    fun deleteUser(userId: String)

    @Query("DELETE FROM receipts WHERE period = :period")
    fun deleteReceipt(period: String)

    @Query("DELETE FROM readings WHERE id = :id")
    fun deleteReading(id: String)

    @Query("DELETE FROM service_expenses WHERE id = :id")
    fun deleteService(id: String)

    @Query("DELETE FROM user_payments WHERE id = :id")
    fun deletePayment(id: String)

    @Query("DELETE FROM users")
    fun clearUsers()

    @Query("DELETE FROM receipts")
    fun clearReceipts()

    @Query("DELETE FROM readings")
    fun clearReadings()

    @Query("DELETE FROM service_expenses")
    fun clearServices()

    @Query("DELETE FROM user_payments")
    fun clearPayments()
}

@Database(
    entities = [
        UserEntity::class,
        ReceiptEntity::class,
        ReadingEntity::class,
        ServiceEntity::class,
        PaymentEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ControlElectricoDatabase : RoomDatabase() {
    abstract fun electricDao(): ElectricDao

    companion object {
        @Volatile
        private var instance: ControlElectricoDatabase? = null

        fun get(context: Context): ControlElectricoDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ControlElectricoDatabase::class.java,
                    "control_electrico.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .allowMainThreadQueries()
                    .build()
                    .also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `user_payments` (
                        `id` TEXT NOT NULL,
                        `period` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `amountPaid` REAL NOT NULL,
                        `paymentDate` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_user_payments_period_userId` " +
                        "ON `user_payments` (`period`, `userId`)"
                )
            }
        }
    }
}
