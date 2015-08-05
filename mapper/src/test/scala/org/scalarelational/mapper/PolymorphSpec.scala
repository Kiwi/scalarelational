package org.scalarelational.mapper

import java.sql.{JDBCType, SQLType}

import org.scalatest.{Matchers, WordSpec}

import org.scalarelational.column.{ColumnLike, ColumnPropertyContainer}
import org.scalarelational.datatype.DataType
import org.scalarelational.h2.{H2Memory, H2Datastore}
import org.scalarelational.column.property.{Polymorphic, PrimaryKey, AutoIncrement}
import org.scalarelational.model.Datastore

/**
 * @author Tim Nieradzik <tim@kognit.io>
 */
class PolymorphSpec extends WordSpec with Matchers {
  import PolymorphDatastore._

  val insertUsers = Seq(
    UserGuest("guest"),
    UserAdmin("admin", true)
  )

  val insertContent = Seq(
    ContentString("hello"),
    ContentList(List("a", "b", "c"))
  )

  "Users" should {
    "create tables" in {
      session {
        create(users)
      }
    }
    "insert users" in {
      session {
        insertUsers.zipWithIndex.foreach {
          case (usr, index) => {
            val result = users.insert(usr).result
            result.id should equal (index + 1)
          }
        }
      }
    }
    "query users" in {
      session {
        val query = users.q from users
        val x = query.asCase[User] { row =>
          if (row(users.isGuest)) classOf[UserGuest]
          else classOf[UserAdmin]
        }
        insertUsers should equal (x.result.converted.toList.map(_.withoutId))
      }
    }
  }

  "Content" should {
    "create tables" in {
      session {
        create(content)
      }
    }
    "insert content" in {
      session {
        insertContent.zipWithIndex.foreach {
          case (c, index) => {
            val result = content.insert(c).result
            result.id should equal (index + 1)
          }
        }
      }
    }
    "query content" in {
      session {
        val query = content.q from content
        val x = query.asCase[Content] { row =>
          if (row(content.isString)) classOf[ContentString]
          else classOf[ContentList]
        }
        insertContent should equal (x.result.converted.toList.map(_.withoutId))
      }
    }
  }
}

trait User {
  def name: String
  def id: Option[Int]
  def withoutId: User
}

case class UserGuest(name: String, id: Option[Int] = None) extends User {
  val isGuest = true
  def withoutId = copy(id = None)
}

case class UserAdmin(name: String, canDelete: Boolean, id: Option[Int] = None) extends User {
  val isGuest = false
  def withoutId = copy(id = None)
}

// ---

trait Content {
  def id: Option[Int]
  def withoutId: Content
}

case class ContentString(string: String, id: Option[Int] = None) extends Content {
  val isString = true
  def withoutId = copy(id = None)
}

case class ContentList(entries: List[String], id: Option[Int] = None) extends Content {
  val isString = false
  def withoutId = copy(id = None)
}

// ---

object PolymorphDatastore extends H2Datastore(mode = H2Memory("polymorph_test")) {
  object users extends MappedTable[User]("users") {
    val id = column[Option[Int]]("id", PrimaryKey, AutoIncrement)
    val name = column[String]("name")
    val canDelete = column[Boolean]("canDelete", Polymorphic)
    val isGuest = column[Boolean]("isGuest")
  }

  object content extends MappedTable[Content]("content") {
    implicit val listStringConverter = new DataType[List[String]] {
      override def jdbcType = JDBCType.VARCHAR
      def sqlType(datastore: Datastore, properties: ColumnPropertyContainer) = "VARCHAR(1024)"
      def toSQLType(column: ColumnLike[_], value: List[String]) = value.mkString("|")
      def fromSQLType(column: ColumnLike[_], value: Any) =
        value.asInstanceOf[String].split('|').toList
    }

    val id = column[Option[Int]]("id", PrimaryKey, AutoIncrement)
    val string = column[String]("string", Polymorphic)
    val entries = column[List[String]]("entries", Polymorphic)
    val isString = column[Boolean]("isString")
  }
}
