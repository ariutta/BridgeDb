// BridgeDb,
// An abstraction layer for identifier mapping services, both local and online.
//
// Copyright      2012  Christian Y. A. Brenninkmeijer
// Copyright      2012  OpenPhacts
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
package org.bridgedb.linkset.rdf;

import org.bridgedb.rdf.constants.VoidConstants;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.bridgedb.IDMapperException;
import org.bridgedb.tools.metadata.LinksetVoidInformation;
import org.bridgedb.tools.metadata.constants.PavConstants;
import org.bridgedb.rdf.IDMapperLinksetException;
import org.bridgedb.url.URLListener;
import org.bridgedb.utils.BridgeDBException;
import org.bridgedb.utils.StoreType;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.CalendarLiteralImpl;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFHandlerException;

/**
 *
 * @author Christian
 */
public class RDFWriter implements RdfLoader{

    private final StoreType storeType;
    private final ArrayList<Statement> statements;
    private final int mappingId;
    private final URI linksetContext;
    private final URI inverseContext;
    private final Resource linksetResource;
    private final Resource inverseResource;
    private final boolean symmetric;
    private final String mainCaller;
    private String accessedFrom;
    private final URLListener urlListener;
    private static final URI HIGHEST_LINKSET_ID_PREDICATE = new URIImpl("http://www.bridgedb.org/highested_linkset_id");
    private static final Resource ANY_RESOURCE = null;

    public RDFWriter(StoreType storeType, LinksetVoidInformation information, URLListener listener, String mainCaller) 
            throws IDMapperException{
        this.storeType = storeType;
        urlListener = listener;
        statements = new ArrayList<Statement>();
        this.mainCaller = mainCaller;
        try {
            String subjectUriSpace = information.getSubjectUriSpace();
            String targetUriSpace = information.getTargetUriSpace();
            String predicate = information.getPredicate();
            //TODO work out way to do this
            symmetric = true;
            boolean transative = information.isTransative();
            linksetResource = information.getLinksetResource();
            inverseResource = invertResource(linksetResource);
            mappingId = urlListener.registerMappingSet(subjectUriSpace, predicate, targetUriSpace, 
                    symmetric, transative);            
            linksetContext = RdfFactory.getLinksetURL(mappingId);
            if (symmetric) {
                inverseContext = RdfFactory.getLinksetURL(mappingId + 1);             
            } else {
                inverseContext = null;
            }
        } catch (BridgeDBException ex) {
            throw new IDMapperLinksetException("Error setting the context", ex);
        }
     }

    private Resource invertResource(Resource resource){
        if (resource instanceof URI){
            return new URIImpl(resource.toString()+"_Symmetric");
        }
        return resource;
    }
    
    @Override
    public void processFirstNoneHeader(Statement firstMap) throws RDFHandlerException {
        RdfWrapper rdfWrapper = RdfFactory.setupConnection(storeType);
        for (Statement st:statements){
            rdfWrapper.add(st.getSubject(), st.getPredicate(), st.getObject(), linksetContext);
            if (st.getPredicate().equals(VoidConstants.SUBJECTSTARGET)){
                addInverse(rdfWrapper, st.getSubject(), VoidConstants.OBJECTSTARGET, st.getObject());    
            } else if (st.getPredicate().equals(VoidConstants.OBJECTSTARGET)){
                addInverse(rdfWrapper, st.getSubject(), VoidConstants.SUBJECTSTARGET, st.getObject()); 
            } else {
                addInverse(rdfWrapper, st.getSubject(), st.getPredicate(), st.getObject());                
            }
        }
        Value from;
        try {
            from =  new URIImpl(accessedFrom);
            try {
                from =  new URIImpl("file:///" +accessedFrom);
            } catch (Exception e){
                from = new LiteralImpl(accessedFrom);
            }
        } catch (Exception e){
            from = new LiteralImpl(accessedFrom);
        }
        rdfWrapper.add(linksetResource, PavConstants.SOURCE_ACCESSED_FROM, from, linksetContext);
        addInverse(rdfWrapper, linksetResource, PavConstants.SOURCE_ACCESSED_FROM, from);
        GregorianCalendar gcal = (GregorianCalendar) GregorianCalendar.getInstance();
        try {
            XMLGregorianCalendar xgcal = DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal);
            rdfWrapper.add(linksetResource, PavConstants.SOURCE_ACCESSED_ON, new CalendarLiteralImpl(xgcal), linksetContext);
            addInverse(rdfWrapper, linksetResource, PavConstants.SOURCE_ACCESSED_ON, new CalendarLiteralImpl(xgcal));
        } catch (DatatypeConfigurationException ex) {
            //Should never happen so basically ignore
            ex.printStackTrace();
        }
        rdfWrapper.add(linksetResource, PavConstants.SOURCE_ACCESSED_BY, new LiteralImpl(mainCaller), linksetContext);
        addInverse(rdfWrapper, linksetResource, PavConstants.SOURCE_ACCESSED_BY, new LiteralImpl(mainCaller));
        addInverse(rdfWrapper, inverseResource, PavConstants.DERIVED_FROM, linksetResource);
        rdfWrapper.shutdown();
    }

    private void addInverse(RdfWrapper rdfWrapper, Resource subject, URI predicate, Value object) 
            throws RDFHandlerException{
        if (subject.equals(linksetResource)){
            rdfWrapper.add(inverseResource, predicate, object, inverseContext); 
        } else {
            rdfWrapper.add(subject, predicate, object, inverseContext);
        }
    }
    
    @Override
    public void addHeaderStatement(Statement st) throws RDFHandlerException {
        statements.add(st);
    }

    @Override
    public void closeInput() throws IDMapperException {
        urlListener.closeInput();
    }

    @Override
    public void insertURLMapping(Statement st) throws RDFHandlerException {
        String sourceURL = st.getSubject().stringValue();
        String targetURL = st.getObject().stringValue();
        try {
            urlListener.insertURLMapping(sourceURL, targetURL, mappingId, symmetric);
        } catch (IDMapperException ex) {
            throw new RDFHandlerException ("Error inserting mapping. ", ex);
        }
    }

    @Override
    public void setSourceFile(String absolutePath) {
        accessedFrom = absolutePath;
    }

    @Override
    public String getBaseUri() {
        return linksetContext.stringValue() + "/";
    }

}