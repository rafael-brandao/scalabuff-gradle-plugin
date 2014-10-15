/**
 * Copyright (c) 2014 Rafael Brandão <rafa.bra@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.rafaelbrandao.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware

import scala.language.implicitConversions

package object scalabuff {

  private[scalabuff] type ScalaBuffPluginExtensionAware = ScalaBuffPluginExtension with ExtensionAware

  private[scalabuff] implicit class ProjectExtensions(val project: Project) extends AnyVal {
    def scalabuff =
      project
        .getExtensions
        .getByType(classOf[ScalaBuffPluginExtension])
        .asInstanceOf[ScalaBuffPluginExtensionAware]
  }

}
