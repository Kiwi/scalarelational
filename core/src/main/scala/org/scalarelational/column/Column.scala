package org.scalarelational.column

import org.scalarelational.datatype.DataType
import org.scalarelational.column.property.ColumnProperty
import org.scalarelational.table.Table

/**
 * @author Matt Hicks <matt@outr.com>
 */
private[scalarelational] class Column[T](val name: String,
                                         val dataType: DataType[T],
                                         val manifest: Manifest[T],
                                         val table: Table,
                                         val props: Seq[ColumnProperty]
                                        ) extends ColumnLike[T] {
  table.addColumn(this)     // Add this column to the table
  this.props(props: _*)

  lazy val classType = manifest.runtimeClass
  lazy val longName = s"${table.tableName}.$name"
  lazy val index = table.columns.indexOf(this)
  lazy val fieldName = table.fieldName(this)

  def as(alias: String) = ColumnAlias[T](this, None, None, Option(alias))
  def opt: ColumnLike[Option[T]] = ColumnOption(this)

  override def toString = name
}