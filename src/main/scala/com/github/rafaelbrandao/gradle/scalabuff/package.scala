/**
 * Copyright (c) 2014 Rafael Brandão <rafa.bra@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.rafaelbrandao.gradle

import scala.reflect.ClassTag

import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.HasConvention
import org.gradle.api.tasks.{ScalaSourceSet, SourceSetContainer, TaskContainer}
import org.gradle.api.{Action, Project, Task}
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.plugins.ide.idea.model.IdeaModel


package object scalabuff {

  type SourceSet = org.gradle.api.tasks.SourceSet with HasConvention

  def getClassOf[T](implicit ev: ClassTag[T]): Class[T] =
    implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]

  implicit class ProjectExtensions(val project: Project) extends AnyVal {
    def scalabuff =
      project
        .getExtensions
        .getByType(classOf[ScalabuffExtension])


    def configure(fns: (Project => Any)*): Unit = fns.foreach(_(project))

    def afterEvaluate(fn: => Any) = project.afterEvaluate {
      new Action[Project]() {
        override def execute(project: Project): Unit = fn
      }
    }

    def afterEvaluate(fns: (Project => Any)*): Unit = project.afterEvaluate {
      new Action[Project]() {
        override def execute(project: Project): Unit = fns.foreach(_(project))
      }
    }


    def sourceSets: SourceSetContainer =
      project.getProperties.get("sourceSets").asInstanceOf[SourceSetContainer]


    def tasks: TaskContainer = project.getTasks


    def idea: Option[IdeaModel] =
      Option(project.getPlugins.findPlugin(classOf[IdeaPlugin])).map(_.getModel)


    def getConfiguration(name: String) = project.getConfigurations.getByName(name)
  }

  implicit class SourceSetContainerExtensions(val sourceSets: SourceSetContainer) extends AnyVal {
    def main: SourceSet = sourceSets.getByName("main").asInstanceOf[SourceSet]
  }

  implicit class SourceSetExtensions(val sourceSet: SourceSet) extends AnyVal {
    def scala = sourceSet.getConvention.findPlugin(classOf[ScalaSourceSet]).getScala
  }

  implicit class DependencyExtensions(val dependency: Dependency) extends AnyVal {
    def notation = s"${dependency.getGroup}:${dependency.getName}:${dependency.getVersion}"
  }

  implicit class TaskContainerExtensions(val container: TaskContainer) extends AnyVal {
    def createTask[T <: Task](name: String)(fn: T => Any)(implicit ev: ClassTag[T]): T = {
      val task = container.create(name, getClassOf[T])
      fn(task)
      task
    }
  }
}
