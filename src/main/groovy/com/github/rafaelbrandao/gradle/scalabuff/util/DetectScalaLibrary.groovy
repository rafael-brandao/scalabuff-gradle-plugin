/*
 * Copyright (c) 2014 Rafael Brandão <rafa.bra@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.rafaelbrandao.gradle.scalabuff.util

import groovy.transform.CompileStatic

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.artifacts.Dependency

import fj.P1
import fj.data.Option

@CompileStatic
trait DetectScalaLibrary {

    static Option<String> getScalaBaseVersionFrom(String version) {
        SemanticVersion.of(version).flatMap { SemanticVersion v ->
            v.major.flatMap { String major ->
                v.minor.map { String minor -> "${major}.$minor" }
            }
        }.orElse({
            throw new InvalidUserDataException("'$version' is not a valid scala version string.")
        } as P1)
    }

    static Option<Dependency> getScalaLibraryFrom(Project project) {
        Option.fromNull(
            project
                .configurations.getByName('compile')
                .dependencies.find { Dependency d -> d.name == 'scala-library' }
        ).orElse({
            throw new ProjectConfigurationException(
                "Project ${project.name} must add a 'compile' library to 'scala-library'.", null
            )
        } as P1)
    }
}
