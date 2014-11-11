/*
 * Copyright (c) 2014 Rafael Brandão <rafa.bra@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.rafaelbrandao.gradle.scalabuff

import java.nio.file.Files
import java.nio.file.Path
import groovy.io.FileType
import groovy.transform.CompileStatic
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.process.ExecSpec
import org.gradle.process.JavaExecSpec

import org.apache.tools.ant.taskdefs.condition.Os

@CompileStatic
class ScalabuffTasks {
    static final String SCALABUFF_TASK_GROUP = "Scalabuff"
}

@CompileStatic
interface TaskAction {
    void taskAction()
}

@CompileStatic
trait OutputDirectory {
    abstract File getOutputDirectory()
}

@CompileStatic
trait ScalabuffTaskAction implements TaskAction, Task, OutputDirectory {
    @org.gradle.api.tasks.TaskAction
    void scalabuffTaskAction() {
        try {
            outputDirectory.mkdirs()
            taskAction()
        } catch (Exception e) {
            throw new TaskExecutionException(this, e)
        }
    }
}

@CompileStatic
trait ScalabuffCompileAction extends ScalabuffTaskAction {

    abstract Collection<File> getSourceSetDirs()

    abstract String getConfigurationName()

    void taskAction() {
        String outputDirectoryPath = outputDirectory.canonicalPath
        Configuration cp = project.configurations.getByName(configurationName)
        boolean isInfoEnabled = project.logger.isInfoEnabled()

        project.javaexec { JavaExecSpec spec ->
            spec.with {
                main = 'net.sandrogrzicic.scalabuff.compiler.ScalaBuff'
                classpath = cp
                args = [
                    sourceSetDirs.collect { "--proto_path=$it.canonicalPath" as String },
                    ["--scala_out=$outputDirectoryPath" as String],
                    { isInfoEnabled ? ['--verbose'] : [] }()
                ].flatten()
            }
            spec
        }
    }

}

@CompileStatic
trait ScalabuffPostCompileAction implements Task, TaskAction, OutputDirectory {

    abstract String getScalaBaseVersion()

    void taskAction() {
        super.taskAction()
        if (scalaBaseVersion == '2.11') {
            project.logger.info('Scala 2.11 detected. Post processing scala files...')

            final Path tempFile = Files.createTempFile('temp', '.scala')
            final Closure replaceFn = replaceScalaFile

            outputDirectory.listFiles().each { File f1 ->
                f1.eachFileRecurse(FileType.FILES) { File f2 ->
                    if ((f2.name =~ ~/^(.+)\.scala$/).matches()) {
                        replaceFn(f2.toPath(), tempFile)
                    }
                }
            }
        }
    }

    private Closure replaceScalaFile = { Path scalaFile, Path tempFile ->
        String line
        tempFile.withWriter { Writer writer ->
            BufferedReader reader = scalaFile.newReader()
            try {
                while ((line = reader.readLine()) != null) {
                    writer.write(
                        (line =~ /@reflect\.BeanProperty/)
                            .replaceAll('@beans.BeanProperty').concat('\n')
                    )
                }
            } finally {
                reader.close()
            }
        }
        Files.copy(tempFile, scalaFile, REPLACE_EXISTING)
    }
}


@CompileStatic
trait GenerateProtoDescriptorsAction extends ScalabuffTaskAction {

    abstract List<File> getProtoFiles()

    abstract String getProtocPath()

    void taskAction() {
        String descriptorFile
        String outputPath = outputDirectory.canonicalPath
        Project project = this.project

        protoFiles.each { File protoFile ->
            descriptorFile = "${protoFile.name}.descriptor"
            project.exec { ExecSpec spec ->
                spec.with {
                    if (Os.isFamily(Os.FAMILY_WINDOWS))
                        commandLine 'cmd', '/c', protocPath
                    else
                        commandLine protocPath

                    args = [
                        "--proto_path=${protoFile.parentFile.canonicalPath}" as String,
                        "--descriptor_set_out=$outputPath/$descriptorFile" as String,
                        protoFile.canonicalPath
                    ]
                }
                spec
            }
        }
    }

}

@CompileStatic
class ScalabuffCompile extends DefaultTask
    implements ScalabuffCompileAction, ScalabuffPostCompileAction {

    static final String CONFIGURATION_NAME = 'scalabuffCompile'
    static final String NAME = CONFIGURATION_NAME

    String configurationName = CONFIGURATION_NAME

    @InputFiles
    @SkipWhenEmpty
    Collection<File> sourceSetDirs

    @Input
    String scalaBaseVersion

    @org.gradle.api.tasks.OutputDirectory
    File outputDirectory
}

@CompileStatic
class GenerateProtoDescriptors extends DefaultTask
    implements GenerateProtoDescriptorsAction {

    static final String NAME = 'generateProtoDescriptors'

    @InputFiles
    @SkipWhenEmpty
    List<File> protoFiles

    @Input
    String protocPath = 'protoc'

    @org.gradle.api.tasks.OutputDirectory
    File outputDirectory
}
