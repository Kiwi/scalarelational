package org.scalarelational.instruction.ddl

import org.scalarelational.column.ColumnPropertyContainer
import org.scalarelational.column.property.ColumnProperty
import org.scalarelational.datatype.DataType

/**
 * @author Matt Hicks <matt@outr.com>
 */
case class CreateColumn[T, S](tableName: String, name: String, dataType: DataType[T, S], props: Seq[ColumnProperty])
                          (implicit manifest: Manifest[T]) extends ColumnPropertyContainer {
  override def classType = manifest.runtimeClass

  this.props(props: _*)
}