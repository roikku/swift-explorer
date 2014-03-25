/*
 *  Copyright 2012-2013 E.Hooijmeijer
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.swiftexplorer.swift.operations;

/**
 * ContainerSpec holds the specification of a new container.
 * @author E.Hooijmeijer
 */
public class ContainerSpecification {

    private String name;
    private boolean mustAuth;

    /**
     * creates a new container specification.
     * @param name the name of the container.
     * @param privateContainer true if its private.
     */
    public ContainerSpecification(String name, boolean privateContainer) {
        this.name = name;
        this.mustAuth = privateContainer;
    }

    public String getName() {
        return name;
    }

    public boolean isPrivateContainer() {
        return mustAuth;
    }

}
