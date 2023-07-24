package com.shaphr.accessanotes.data.database

import java.io.Serializable
import java.util.Date
import androidx.room.*
import kotlinx.serialization.json.JsonNames
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import androidx.room.TypeConverter

@Entity(
    tableName = "Note",
)

data class NoteImage(
    @Embedded val note: Note,
    @Relation(
        parentColumn = "id",
        entityColumn = "note_id",
        entity = NoteItem::class
    )
    val items: List<NoteItem>
)
data class Note(
    @ColumnInfo(name = "title") val title: String = "",
    @ColumnInfo(name = "summarize_content") val content: String = "",
    @ColumnInfo(name = "transcript") val transcript: String = "",
    @ColumnInfo(name = "date") val date: LocalDate? = null,
    @ColumnInfo(name = "notifyAt") val notifyAt: Long = Date().time,
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
) : Serializable

@Entity(tableName = "note_items")
data class NoteItem(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    @ColumnInfo(name = "note_id") var noteId: Int,
    @ColumnInfo(name = "content") var content: String?,
    @ColumnInfo(name = "image_path") var imagePath: String?,
    @ColumnInfo(name = "order") var order: Int,
)
