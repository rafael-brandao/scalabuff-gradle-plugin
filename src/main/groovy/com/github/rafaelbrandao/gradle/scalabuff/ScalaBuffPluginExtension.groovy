/*
 * Copyright (c) 2014 Rafael Brandão <rafa.bra@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.rafaelbrandao.gradle.scalabuff

import com.github.rafaelbrandao.gradle.scalabuff.domain.ScalaBuffSourceSet
import org.gradle.api.NamedDomainObjectContainer

class ScalaBuffPluginExtension {

  static final String NAME = 'scalabuff'

  File outputDir;
  File generatedSourcesDir;
  File generatedResourcesDir;

  // If set to true, remove all sourceSets but the generated scalabuff one.
  boolean generateDescriptor

  final NamedDomainObjectContainer<ScalaBuffSourceSet> sourceSets

  ScalaBuffPluginExtension(NamedDomainObjectContainer<ScalaBuffSourceSet> sourceSets) {
    this.sourceSets = sourceSets
  }

  def sourceSets(Closure closure) {
    sourceSets.configure(closure)
  }
}
