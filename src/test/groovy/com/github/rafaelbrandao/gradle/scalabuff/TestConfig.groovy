/*
 * Copyright (c) 2014 Rafael Brandão <rafa.bra@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.rafaelbrandao.gradle.scalabuff

class TestConfig {
    public static final String SCALA_BASE_VERSION

    static {
        def classLoader = Thread.currentThread().contextClassLoader
        InputStream inputStream = classLoader.getResourceAsStream('META-INF/test-runtime-config.groovy')
        def config = new ConfigSlurper().parse(inputStream.text)
        SCALA_BASE_VERSION = config.scala.base.version
    }
}
