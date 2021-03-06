package org.scalarelational.extra

import org.powerscala.property.Property

import org.scalarelational.column.property.{PrimaryKey, Unique, AutoIncrement}
import org.scalarelational.model.Datastore
import org.scalarelational.table.Table

/**
 * Persistent Properties allows key/value pairs to be persisted to a table for later retrieval and modification.
 *
 * Convenience methods are provided to get, set, and remove the value for a property and to create a Property instance
 * that can update the database in an event-driven manner.
 *
 * @author Matt Hicks <matt@outr.com>
 */
trait PersistentProperties extends Datastore {
  object persistentProperties extends Table("PERSISTENT_PROPERTIES") {
    val id = column[Int]("id", PrimaryKey, AutoIncrement)
    val key = column[String]("name", Unique)
    val value = column[String]("value")
  }

  object persistence {
    def get(name: String): Option[String] = {
      val query = select(persistentProperties.value) from persistentProperties where persistentProperties.key === name
      query.result.converted.headOption
    }

    def apply(name: String): String = {
      get(name).getOrElse(throw new NullPointerException(s"Unable to find $name in persistent properties table."))
    }

    def update(name: String, newValue: String) {
      val m = merge(
        persistentProperties.key,
        persistentProperties.key(name),
        persistentProperties.value(newValue))
      m.result
    }

    def remove(name: String) {
      val d = delete(persistentProperties) where(persistentProperties.key === name)
      d.result
    }

    def stringProperty(key: String, default: String = null): Property[String] = {
      val p = Property[String](default = Some(get(key).getOrElse(default)))
      p.change.on {
        case evt => if (evt.newValue != null) {
          this(key) = evt.newValue
        } else {
          remove(key)
        }
      }
      p
    }

    def intProperty(key: String, default: Int = 0): Property[Int] = {
      val p = Property[Int](default = Some(get(key).map(s => s.toInt).getOrElse(default)))
      p.change.on {
        case evt => this(key) = evt.newValue.toString
      }
      p
    }
  }
}