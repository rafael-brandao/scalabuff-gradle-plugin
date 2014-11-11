/*
 * Copyright (c) 2014 Rafael Brandão <rafa.bra@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.rafaelbrandao.gradle.scalabuff

import groovy.transform.CompileStatic

import org.gradle.api.NamedDomainObjectContainer

import com.github.rafaelbrandao.gradle.scalabuff.domain.ScalabuffSourceSet

@CompileStatic
class ScalabuffExtension {

    static final String NAME = 'scalabuff'
    static final String DEFAULT_SOURCE_SET_NAME = 'proto'

    final NamedDomainObjectContainer<ScalabuffSourceSet> sourceSets
    final File outputDir
    final File generatedSourcesDir
    final File generatedResourcesDir

    String protocPath
    boolean failIfProtocNotDetected
    boolean generateDescriptors

    ScalabuffExtension(NamedDomainObjectContainer<ScalabuffSourceSet> sourceSets, File outputDir) {
        this.sourceSets = sourceSets
        this.outputDir = outputDir
        this.generatedSourcesDir = new File(outputDir, 'generated-sources')
        this.generatedResourcesDir = new File(outputDir, 'generated-resources')

        // Apply defaults
        sourceSets
            .create(DEFAULT_SOURCE_SET_NAME)
            .srcDirs.add("src/$DEFAULT_SOURCE_SET_NAME" as String)

        protocPath = 'protoc'
        failIfProtocNotDetected = false
        generateDescriptors = true
    }

    def sourceSets(Closure closure) {
        sourceSets.configure(closure)
    }
}
