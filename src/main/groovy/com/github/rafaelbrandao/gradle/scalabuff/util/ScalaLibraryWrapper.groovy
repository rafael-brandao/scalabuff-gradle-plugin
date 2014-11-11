/*
 * Copyright (c) 2014 Rafael Brandão <rafa.bra@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.rafaelbrandao.gradle.scalabuff.util

import groovy.transform.Canonical
import groovy.transform.CompileStatic

import org.gradle.api.artifacts.Dependency

@Canonical
@CompileStatic
class ScalaLibraryWrapper {
    final Dependency library
    final String baseVersion

    ScalaLibraryWrapper(Dependency dependency, String baseVersion) {
        this.library = dependency
        this.baseVersion = baseVersion
    }
}
