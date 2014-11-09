/**
 * Copyright (c) 2014 Rafael Brandão <rafa.bra@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.rafaelbrandao.gradle.scalabuff

import groovy.lang.Closure

import scala.beans.BeanProperty

import org.gradle.api.{NamedDomainObjectContainer, Project}

import com.github.rafaelbrandao.gradle.scalabuff.ScalabuffExtension._
import com.github.rafaelbrandao.gradle.scalabuff.domain.ScalabuffSourceSet


class ScalabuffExtension(project: Project) {

  @BeanProperty val sourceSets = project.container(classOf[ScalabuffSourceSet])
  @BeanProperty val outputDir = project.file(s"${project.getBuildDir}/${ScalabuffExtension.NAME}")
  @BeanProperty val generatedSourcesDir = project.file(s"$outputDir/generated-sources")
  @BeanProperty val generatedResourcesDir = project.file(s"$outputDir/generated-resources")

  @BeanProperty var protocPath = "protoc"
  @BeanProperty var failIfProtocNotDetected = false
  @BeanProperty var generateDescriptors = true

  // Groovy helper methods
  def sourceSets(closure: Closure[_]): NamedDomainObjectContainer[ScalabuffSourceSet] =
    sourceSets.configure(closure)

  // Apply defaults
  sourceSets
    .create(DEFAULT_SOURCE_SET_NAME)
    .srcDir = s"src/$DEFAULT_SOURCE_SET_NAME"
}

object ScalabuffExtension {
  final def NAME = "scalabuff"
  final def DEFAULT_SOURCE_SET_NAME = "proto"
}
