package com.kidstracker.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "bambini")
data class BambinoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val coloreIndex: Int,
    val ordine: Int
)

@Entity(
    tableName = "giornate",
    primaryKeys = ["bambinoId", "giorno"],
    foreignKeys = [
        ForeignKey(
            entity = BambinoEntity::class,
            parentColumns = ["id"],
            childColumns = ["bambinoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bambinoId"), Index("giorno")]
)
data class GiornataEntity(
    val bambinoId: Long,
    /** Data come epoch day, così si ordina e si filtra con un intero. */
    val giorno: Long,
    val presenza: String,
    val nanna: String?,
    val primo: String?,
    val secondo: String?,
    val dolce: String?,
    val entrata: String?,
    val uscita: String?,
    val salute: String,
    val nota: String,
    val aggiornatoIl: Long
)

@Dao
interface KidsDao {

    @Query("SELECT * FROM bambini ORDER BY ordine ASC, id ASC")
    fun osservaBambini(): Flow<List<BambinoEntity>>

    @Query("SELECT COUNT(*) FROM bambini")
    suspend fun quantiBambini(): Int

    @Insert
    suspend fun inserisciBambino(bambino: BambinoEntity): Long

    @Update
    suspend fun aggiornaBambino(bambino: BambinoEntity)

    @Query("DELETE FROM bambini WHERE id = :id")
    suspend fun eliminaBambino(id: Long)

    @Query("SELECT * FROM giornate WHERE giorno BETWEEN :da AND :a")
    fun osservaIntervallo(da: Long, a: Long): Flow<List<GiornataEntity>>

    @Query("SELECT * FROM giornate WHERE bambinoId = :bambinoId AND giorno = :giorno LIMIT 1")
    fun osservaGiornata(bambinoId: Long, giorno: Long): Flow<GiornataEntity?>

    @Query("SELECT * FROM giornate WHERE giorno = :giorno")
    fun osservaGiorno(giorno: Long): Flow<List<GiornataEntity>>

    @Query("SELECT * FROM giornate ORDER BY giorno ASC")
    suspend fun tutte(): List<GiornataEntity>

    @Upsert
    suspend fun salva(giornata: GiornataEntity)

    @Upsert
    suspend fun salvaTutte(giornate: List<GiornataEntity>)

    @Query("DELETE FROM giornate WHERE bambinoId = :bambinoId AND giorno = :giorno")
    suspend fun elimina(bambinoId: Long, giorno: Long)

    @Query("DELETE FROM giornate")
    suspend fun cancellaGiornate()
}

@Database(
    entities = [BambinoEntity::class, GiornataEntity::class],
    version = 1,
    exportSchema = false
)
abstract class KidsDatabase : RoomDatabase() {
    abstract fun dao(): KidsDao

    companion object {
        const val NOME = "kids.db"
    }
}
