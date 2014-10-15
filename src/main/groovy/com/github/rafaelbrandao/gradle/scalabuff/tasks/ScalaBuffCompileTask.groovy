/*
 * Copyright (c) 2014 Rafael Brandão <rafa.bra@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.rafaelbrandao.gradle.scalabuff.tasks

import net.sandrogrzicic.scalabuff.compiler.ScalaBuff
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class ScalaBuffCompileTask extends DefaultTask {

    @TaskAction
    def compileProto() {
        def sourceSetDirs = getSourceSetDirs()
        def nonExistentSourceSetDirs = sourceSetDirs.findAll { !it.exists() }
        if (nonExistentSourceSetDirs.size() > 0) {
            throw new GradleException(nonExistentSourceSetDirs.collect {
                "\ndirectory '$it' not found"
            }.join(', ') + '.')
        }
        def args = (sourceSetDirs.collect { File inputDir ->
            "--proto_path=${inputDir.absolutePath}"
        } + ["--scala_out=${project.scalabuff.generatedSourcesDir}", '--verbose']).collect {
            it.toString()
        }

        try {
            project.scalabuff.generatedSourcesDir.mkdirs()
            project.scalabuff.generatedResourcesDir.mkdirs()

            ScalaBuff.run(args as String[])

            if (project.scalabuff.generateDescriptor) {
                protoFiles.each {
                    def descriptor = it.name + '.descriptor'
                    def command = [
                            "protoc",
                            "--proto_path=${it.parent}",
                            "--descriptor_set_out=${project.scalabuff.generatedResourcesDir}/$descriptor",
                            it.absolutePath
                    ]
                    command.execute().waitFor()
                }
            }
        }
        catch (Exception e) {
            throw new GradleException(e.getMessage(), e)
        }
    }

    @OutputDirectory
    File getOutputDirectory() {
        project.scalabuff.outputDir
    }

    @InputFiles
    List<File> getProtoFiles() {
        getSourceSetDirs().collect { File dir ->
            dir.listFiles().findAll { File file -> file.name.endsWith('.proto') }
        }.flatten()
    }

    private List<File> getSourceSetDirs() {
        project.scalabuff.sourceSets.collect {
            project.file(it.srcDir)
        }
    }
}
