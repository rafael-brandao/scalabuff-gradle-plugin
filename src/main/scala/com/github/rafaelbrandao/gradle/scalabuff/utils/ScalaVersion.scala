/**
 * Copyright (c) 2014 Rafael Brandão <rafa.bra@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.rafaelbrandao.gradle.scalabuff.utils

import org.gradle.api.InvalidUserDataException


trait ScalaVersion {

  def scalaBaseVersion(version: String): Option[String] = {
    for {
      version <- SemanticVersion.of(version)
      major <- version.major
      minor <- version.minor
    } yield s"$major.$minor"
  }.orElse(
      throw new InvalidUserDataException(s"'$version' is not a valid version string.")
    )
}
