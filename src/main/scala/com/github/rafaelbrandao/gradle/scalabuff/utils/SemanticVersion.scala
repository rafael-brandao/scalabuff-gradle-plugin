/**
 * Copyright (c) 2014 Rafael Brandão <rafa.bra@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.rafaelbrandao.gradle.scalabuff.utils


case class SemanticVersion(major: Option[String],
                           minor: Option[String] = None,
                           patch: Option[String] = None,
                           revision: Option[String] = None,
                           private val revisionSeparator: Option[String] = None) {

  def print(revisionSeparator: String = this.revisionSeparator.getOrElse("-")): String =
    Seq(major, minor, patch).flatten.mkString("", ".", {
      revision.map(r => s"$revisionSeparator$r").getOrElse("")
    })
}


object SemanticVersion {

  def of(version: String) = pattern.findFirstMatchIn(version).map { m =>
    import m._
    SemanticVersion(
      major = Option(group(2)),
      minor = Option(group(8)),
      patch = Option(group(14)),
      revision = Array(group(5), group(7), group(11), group(13), group(17)).find(_ != null),
      revisionSeparator = Array(group(4), group(6), group(10), group(12), group(16)).find(_ != null)
    )
  }

  private[this] def pattern =
    "^([^\\.]+ )?(\\d+)((-)([^ ]+)|(\\.)([^\\d^ ].*)|\\.(\\d+)((-)([^ ]+)|(\\.)([^\\d^ ].*)|\\.(\\d+)(([-\\.])([^ ]+))?)?)?( .*)?$".r
}
