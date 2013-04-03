// BridgeDb,
// An abstraction layer for identifier mapping services, both local and online.
//
// Copyright 2006-2009  BridgeDb developers
// Copyright 2012-2013  Christian Y. A. Brenninkmeijer
// Copyright 2012-2013  OpenPhacts
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package org.bridgedb.tools.metadata;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bridgedb.rdf.reader.StatementReader;
import org.bridgedb.utils.BridgeDBException;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;

/**
 *
 * @author Christian
 */
public class ResourceMetaData extends HasChildrenMetaData implements MetaData{

    private final URI resourceType;
    private final Set<ResourceMetaData> parents = new HashSet<ResourceMetaData>();
    private boolean isParent1 = false;
    private boolean isLinked = false;
    private boolean directlyLinkedTo = false;
    private static String UNSPECIFIED = "unspecified";
    private String label; 

    ResourceMetaData(Resource id, URI type) {
        super(getLocalName(type), UNSPECIFIED, RequirementLevel.SHOULD, new ArrayList<MetaDataBase>());
        if (type != null){
            childMetaData.add(PropertyMetaData.getTypeProperty(id,type.getLocalName()));
        }
        this.resourceType = type;
        setupValues(id);
    }
    
    private static String getLocalName(URI type){
        if (type == null){
            return "(" + UNSPECIFIED + ")";
        }
        return type.getLocalName();
    }
    
    ResourceMetaData(URI type, List<MetaDataBase> childMetaData) {
        super(type.getLocalName(), type.getLocalName(), RequirementLevel.SHOULD, childMetaData);
        childMetaData.add(PropertyMetaData.getTypeProperty(type.getLocalName()));
        this.resourceType = type;
        label = "(" + type + ") ";
    }
        
    private ResourceMetaData(Resource id, ResourceMetaData other, MetaDataCollection collection) {
        super(id, other, collection);
        this.resourceType = other.resourceType;
//        setupValues(id);
    }

    /*private ResourceMetaData(String name){
        super(name, new ArrayList<MetaDataBase>());
        type = null;
    }*/
    
   @Override
    public void loadValues(Set<Statement> data, Set<String> errors) {
        super.loadValues(data, errors);
        Set<URI> predicates = getUsedPredicates(data);
        for (URI predicate:predicates){
            PropertyMetaData metaData = PropertyMetaData.getUnspecifiedProperty(id, predicate, type);
            metaData.loadValues(data, errors);
            childMetaData.add(metaData);
        }
    }

    void setupValues(Resource id){
        super.setupValues(id);
        if (id.stringValue().startsWith(StatementReader.DEFAULT_BASE_URI)){
            label = id.stringValue().substring(StatementReader.DEFAULT_BASE_URI.length());
        } else {
            label = id.stringValue();
        }    
        if (!UNSPECIFIED.equals(type)){
            label = "(" + type + ") " + label;
        }
    }
    
    private Set<URI> getUsedPredicates(Set<Statement> data){
        Set<URI> results = new HashSet<URI>();
        for (Statement statement: data){
            if (statement.getSubject().equals(id)){
                results.add(statement.getPredicate());
            }
        }        
        return results;
    }
    
    @Override
    public void appendSchema(StringBuilder builder, int tabLevel) {
        tab(builder, tabLevel);
        builder.append("Resource ");
        builder.append(name);
        newLine(builder, tabLevel + 1);
        builder.append("rdf:type ");
        builder.append(resourceType);
        newLine(builder);
        for (MetaDataBase child:childMetaData){
            child.appendSchema(builder, tabLevel + 1);
        }
    }

    URI getType() {
        return resourceType;
    }

    public ResourceMetaData getSchemaClone(Resource id, MetaDataCollection collection) {
        return new ResourceMetaData(id, this, collection);
    }

    @Override
    boolean hasValues() {
        //A Resource has values if any of the children have values.
        for (MetaDataBase child:childMetaData){
            if (child.hasValues()) { 
                return true; 
            }
        }
        return false;
    }

