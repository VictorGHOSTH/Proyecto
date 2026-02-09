// Tables.kt - CON SOPORTE PARA TIMESTAMP
package com.example

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object PalabrasGuardadas : Table("palabras_guardadas") {
    val id = integer("id").autoIncrement()
    val palabra = varchar("palabra", 100)
    val fechaCreacion = timestamp("fecha_creacion")

    override val primaryKey = PrimaryKey(id)
}