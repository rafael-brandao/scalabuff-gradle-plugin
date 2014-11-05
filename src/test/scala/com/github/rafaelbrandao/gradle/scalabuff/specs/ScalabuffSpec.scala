/**
 * Copyright (c) 2014 Rafael Brandão <rafa.bra@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.rafaelbrandao.gradle.scalabuff.specs

import scala.collection.JavaConverters._

import com.github.rafaelbrandao.gradle.scalabuff._
import com.github.rafaelbrandao.gradle.scalabuff.specs.ScalabuffSpec._

import org.gradle.api._
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.testfixtures.ProjectBuilder

import org.codehaus.groovy.runtime.MethodClosure
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner


@RunWith(classOf[JUnitRunner])
class ScalabuffSpec extends WordSpec with GivenWhenThen with Matchers with Inside {

  "Any gradle project" which {
    "only applies the ScalabuffPlugin" should {

      trait fixture {
        val project = buildProject(applyScalabuffPlugin)
      }

      "have ScalabuffPlugin defined" in new fixture {
        project.findPlugin[ScalabuffPlugin] shouldBe 'defined
        project.isEvaluated shouldBe false
      }

      "have ScalaPlugin defined" in new fixture {
        project.findPlugin[ScalaPlugin] shouldBe 'defined
        project.isEvaluated shouldBe false
      }

      "have ScalabuffPluginExtension defined" in new fixture {
        project.findExtension(ScalabuffExtension.NAME) shouldBe 'defined
        project.isEvaluated shouldBe false
      }

      "have configured the scala classpath correctly" in new fixture {
        val srcSets = project.sourceSets.main.scala.getSrcDirs
        srcSets.size() shouldEqual 2
        srcSets.contains(project.scalabuff.generatedSourcesDir) shouldBe true
        project.isEvaluated shouldBe false
      }

      s"have configured the '${ScalabuffTasks.ScalabuffCompile.NAME}' task" in new fixture {
        inside(project.findTask(ScalabuffTasks.ScalabuffCompile.NAME)) {

          case Some(task) => task shouldBe a[ScalabuffTasks.ScalabuffCompile]

          case None => fail(s"Project '${project.getName}' should have defined a " +
            s"'${ScalabuffTasks.ScalabuffCompile.NAME}' task")
        }
        project.isEvaluated shouldBe false
      }

      s"have configured the '${ScalabuffTasks.GenerateProtoDescriptors.NAME}' task" in new fixture {

        inside(project.findTask(ScalabuffTasks.GenerateProtoDescriptors.NAME)) {

          case Some(task) => task shouldBe a[ScalabuffTasks.GenerateProtoDescriptors]

          case None => fail(s"Project '${project.getName}' should have defined a " +
            s"'${ScalabuffTasks.GenerateProtoDescriptors.NAME}' task")
        }
        project.isEvaluated shouldBe false
      }

      "fail when it is evaluated" in new fixture {
        When("project is evaluated")
        Then("a ProjectConfigurationException should be thrown")
        val exception = intercept[ProjectConfigurationException] {
          project.evaluate()
        }
        project.isEvaluated shouldBe true

        And("it should have another ProjectConfigurationException as cause")
        val cause = exception.getCause
        cause should not be null
        cause shouldBe a[ProjectConfigurationException]

        And("this cause exception should include a message indicating that the project didn't add a 'compile' dependency to 'scala-library'")
        cause.getMessage should include(s"Project ${project.getName} must add a 'compile' dependency to 'scala-library'.")
      }
    }
  }


  "Any gradle project" which {
    "applies both ScalabuffPlugin and IdeaPlugin" should {

      trait fixture {
        val project = buildProject(
          applyScalabuffPlugin,
          applyIdeaPlugin
        )
      }

      "have ScalabuffPlugin defined" in new fixture {
        project.findPlugin[ScalabuffPlugin] shouldBe 'defined
        project.isEvaluated shouldBe false
      }

      "have IdeaPlugin defined" in new fixture {
        project.findPlugin[IdeaPlugin] shouldBe 'defined
        project.idea shouldBe 'defined
        project.isEvaluated shouldBe false
      }

      "have configured idea classpath even before evaluation" in new fixture {
        inside(project.idea) {
          case Some(ideaModel) =>
            val srcDirs = ideaModel.getModule.getSourceDirs
            print(srcDirs)
            srcDirs.contains(project.scalabuff.generatedSourcesDir) shouldBe true
        }
        project.isEvaluated shouldBe false
      }
    }
  }


  "Any gradle project" which {
    "applies ScalabuffPlugin and " +
      "declares a 'compile' dependency to 'scala-library' with an invalid version" should {

      trait fixture {
        val version = "2.INVALID.10.VERSION"
        val project = buildProject(
          applyScalabuffPlugin,
          declareCompileDependencyToScalaLibrary(version)
        )
      }

      "fail when it is evaluated" in new fixture {
        When("project is evaluated")
        Then("a ProjectConfigurationException should be thrown")
        val exception = intercept[ProjectConfigurationException] {
          project.evaluate()
        }
        project.isEvaluated shouldBe true

        And("it should have an InvalidUserDataException as cause")
        val cause = exception.getCause
        cause should not be null
        cause shouldBe an[InvalidUserDataException]

        And("this cause exception should include a message indicating that the provided scala version is not valid")
        cause.getMessage should include(s"'$version' is not a valid version string.")
      }
    }
  }


  "Any gradle project" which {
    "applies ScalabuffPlugin and " +
      "declares a 'compile' dependency to 'scala-library' with a valid version" should {

      val compileProtoConfigName = ScalabuffTasks.ScalabuffCompile.CONFIGURATION_NAME

      trait fixture {
        val scalaVersion = "2.10.3"
        val scalaBase = "2.10"
        val project = buildProject(
          applyScalabuffPlugin,
          declareCompileDependencyToScalaLibrary(scalaVersion)
        )
      }

      "not fail when it is evaluated" in new fixture {
        try {
          project.evaluate()
        } catch {
          case e: GradleException =>
            fail(s"Project '${project.getName}' should not fail on evaluation.", e)
        }
        project.isEvaluated shouldBe true
      }

      s"have created a '$compileProtoConfigName' configuration only when it is evaluated" in new fixture {
        project.findConfiguration(compileProtoConfigName) should not be 'defined
        project.evaluate()
        project.isEvaluated shouldBe true
        project.findConfiguration(compileProtoConfigName) shouldBe 'defined
      }

      s"declare a '$compileProtoConfigName' dependency to 'scalabuff-compiler' library when it is evaluated" in new fixture {
        project.evaluate()
        project.isEvaluated shouldBe true

        val dependencies = for {
          configuration <- project.findConfiguration(compileProtoConfigName).toSeq
          dependency <- configuration.getDependencies.asScala
          if dependency.getName == s"scalabuff-compiler_$scalaBase"
        } yield dependency

        dependencies.size shouldEqual 1
        dependencies.head.getName should include("scalabuff-compiler_")
      }

      "declare a 'compile' dependency to 'scalabuff-runtime' library when it is evaluated" in new fixture {
        project.evaluate()
        project.isEvaluated shouldBe true

        val dependencies = for {
          configuration <- project.findConfiguration("compile").toSeq
          dependency <- configuration.getDependencies.asScala
          if dependency.getName.startsWith("scalabuff-runtime_")
        } yield dependency

        dependencies.size shouldEqual 1
        dependencies.head.getName should include("scalabuff-runtime_")
      }
    }
  }


  "The following project" should {

    trait fixture {
      val project = buildProject(
        applyScalabuffPlugin,
        declareCompileDependencyToScalaLibrary("2.10.3"),
        declareJCenterRepo,
        forbidAnyDependencyResolution
      )
    }

    "not raise a ForbiddenResolutionException when evaluated" in new fixture {
      project.isEvaluated shouldBe false
      try {
        project.evaluate()
      } catch {
        case e: ForbiddenResolutionException => fail(e.getMessage, e)
      }
      project.isEvaluated shouldBe true
    }

    "raise a ForbiddenResolutionException when any configuration is resolved" in new fixture {
      project.isEvaluated shouldBe false

      val exception = intercept[ForbiddenResolutionException] {
        project.getConfiguration("compile").resolve()
      }
      exception.getMessage shouldEqual "'compile' configuration is being resolved."

      project.isEvaluated shouldBe false
    }

  }
}

object ScalabuffSpec {
  def buildProject(fns: (Project => Any)*): Project =
    fns.foldLeft(ProjectBuilder.builder().build()) {
      (project, fn) => fn(project); project
    }

  def applyScalabuffPlugin = { project: Project => project.applyPlugin[ScalabuffPlugin] }

  def applyIdeaPlugin = { project: Project => project.applyPlugin[IdeaPlugin] }

  def declareCompileDependencyToScalaLibrary = {
    scalaVersion: String => {
      project: Project =>
        project.getDependencies.add("compile", s"org.scala-lang:scala-library:$scalaVersion")
    }
  }

  def declareJCenterRepo = { project: Project =>
    project.getRepositories.add(project.getRepositories.jcenter())
  }

  def forbidAnyDependencyResolution = { project: Project =>
    project.getConfigurations.asScala.foreach { config =>
      config.getIncoming.beforeResolve {
        val closure = () => throw new ForbiddenResolutionException(config)
        new MethodClosure(closure, "apply")
      }
    }
  }

  class ForbiddenResolutionException(config: Configuration)
    extends GradleException(s"'${config.getName}' configuration is being resolved.")
}
