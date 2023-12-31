/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
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
package org.keycloak.models.map.storage.jpa.authorization.resource.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.keycloak.models.map.storage.jpa.JpaAttributeEntity;

@Entity
@Table(name = "kc_authz_resource_attribute")
public class JpaResourceAttributeEntity extends JpaAttributeEntity<JpaResourceEntity> {

    public JpaResourceAttributeEntity() {
    }

    public JpaResourceAttributeEntity(JpaResourceEntity root, String name, String value) {
        super(root, name, value);
    }
}
