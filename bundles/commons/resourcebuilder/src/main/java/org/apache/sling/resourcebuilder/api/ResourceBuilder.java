/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.resourcebuilder.api;

import org.apache.sling.api.resource.Resource;

/** Builds Sling Resources using a simple fluent API */
public interface ResourceBuilder {
    
    public static final String DEFAULT_PRIMARY_TYPE = "nt:unstructured";
    
    /** Create a Resource, which optionally becomes the current 
     *  parent Resource. 
     * @param relativePath The path of the Resource to create, relative to 
     *          this builder's current parent Resource.
     * @param properties optional name-value pairs 
     * @return this builder
     */
    ResourceBuilder resource(String relativePath, Object ... properties);
    
    /** Commit created resources */
    ResourceBuilder commit();
    
    /** Set the primary type for intermediate resources created
     *  when the parent of resource being created does not exist.
     * @param primaryType If null the DEFAULT_PRIMARY_TYPE is used.
     * @return this builder
     */
    ResourceBuilder withIntermediatePrimaryType(String primaryType);
    
    /** Set siblings mode (as opposed to hierarchy mode) where creating a resource 
     *  doesn't change the current parent. Used to create flat structures.
     *  This is off by default.
     * @return this builder
     */
    ResourceBuilder siblingsMode();
    
    /** Set hierarchy mode (as opposed to siblings mode) where creating a resource 
     *  sets it as the current parent. Used to create tree structures.
     *  This is on by default.
     * @return this builder
     */
    ResourceBuilder hierarchyMode();
    
    /** Return the current parent resource */
    Resource getCurrentParent();
    
    /** Reset the current parent Resource to the original one */ 
    ResourceBuilder resetParent();
}