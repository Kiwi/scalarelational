package org.scalarelational.instruction.ddl

import org.scalarelational.CallableInstruction
import org.scalarelational.model.{Column, Table}

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait DDLSupport {
  def table2Create(table: Table, ifNotExists: Boolean = true): CreateTable
  def column2Create[T](column: Column[T]): CreateColumn[T]

  def ddl(tables: List[Table], ifNotExists: Boolean = true): List[CallableInstruction]

  def ddl(create: CreateTable): List[CallableInstruction]

  def ddl[T](create: CreateColumn[T]): List[CallableInstruction]

  def ddl(alter: CreateForeignKey): List[CallableInstruction]

  def ddl(create: CreateIndex): List[CallableInstruction]
}