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
    @ColumnInfo(name = "string_mass_density") val stringMassDensity: Double,
    @ColumnInfo(name = "order") var order: Int = 0 // The display order in racquet list
) {
    companion object {
        fun defaultRacquet() = Racquet(
            id = 0,
            name = "Default Racquet",
            headSize = 100.0,
            stringMassDensity = 1.53,
            order = 0
        )
    }
}