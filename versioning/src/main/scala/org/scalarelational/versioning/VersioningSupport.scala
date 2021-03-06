package org.scalarelational.versioning

import org.powerscala.concurrent.Time
import org.scalarelational.extra.PersistentProperties

/**
 * @author Matt Hicks <matt@outr.com>
 */
trait VersioningSupport extends PersistentProperties {
  lazy val version = persistence.intProperty("databaseVersion")

  private var upgrades = Map.empty[Int, UpgradableVersion]

  def register(upgrade: UpgradableVersion) = synchronized {
    upgrades += upgrade.version -> upgrade
  }

  /**
   * Upgrades the database to the latest version iteratively. This utilizes the registered versions compared against the
   * current version in the database and iterates over all unapplied versions upgrading to the latest.
   */
  def upgrade() = synchronized {
    info("Checking for Database Upgrades...")

    session {
      val latestVersion = upgrades.keys.toList match {
        case Nil => 0
        case keys => keys.max
      }

      if (version() == 0 && persistence.get("databaseVersion").isEmpty) {
        info(s"New database created. Setting version to latest (version $latestVersion) without running upgrades.")
        version := latestVersion // New database created, we don't have to run upgrades
        if (latestVersion == 0) {
          persistence("databaseVersion") = "0"    // Make sure the value is created in the database
        }
      } else {
        info(s"Current Version: ${version()}, Latest Version: ${latestVersion}")
        (version() until latestVersion).foreach {
          case v => transaction {
            val nextVersion = v + 1
            info(s"Upgrading from version $v to $nextVersion...")
            val upgrade = upgrades.getOrElse(nextVersion, throw new RuntimeException(s"No version registered for $nextVersion."))
            val elapsed = Time.elapsed {
              upgrade.upgrade()
            }
            version := nextVersion
            info(s"$nextVersion upgrade finished successfully in $elapsed seconds.")
          }
        }
      }
    }

    upgrades = Map.empty      // Clear the map so upgrades can be released
  }
}