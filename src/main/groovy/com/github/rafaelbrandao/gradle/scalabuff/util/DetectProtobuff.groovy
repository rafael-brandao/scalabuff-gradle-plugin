/*
 * Copyright (c) 2014 Rafael Brandão <rafa.bra@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.rafaelbrandao.gradle.scalabuff.util

import groovy.transform.CompileStatic

import org.gradle.api.Project
import org.gradle.process.ExecSpec
import org.gradle.process.internal.ExecException

import fj.data.Option
import org.apache.tools.ant.taskdefs.condition.Os

import com.github.rafaelbrandao.gradle.scalabuff.ScalabuffExtension

@CompileStatic
trait DetectProtobuff {

    static Option<SemanticVersion> getProtobuffVersion(Project project, ScalabuffExtension scalabuff) {
        try {
            final output = new ByteArrayOutputStream()
            project.exec { ExecSpec spec ->
                spec.with {
                    if (Os.isFamily(Os.FAMILY_WINDOWS))
                        commandLine 'cmd', '/c', scalabuff.protocPath
                    else
                        commandLine scalabuff.protocPath

                    args = ['--version']
                    standardOutput = output
                }
                spec
            }
            SemanticVersion.of(output.toString().trim())
        } catch (ExecException ignored) {
            Option.none()
        }
    }

}
