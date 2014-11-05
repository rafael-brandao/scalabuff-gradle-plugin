/**
 * Copyright (c) 2014 Rafael Brandão <rafa.bra@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.rafaelbrandao.gradle.scalabuff

import scala.reflect.ClassTag

import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.{Plugin, Project, Task}


package object specs {

  implicit class TestProjectExtensions(val project: Project) extends AnyVal {

    def evaluate() = project.asInstanceOf[ProjectInternal].evaluate()

    def isEvaluated: Boolean = project.getState.getExecuted

    def findConfiguration(name: String): Option[Configuration] =
      Option(project.getConfigurations.findByName(name))

    def findExtension(name: String): Option[Any] =
      Option(project.getExtensions.findByName(name))

    def findTask(name: String): Option[Task] =
      Option(project.getTasks.findByName(name))

    def findPlugin[T <: Plugin[_]](implicit ev: ClassTag[T]): Option[T] =
      Option(project.getPlugins.findPlugin(getClassOf[T]))

    def applyPlugin[T <: Plugin[_]](implicit ev: ClassTag[T]): T =
      project.getPlugins.apply(getClassOf[T])
  }

}
