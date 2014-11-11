/*
 * Copyright (c) 2014 Rafael Brandão <rafa.bra@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.github.rafaelbrandao.gradle.scalabuff.util

import java.util.regex.Matcher
import java.util.regex.Pattern
import groovy.transform.Canonical
import groovy.transform.CompileStatic

import fj.P1
import fj.data.Option
import static fj.data.Option.iif
import static fj.data.Option.none

@Canonical
@CompileStatic
class SemanticVersion {

    Option<String> major = none()
    Option<String> minor = none()
    Option<String> patch = none()
    Option<String> revision = none()
    private Option<String> revisionSeparator = none()

    String print(String revisionSeparator = this.revisionSeparator.orSome('-')) {
        [major, minor, patch]
            .filter { Option<String> v -> v.some }
            .collect { Option<String> v -> v.some() }
            .join('.') << revision.map { String s -> revisionSeparator.concat(s) }.orSome('')
    }

    String toString() {  print() }

    static Option<SemanticVersion> of(String version) {
        final Matcher m = (version =~ pattern)
        iif(m.matches(), {
            new SemanticVersion(
                major: Option.fromString(m.group(2)),
                minor: Option.fromString(m.group(8)),
                patch: Option.fromString(m.group(14)),
                revision:
                    [m.group(5), m.group(7), m.group(11), m.group(13), m.group(17)]
                        .findFirst { it },
                revisionSeparator:
                    [m.group(4), m.group(6), m.group(10), m.group(12), m.group(16)]
                        .findFirst { it }
            )
        } as P1)
    }

    private static final Pattern pattern =
        ~/([^\.]+ )?(\d+)((-)([^ ]+)|(\.)([^\d^ ].*)|\.(\d+)((-)([^ ]+)|(\.)([^\d^ ].*)|\.(\d+)(([-\.])([^ ]+))?)?)?( .*)?$/
}
