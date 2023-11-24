package ca.unb.mobiledev.tennis_tune.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "racquets")
data class Racquet(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "head_size") val headSize: Double,
    @ColumnInfo(name = "string_mass_density") val stringMassDensity: Double
)