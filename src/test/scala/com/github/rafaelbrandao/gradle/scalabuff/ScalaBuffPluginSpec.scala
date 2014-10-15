/**
 * Copyright (c) 2014 Rafael Brandão <rafa.bra@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.rafaelbrandao.gradle.scalabuff

import com.github.rafaelbrandao.gradle.scalabuff.TestConfig.SCALA_BASE_VERSION
import com.github.rafaelbrandao.gradle.scalabuff.tasks.{ScalaBuffCleanTask, ScalaBuffCompileTask}
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}

import scala.collection.JavaConversions.asScalaSet

@RunWith(classOf[JUnitRunner])
class ScalaBuffPluginSpec extends WordSpec with Matchers {

  def buildProject(): Project = {
    val project = ProjectBuilder.builder().build()
    project.getPlugins.apply(s"com.github.rafael-brandao.scalabuff_$SCALA_BASE_VERSION")
    project
  }

  "Any given gradle project" which {
    "is set with ScalaBuff Plugin" should {
      val project = buildProject()

      "have a 'compileProto' task" in {
        project.getTasks.getByName("compileProto") shouldBe a[ScalaBuffCompileTask]
      }

      "have a 'cleanProto' task" in {
        project.getTasks.getByName("cleanProto") shouldBe a[ScalaBuffCleanTask]
      }

      "have ScalaBuffPluginExtension as an extension" in {
        val extension = project.scalabuff
        extension should not be null
        extension shouldBe a[ScalaBuffPluginExtension]
      }

      "have a compile dependency on 'protobuf-java' and on 'scalabuff-runtime' libraries" in {
        val compileDependencies = project.getConfigurations.getByName("compile").getDependencies
        compileDependencies.find(_.getName.startsWith("protobuf-java")) should be('defined)
        compileDependencies.find(_.getName.startsWith("scalabuff-runtime")) should be('defined)
      }

      "have the correct environment configured" in {
        val protoc = new ProcessBuilder("protoc", "--version").start()
        protoc.waitFor()
        protoc.exitValue() should be(0)
      }
    }
  }

}
