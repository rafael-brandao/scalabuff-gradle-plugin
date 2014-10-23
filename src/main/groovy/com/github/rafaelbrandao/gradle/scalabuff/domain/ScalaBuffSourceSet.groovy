/*
 * Copyright (c) 2014 Rafael Brandão <rafa.bra@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.rafaelbrandao.gradle.scalabuff.domain

class ScalaBuffSourceSet {

  final String name
  String srcDir

  ScalaBuffSourceSet(String name) {
    this.name = name
  }

}
