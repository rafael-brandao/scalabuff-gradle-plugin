/*
 * Copyright (c) 2014 Rafael Brandão <rafa.bra@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.rafaelbrandao.gradle.scalabuff.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class ScalaBuffCleanTask extends DefaultTask {

  @TaskAction
  def cleanProto() {
    project.scalabuff.outputDir.deleteDir()
  }

}