    @Override
    public void appendValidityReport(StringBuilder builder, boolean checkAllpresent, boolean includeWarnings, int tabLevel) 
            throws BridgeDBException {
        appendSpecific(builder, tabLevel);
        if (type.equals(UNSPECIFIED)){
            tab(builder, tabLevel+1);
            if (this.resourceType == null){
                builder.append("INFO: ");
                builder.append(" No type specified so unable to validate.");
                newLine(builder);
            } else {
                builder.append("INFO: ");
                builder.append(" No requirments have been specified for type ");
                builder.append(this.resourceType);
                newLine(builder);
            }
        } else {
            int before = builder.length();
            for (MetaDataBase child:childMetaData){
                child.appendValidityReport(builder, checkAllpresent, includeWarnings, tabLevel+1);
            }
            if (before == builder.length()){
                tab(builder, tabLevel+1);
                builder.append("All Required values found and correctly typed. ");
                newLine(builder);               
            }
        }
        newLine(builder);
    }

    private void appendSpecificNoNewLine(StringBuilder builder, int tabLevel) {
        tab(builder, tabLevel);
        builder.append(label);
    }
    
    @Override
    void appendSpecific(StringBuilder builder, int tabLevel) {
        appendSpecificNoNewLine(builder, tabLevel);
        newLine(builder);
    }
     
    public void appendParentValidityReport(StringBuilder builder, boolean checkAllpresent, boolean includeWarnings, int tabLevel) throws BridgeDBException{
        if (!isSuperset()){
            //Wrong method called 
            appendValidityReport(builder, checkAllpresent, includeWarnings, tabLevel);
        } else { 
            appendSpecific(builder, tabLevel);
            if (!this.hasCorrectTypes()) {            
                //Report all to pick up the incorrect type
                for (MetaDataBase child:childMetaData){
                    child.appendValidityReport(builder, false, false, tabLevel + 1);
                }
                if (hasRequiredValues()) {
                    tab(builder, tabLevel+1);
                    builder.append("All Required values found but above incorrectly typed. ");
                    newLine(builder);
                } else {
                    tab(builder, tabLevel+1);
                    builder.append("WARNING: Incomplete so can only be used as a superset ");
                    newLine(builder);                
                }
            } else if (hasRequiredValues()) {
                tab(builder, tabLevel+1);
                builder.append("All Required values found and correctly typed. ");
                newLine(builder);
            } else {
                tab(builder, tabLevel+1);
                builder.append("WARNING: Incomplete so can only be used as a superset ");
                newLine(builder);                
            }
            newLine(builder);                
        }
    }
    
    void addParent(ResourceMetaData parent, Set<String> errors) {
        parents.add(parent);
        Set<LeafMetaData> leaves = getLeaves();
        for (LeafMetaData leaf: leaves){
            URI predicate = leaf.getPredicate();
            LeafMetaData parentLeaf = parent.getLeafByPredicate(predicate);
            leaf.addParent(parentLeaf, errors);
        }
        parent.isParent1 = true;
    }

    boolean isSuperset() {
        return isParent1 && !isLinked;
    }

    public void appendSummary(StringBuilder builder, int tabLevel) throws BridgeDBException {
        tab(builder, tabLevel);
        if (type.equals(UNSPECIFIED)){
            builder.append(label);
            builder.append(" has an unspecified type of ");
            builder.append(resourceType);
        } else {
            appendSpecificNoNewLine(builder, tabLevel);
            if (this.hasCorrectTypes()){
                if (this.hasRequiredValues()){
                    builder.append(" OK!");
                } else if (this.hasRequiredValues()){
                    builder.append(" has missing MUST values.");
                } else if (isSuperset()){
                    builder.append(" can only be used as a superset.");
                } else {
                    builder.append(" incomplete!");
                }
            } else {
                builder.append(" has incorrect typed statements.");
            }
        }
        newLine(builder);        
    }

    public Resource getId() {
        return id;
    }

    void addChildren(List<MetaDataBase> childMetaData) {
        super.addChildren(childMetaData);
    }

    void setAsLinked() {
        isLinked = true;
    }
}
