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
package org.keycloak.models.map.storage.jpa.group.entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.*;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UuidValidator;
import org.keycloak.models.map.group.MapGroupEntity;
import org.keycloak.models.map.group.MapGroupEntity.AbstractGroupEntity;
import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_GROUP;
import org.keycloak.models.map.storage.jpa.JpaRootVersionedEntity;
import org.keycloak.models.map.storage.jpa.hibernate.jsonb.JsonbType;

/**
 * There are some fields marked by {@code @Column(insertable = false, updatable = false)}.
 * Those fields are automatically generated by database from json field,
 * therefore marked as non-insertable and non-updatable to instruct hibernate.
 */
@Entity
@Table(name = "kc_group", uniqueConstraints = {@UniqueConstraint(columnNames = {"realmId", "name", "parentId"})})
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonbType.class)})
public class JpaGroupEntity extends AbstractGroupEntity implements JpaRootVersionedEntity {

    @Id
    @Column
    private UUID id;

    //used for implicit optimistic locking
    @Version
    @Column
    private int version;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private final JpaGroupMetadata metadata;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private Integer entityVersion;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private String realmId;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private String name;

    @Column(insertable = false, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private String parentId;


    @ManyToMany(fetch = FetchType.LAZY, cascade = {})
    @JoinTable(name = "kc_group_reference",
            joinColumns = @JoinColumn(name = "parent_group", referencedColumnName = "ID"),
            inverseJoinColumns = @JoinColumn(name = "child_group", referencedColumnName = "ID"))
    @BatchSize(size=100)
    private Set<JpaGroupEntity> childGroupsReference;

    @BatchSize(size=100)
    @ManyToMany(mappedBy="childGroupsReference")
    private Set<JpaGroupEntity> parentGroupsReference;

    @OneToMany(mappedBy = "root", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private final Set<JpaGroupAttributeEntity> attributes = new HashSet<>();

    /**
     * No-argument constructor, used by hibernate to instantiate entities.
     */
    public JpaGroupEntity() {
        this.metadata = new JpaGroupMetadata();
    }

    public JpaGroupEntity(DeepCloner cloner) {
        this.metadata = new JpaGroupMetadata(cloner);
    }

    /**
     * Used by hibernate when calling cb.construct from read(QueryParameters) method.
     * It is used to select group without metadata(json) field.
     */
    public JpaGroupEntity(UUID id, int version, Integer entityVersion, String realmId, 
            String name, String parentId) {
        this.id = id;
        this.version = version;
        this.entityVersion = entityVersion;
        this.realmId = realmId;
        this.name = name;
        this.parentId = parentId;
        this.metadata = null;
    }

    public boolean isMetadataInitialized() {
        return metadata != null;
    }

    @Override
    public Integer getEntityVersion() {
        if (isMetadataInitialized()) return metadata.getEntityVersion();
        return entityVersion;
    }

    @Override
    public Integer getCurrentSchemaVersion() {
        return CURRENT_SCHEMA_VERSION_GROUP;
    }

    @Override
    public void setEntityVersion(Integer entityVersion) {
        metadata.setEntityVersion(entityVersion);
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public String getId() {
        return id == null ? null : id.toString();
    }

    @Override
    public void setId(String id) {
        String validatedId = UuidValidator.validateAndConvert(id);
        this.id = UUID.fromString(validatedId);
    }

    @Override
    public String getRealmId() {
        if (isMetadataInitialized()) return metadata.getRealmId();
        return realmId;
    }

    @Override
    public void setRealmId(String realmId) {
        metadata.setRealmId(realmId);
    }

    @Override
    public String getName() {
        if (isMetadataInitialized()) return metadata.getName();
        return name;
    }

    @Override
    public void setName(String name) {
        metadata.setName(name);
    }

    @Override
    public void setParentId(String parentId) {
        metadata.setParentId(parentId);
    }

    @Override
    public String getParentId() {
        if (isMetadataInitialized()) return metadata.getParentId();
        return parentId;
    }

    @Override
    public Set<String> getGrantedRoles() {
        return metadata.getGrantedRoles();
    }

    @Override
    public void setGrantedRoles(Set<String> grantedRoles) {
        metadata.setGrantedRoles(grantedRoles);
    }

    @Override
    public void addGrantedRole(String role) {
        metadata.addGrantedRole(role);
    }

    @Override
    public void removeGrantedRole(String role) {
        metadata.removeGrantedRole(role);
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        Map<String, List<String>> result = new HashMap<>();
        for (JpaGroupAttributeEntity attribute : attributes) {
            List<String> values = result.getOrDefault(attribute.getName(), new LinkedList<>());
            values.add(attribute.getValue());
            result.put(attribute.getName(), values);
        }
        return result;
    }

    @Override
    public void setAttributes(Map<String, List<String>> attributes) {
        this.attributes.clear();
        if (attributes != null) {
            for (Map.Entry<String, List<String>> entry : attributes.entrySet()) {
                setAttribute(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public List<String> getAttribute(String name) {
        return attributes.stream()
                .filter(a -> Objects.equals(a.getName(), name))
                .map(JpaGroupAttributeEntity::getValue)
                .collect(Collectors.toList());
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        removeAttribute(name);
        for (String value : values) {
            JpaGroupAttributeEntity attribute = new JpaGroupAttributeEntity(this, name, value);
            attributes.add(attribute);
        }
    }

    @Override
    public void removeAttribute(String name) {
        attributes.removeIf(attr -> Objects.equals(attr.getName(), name));
    }

    public Set<MapGroupEntity> getChildGroupsReference() {
        if (childGroupsReference == null) {
            childGroupsReference = new HashSet<>();
        }
        return childGroupsReference.stream().map(e -> (MapGroupEntity)e).collect(Collectors.toSet());
    }

    public void setChildGroupsReference(Set<MapGroupEntity> childGroupsReference) {
        this.childGroupsReference = childGroupsReference.stream().map(e -> (JpaGroupEntity)e).collect(Collectors.toSet());
    }

    public Set<MapGroupEntity> getParentGroupsReference() {
        if (parentGroupsReference == null) {
            parentGroupsReference = new HashSet<>();
        }
        return parentGroupsReference.stream().map(e -> (MapGroupEntity)e).collect(Collectors.toSet());
    }
    
    public void setParentGroupsReference(Set<MapGroupEntity> parentGroupsReference) {
        this.parentGroupsReference = parentGroupsReference.stream().map(e -> (JpaGroupEntity)e).collect(Collectors.toSet());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof JpaGroupEntity)) return false;
        return Objects.equals(getId(), ((JpaGroupEntity) obj).getId());
    }
}
