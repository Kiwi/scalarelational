package org.scalarelational.h2.trigger

import org.powerscala.enum.{EnumEntry, Enumerated}

/**
 * @author Matt Hicks <matt@outr.com>
 */
sealed abstract class TriggerState extends EnumEntry

object TriggerState extends Enumerated[TriggerState] {
  case object Before extends TriggerState
  case object After extends TriggerState

  val values = findValues.toVector
}