package org.scalarelational.existing

import java.sql.ResultSet

import org.powerscala.reflect._
import org.scalarelational.datatype.{DataTypes, TypedValue}
import org.scalarelational.model.Datastore

import scala.annotation.tailrec

/**
 * ExistingQuery allows mapping of an existing query to a result result of type R.
 *
 * @author Matt Hicks <matt@outr.com>
 */
class ExistingQuery[R](datastore: Datastore, queryString: String)(implicit manifest: Manifest[R]) {
  private val caseClass: EnhancedClass = manifest.runtimeClass
  if (!caseClass.isCase) throw new RuntimeException(s"$caseClass is not a case class!")
  private val caseValues = caseClass.caseValues.map(cv => cv.name.toLowerCase -> cv).toMap

  def query(args: List[TypedValue[_, _]]) = {
    val namedArgs = args.collect {
      case arg if arg.isInstanceOf[NamedArgument] => arg.asInstanceOf[NamedArgument]
    }
    val query = applyNamed(queryString, namedArgs)
    val results = datastore.session.executeQuery(query, args.filterNot(a => a.isInstanceOf[NamedArgument]))
    new Iterator[R] {
      def hasNext = results.next()
      def next() = result2R(results)
    }.toStream
  }

  private def result2R(result: ResultSet): R = {
    val meta = result.getMetaData
    val args = (0 until meta.getColumnCount).flatMap {
      case index => {
        val name = meta.getColumnLabel(index + 1).toLowerCase
        caseValues.get(name).map(cv => cv.name -> result.getObject(index + 1))
      }
    }.toMap
    caseClass.create(args)
  }

  @tailrec
  private def applyNamed(q: String, args: List[NamedArgument]): String = if (args.isEmpty) {
    q
  } else {
    val arg = args.head
    val modified = q.replaceAll(s":${arg.key}", arg.value)
    applyNamed(modified, args.tail)
  }
}

class NamedArgument(val key: String, value: String) extends TypedValue[String, String](DataTypes.StringType, value) {
  override def toString = s"NamedArgument($key = $value)"
}