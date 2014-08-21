/**
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glowroot.plugin.tomcat;

import org.apache.catalina.loader.WebappClassLoader;

// this is needed in order to override 'system' class loader, unfortunately, setting the parent and
// setting delegate = true is not enough
public class TomcatClassLoader extends WebappClassLoader {

    public TomcatClassLoader(ClassLoader parent) {
        super(parent);
        system = TomcatClassLoader.class.getClassLoader();
    }
}