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
package org.bridgedb.ws.server;


import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import org.apache.log4j.Logger;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.metadata.validator.ValidationType;
import org.bridgedb.mysql.MySQLSpecific;
import org.bridgedb.rdf.RdfConfig;
import org.bridgedb.rdf.RdfReader;
import org.bridgedb.rdf.RdfFactory;
import org.bridgedb.rdf.StatementReader;
import org.bridgedb.sql.SQLAccess;
import org.bridgedb.sql.SQLUrlMapper;
import org.bridgedb.sql.SqlFactory;
import org.bridgedb.statistics.MappingSetInfo;
import org.bridgedb.statistics.OverallStatistics;
import org.bridgedb.url.URLMapping;
import org.bridgedb.utils.BridgeDBException;
import org.bridgedb.utils.IpConfig;
import org.bridgedb.utils.Reporter;
import org.bridgedb.utils.StoreType;
import org.bridgedb.ws.WSOpsService;
import org.openrdf.rio.RDFFormat;

/**
 *
 * @author Christian
 */
public class WSOpsServer extends WSOpsService implements Comparator<MappingSetInfo>{
    
    private NumberFormat formatter;
        
    static final Logger logger = Logger.getLogger(WSOpsService.class);

    public WSOpsServer()  throws IDMapperException   {
        super();
        urlMapper = new SQLUrlMapper(false, StoreType.LIVE);
        idMapper = urlMapper;
        formatter = NumberFormat.getInstance();
        if (formatter instanceof DecimalFormat) {
            DecimalFormatSymbols dfs = new DecimalFormatSymbols();
            dfs.setGroupingSeparator(',');
            ((DecimalFormat) formatter).setDecimalFormatSymbols(dfs);
        }
        logger.info("WsOpsServer setup");        
      }
            
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response welcomeMessage() throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("welcomeMessage called!");
                }
        StringBuilder sb = new StringBuilder();
        StringBuilder sbInnerPure;
        StringBuilder sbInnerEncoded;

        sb.append("<?xml version=\"1.0\"?>");
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
                + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">");
        sb.append("<meta http-equiv=\"content-type\" content=\"text/html; charset=ISO-8859-1\"/>");
        sb.append("<head><title>OPS IMS</title></head><body>");
        sb.append("<h1>Open PHACTS Identity Mapping Service</h1>");
        sb.append("\n<p>Welcome to the prototype Identity Mapping Service. </p>");
       
        OverallStatistics statistics = urlMapper.getOverallStatistics();
        sb.append("\n<p>Currently the service includes: ");
        sb.append("<ul>");
            sb.append("<li>");
                sb.append(formatter.format(statistics.getNumberOfMappings()));
                sb.append(" Mappings</li>");
            sb.append("<li>From ");
                sb.append(formatter.format(statistics.getNumberOfMappingSets()));
                sb.append(" Mapping Sets</li>");
            sb.append("<li>Covering ");
                sb.append(formatter.format(statistics.getNumberOfSourceDataSources()));
                sb.append(" Source Data Sources</li>");
            sb.append("<li>Using ");
                sb.append(formatter.format(statistics.getNumberOfPredicates()));
                sb.append(" Predicates</li>");
            sb.append("<li>Mapping to ");
                sb.append(formatter.format(statistics.getNumberOfTargetDataSources()));
                sb.append(" Target Data Sources</li>");
        sb.append("</ul></p>");
        
        sb.append("\n<p>The links where last updated ");
        sb.append(idMapper.getCapabilities().getProperty("LastUpdates"));
        sb.append("</p>");
                
        sb.append("\n<p>A List of which mappings we current have can be found at ");
        sb.append("<a href=\"/OPS-IMS/getMappingInfo\">Mapping Info Page</a></p>");
        
        sb.append("\n<p>The Main OPS method is <a href=\"/OPS-IMS/api/#mapByURLs\">mapByURLs</a></dt>");
        sb.append("<dd>List the URLs that map to this URL</dd>");
        sb.append("\n<p><a href=\"/OPS-IMS/api\">API Page</a></p>");
        sb.append("</body></html>");
        return Response.ok(sb.toString(), MediaType.TEXT_HTML).build();
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/api")
    public Response apiPage() throws IDMapperException, UnsupportedEncodingException {
        //Long start = new Date().getTime();
        StringBuilder sb = new StringBuilder();
 
        Set<String> urls = urlMapper.getSampleSourceURLs();  
        Iterator<String> urlsIt = urls.iterator();
        Xref first = urlMapper.toXref(urlsIt.next());
        String sysCode = first.getDataSource().getSystemCode();
        Xref second =  urlMapper.toXref(urlsIt.next());
        Set<Xref> firstMaps = idMapper.mapID(first);
        Set<String> keys = idMapper.getCapabilities().getKeys();
        String URL1 = urlsIt.next();
        String text = SQLUrlMapper.getId(URL1);
        String URL2 = urlsIt.next();
        Set<URLMapping> mappings2 = urlMapper.mapURLFull(URL2);
        HashSet<String> URI2Spaces = new HashSet<String>();
        int mappingId = 0;
        for (URLMapping mapping:mappings2){
            if (mapping.getId() != null){
                mappingId = mapping.getId();
            }
            String targetURL = mapping.getTargetURLs().iterator().next();
            URI2Spaces.add(SQLUrlMapper.getUriSpace(targetURL));            
        }
        boolean freeSearchSupported = idMapper.getCapabilities().isFreeSearchSupported(); 

        sb.append("<?xml version=\"1.0\"?>");
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
                + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">");
        sb.append("<meta http-equiv=\"content-type\" content=\"text/html; charset=ISO-8859-1\"/>");
        sb.append("<head><title>OPS IMS</title></head><body>");
        sb.append("<h1>Open PHACTS Identity Mapping Service</h1>");
        sb.append("\n<p><a href=\"/OPS-IMS\">Home Page</a></p>");
                
        sb.append("\n<p>");
        WSOpsApi api = new WSOpsApi();

        sb.append("Support services include:");
        sb.append("<dl>");      
        api.introduce_IDMapper(sb, freeSearchSupported);
        api.introduce_IDMapperCapabilities(sb, keys, freeSearchSupported);     
        api.introduce_URLMapper(sb, freeSearchSupported);
        api.introduce_Info(sb);
        sb.append("</dl>");
        sb.append("</p>");
        
        api.describeParameter(sb);        
        
        api.describe_IDMapper(sb, first, firstMaps, second, freeSearchSupported);
        api.describe_IDMapperCapabilities(sb, first, firstMaps, keys, freeSearchSupported);
        api.describe_URLMapper(sb, URL1, URL2, URI2Spaces, text, mappingId, sysCode, freeSearchSupported);
        api.describe_Info(sb);
        
        sb.append("</body></html>");
        //ystem.out.println("Done "+ (new Date().getTime() - start));
        return Response.ok(sb.toString(), MediaType.TEXT_HTML).build();
    }
    
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Path("/validateVoid")
    public Response validateVoid(@FormParam(INFO)String info, 
            @FormParam(MIME_TYPE)String mimeType) throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("validateVoid called!");
                    if (info == null){
                        logger.debug("NO Info");
                    } else {
                        logger.debug("info length = " + info.length());
                    }
                    if (mimeType == null){
                        logger.debug("NO mimeType");
                    } else {
                        logger.debug("mimeType = " + mimeType);
                    }
                }
        return validate(info, mimeType, ValidationType.VOID);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/validateVoid")
    public Response validateVoidGet(@QueryParam(INFO)String info, 
            @QueryParam(MIME_TYPE)String mimeType) throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("getValidateVoid called!");
                    if (info == null){
                        logger.debug("NO Info");
                    } else {
                        logger.debug("info length = " + info.length());
                    }
                    if (mimeType == null){
                        logger.debug("NO mimeType");
                    } else {
                        logger.debug("mimeType = " + mimeType);
                    }
                }
        return validate(info, mimeType, ValidationType.VOID);
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/validateTurtleVoid")
    public Response validateTurtleVoid(@FormDataParam("file") InputStream uploadedInputStream) 
            throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("validateTurtleVoid called!");
                    if (uploadedInputStream == null){
                        logger.debug("NO uploadedInputStream");
                    } else {
                        try {
                            logger.debug("uploadedInputStream.available = " + uploadedInputStream.available());
                        } catch (IOException ex) {
                            logger.error("unable to get inputStream.available:", ex);
                        }
                    }
                }
        return validate(uploadedInputStream, RDFFormat.TURTLE, ValidationType.VOID);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/validateTurtleVoid")
    public Response validateTurtleVoidGet() 
            throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("getValidateTurtleVoid called!");
                }
        return validate(null, RDFFormat.TURTLE, ValidationType.VOID);
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/validateRdfXmlVoid")
    public Response validateRdfXmlVoid(@FormDataParam("file") InputStream uploadedInputStream) 
            throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("validateRdfXmlVoid called!");
                    if (uploadedInputStream == null){
                        logger.debug("NO uploadedInputStream");
                    } else {
                        try {
                            logger.debug("uploadedInputStream.available = " + uploadedInputStream.available());
                        } catch (IOException ex) {
                            logger.error("unable to get inputStream.available:", ex);
                        }
                    }
                }
        return validate(uploadedInputStream, RDFFormat.RDFXML, ValidationType.VOID);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/validateRdfXmlVoid")
    public Response validateRdfXmlVoidGet() 
            throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("getValidateRdfXmlVoid called!");
                }
        return validate(null, RDFFormat.RDFXML, ValidationType.VOID);
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/validateNTriplesVoid")
    public Response validateNTriplesVoid(@FormDataParam("file") InputStream uploadedInputStream) 
            throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("validateNTriplesVoid called!");
                    if (uploadedInputStream == null){
                        logger.debug("NO uploadedInputStream");
                    } else {
                        try {
                            logger.debug("uploadedInputStream.available = " + uploadedInputStream.available());
                        } catch (IOException ex) {
                            logger.error("unable to get inputStream.available:", ex);
                        }
                    }
                }
        return validate(uploadedInputStream, RDFFormat.NTRIPLES, ValidationType.VOID);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/validateNTriplesVoid")
    public Response validateNTriplesVoidGet() 
            throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("getValidateNTriplesVoid called!");
                }
        return validate(null, RDFFormat.NTRIPLES, ValidationType.VOID);
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Path("/validateLinkSet")
    public Response validateLinkSet(@FormParam(INFO)String info, 
            @FormParam(MIME_TYPE)String mimeType) throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("validateLinkSet called!");
                    if (info == null){
                        logger.debug("NO Info");
                    } else {
                        logger.debug("info length = " + info.length());
                    }
                    if (mimeType == null){
                        logger.debug("NO mimeType");
                    } else {
                        logger.debug("mimeType = " + mimeType);
                    }
                }
        return validate(info, mimeType, ValidationType.LINKS);
    }
    
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Path("/validateLinkSet")
    public Response validateLinkSetGet(@QueryParam(INFO)String info, 
            @QueryParam(MIME_TYPE)String mimeType) throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("getValidateLinkSet called!");
                    if (info == null){
                        logger.debug("NO Info");
                    } else {
                        logger.debug("info length = " + info.length());
                    }
                    if (mimeType == null){
                        logger.debug("NO mimeType");
                    } else {
                        logger.debug("mimeType = " + mimeType);
                    }
                }
        return validate(info, mimeType, ValidationType.LINKS);
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/validateTurtleLinkSet")
    public Response validateTurtleLinkSet(@FormDataParam("file") InputStream uploadedInputStream) 
            throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("validateTurtleLinkSet called!");
                    if (uploadedInputStream == null){
                        logger.debug("NO uploadedInputStream");
                    } else {
                        try {
                            logger.debug("uploadedInputStream.available = " + uploadedInputStream.available());
                        } catch (IOException ex) {
                            logger.error("unable to get inputStream.available:", ex);
                        }
                    }
                }
        return validate(uploadedInputStream, RDFFormat.TURTLE, ValidationType.LINKS);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/validateTurtleLinkSet")
    public Response validateTurtleLinkSetGet() 
            throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("getValidateTurtleLinkSet called!");
                }
        return validate(null, RDFFormat.TURTLE, ValidationType.LINKS);
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/validateRdfXmlLinkSet")
    public Response validateRdfXmlLinkSet(@FormDataParam("file") InputStream uploadedInputStream) 
            throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("validateRdfXmlLinkSet called!");
                    if (uploadedInputStream == null){
                        logger.debug("NO uploadedInputStream");
                    } else {
                        try {
                            logger.debug("uploadedInputStream.available = " + uploadedInputStream.available());
                        } catch (IOException ex) {
                            logger.error("unable to get inputStream.available:", ex);
                        }
                    }
                }
        return validate(uploadedInputStream, RDFFormat.RDFXML, ValidationType.LINKS);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/validateRdfXmlLinkSet")
    public Response validateRdfXmlLinkSetGet() 
            throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("getValidateRdfXmlLinkSet called!");
                }
        return validate(null, RDFFormat.RDFXML, ValidationType.LINKS);
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/validateNTriplesLinkSet")
    public Response validateNTriplesLinkSet(@FormDataParam("file") InputStream uploadedInputStream) 
            throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("validateNTriplesLinkSet called!");
                    if (uploadedInputStream == null){
                        logger.debug("NO uploadedInputStream");
                    } else {
                        try {
                            logger.debug("uploadedInputStream.available = " + uploadedInputStream.available());
                        } catch (IOException ex) {
                            logger.error("unable to get inputStream.available:", ex);
                        }
                    }
                }
        return validate(uploadedInputStream, RDFFormat.NTRIPLES, ValidationType.LINKS);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/validateNTriplesLinkSet")
    public Response validateNTriplesLinkSetGet() 
            throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("getValidateNTriplesLinkSet called!");
                }
        return validate(null, RDFFormat.NTRIPLES, ValidationType.LINKS);
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Path("/validateMinimum")
    public Response validateMinimum(@FormParam(INFO)String info, 
            @FormParam(MIME_TYPE)String mimeType) throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("validateMinimum called!");
                    if (info == null){
                        logger.debug("NO Info");
                    } else {
                        logger.debug("info length = " + info.length());
                    }
                    if (mimeType == null){
                        logger.debug("NO mimeType");
                    } else {
                        logger.debug("mimeType = " + mimeType);
                    }
                }
        return validate(info, mimeType, ValidationType.LINKSMINIMAL);
    }
    
    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/validateMinimum")
    public Response validateMinimumGet(@QueryParam(INFO)String info, 
            @QueryParam(MIME_TYPE)String mimeType) throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("getValidateMinimum called!");
                    if (info == null){
                        logger.debug("NO Info");
                    } else {
                        logger.debug("info length = " + info.length());
                    }
                    if (mimeType == null){
                        logger.debug("NO mimeType");
                    } else {
                        logger.debug("mimeType = " + mimeType);
                    }
                }
        return validate(info, mimeType, ValidationType.LINKSMINIMAL);
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/validateTurtleMinimum")
    public Response validateTurtleMinimum(@FormDataParam("file") InputStream uploadedInputStream) 
            throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("validateTurtleMinimum called!");
                    if (uploadedInputStream == null){
                        logger.debug("NO uploadedInputStream");
                    } else {
                        try {
                            logger.debug("uploadedInputStream.available = " + uploadedInputStream.available());
                        } catch (IOException ex) {
                            logger.error("unable to get inputStream.available:", ex);
                        }
                    }
                }
        return validate(uploadedInputStream, RDFFormat.TURTLE, ValidationType.LINKSMINIMAL);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/validateTurtleMinimum")
    public Response validateTurtleMinimumGet() 
            throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("getValidateTurtleMinimum called!");
                }
        return validate(null, RDFFormat.TURTLE, ValidationType.LINKSMINIMAL);
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/validateRdfXmlMinimum")
    public Response validateRdfXmlMinimum(@FormDataParam("file") InputStream uploadedInputStream) 
            throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("validateRdfXmlMinimum called!");
                    if (uploadedInputStream == null){
                        logger.debug("NO uploadedInputStream");
                    } else {
                        try {
                            logger.debug("uploadedInputStream.available = " + uploadedInputStream.available());
                        } catch (IOException ex) {
                            logger.error("unable to get inputStream.available:", ex);
                        }
                    }
                }
        return validate(uploadedInputStream, RDFFormat.RDFXML, ValidationType.LINKSMINIMAL);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/validateRdfXmlMinimum")
    public Response validateRdfXmlMinimumGet() 
            throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("getValidateRdfXmlMinimum called!");
                }
        return validate(null, RDFFormat.RDFXML, ValidationType.LINKSMINIMAL);
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/validateNTriplesMinimum")
    public Response validateNTriplesMinimum(@FormDataParam("file") InputStream uploadedInputStream) 
            throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("validateNTriplesMinimum called!");
                    if (uploadedInputStream == null){
                        logger.debug("NO uploadedInputStream");
                    } else {
                        try {
                            logger.debug("uploadedInputStream.available = " + uploadedInputStream.available());
                        } catch (IOException ex) {
                            logger.error("unable to get inputStream.available:", ex);
                        }
                    }
                }
        return validate(uploadedInputStream, RDFFormat.NTRIPLES, ValidationType.LINKSMINIMAL);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/validateNTriplesMinimum")
    public Response validateNTriplesMinimumGet() 
            throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("getValidateNTriplesMinimum called!");
                }
        return validate(null, RDFFormat.NTRIPLES, ValidationType.LINKSMINIMAL);
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Path("/validateRdf")
    public Response validateRdf(@FormParam(INFO)String info, 
            @FormParam(MIME_TYPE)String mimeType) throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("validateRdf called!");
                    if (info == null){
                        logger.debug("NO Info");
                    } else {
                        logger.debug("info length = " + info.length());
                    }
                    if (mimeType == null){
                        logger.debug("NO mimeType");
                    } else {
                        logger.debug("mimeType = " + mimeType);
                    }
                }
        return validate(info, mimeType, ValidationType.ANY_RDF);
    }
    
    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/validateRdf")
    public Response validateRdfGet(@QueryParam(INFO)String info, 
            @QueryParam(MIME_TYPE)String mimeType) throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("getValidateRdf called!");
                    if (info == null){
                        logger.debug("NO Info");
                    } else {
                        logger.debug("info length = " + info.length());
                    }
                    if (mimeType == null){
                        logger.debug("NO mimeType");
                    } else {
                        logger.debug("mimeType = " + mimeType);
                    }
                }
        return validate(info, mimeType, ValidationType.ANY_RDF);
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/validateTurtleRdf")
    public Response validateTurtleRdf(@FormDataParam("file") InputStream uploadedInputStream) 
            throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("validateTurtleRdf called!");
                    if (uploadedInputStream == null){
                        logger.debug("NO uploadedInputStream");
                    } else {
                        try {
                            logger.debug("uploadedInputStream.available = " + uploadedInputStream.available());
                        } catch (IOException ex) {
                            logger.error("unable to get inputStream.available:", ex);
                        }
                    }
                }
        return validate(uploadedInputStream, RDFFormat.TURTLE, ValidationType.ANY_RDF);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/validateTurtleRdf")
    public Response validateTurtleRdfGet() 
            throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("getValidateTurtleRdf called!");
                }
        return validate(null, RDFFormat.TURTLE, ValidationType.ANY_RDF);
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/validateRdfXmlRdf")
    public Response validateRdfXmlRdf(@FormDataParam("file") InputStream uploadedInputStream) 
            throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("validateRdfXmlRdf called!");
                    if (uploadedInputStream == null){
                        logger.debug("NO uploadedInputStream");
                    } else {
                        try {
                            logger.debug("uploadedInputStream.available = " + uploadedInputStream.available());
                        } catch (IOException ex) {
                            logger.error("unable to get inputStream.available:", ex);
                        }
                    }
                }
        return validate(uploadedInputStream, RDFFormat.RDFXML, ValidationType.ANY_RDF);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/validateRdfXmlRdf")
    public Response validateRdfXmlRdfGet() 
            throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("getValidateRdfXmlRdf called!");
                }
        return validate(null, RDFFormat.RDFXML, ValidationType.ANY_RDF);
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/validateNTriplesRdf")
    public Response validateNTriplesRdf(@FormDataParam("file") InputStream uploadedInputStream) 
            throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("validateNTriplesRdf called!");
                    if (uploadedInputStream == null){
                        logger.debug("NO uploadedInputStream");
                    } else {
                        try {
                            logger.debug("uploadedInputStream.available = " + uploadedInputStream.available());
                        } catch (IOException ex) {
                            logger.error("unable to get inputStream.available:", ex);
                        }
                    }
                }
        return validate(uploadedInputStream, RDFFormat.NTRIPLES, ValidationType.ANY_RDF);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/validateNTriplesRdf")
    public Response validateNTriplesRdfGet() 
            throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("getValidateNTriplesRdf called!");
                }
        return validate(null, RDFFormat.NTRIPLES, ValidationType.ANY_RDF);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/validateFile")
    public Response validateFileIndexGet(@Context HttpServletRequest hsr) 
            throws IDMapperException, UnsupportedEncodingException {
                if (logger.isDebugEnabled()){
                    logger.debug("getValidateFileIndex called!");
                }
        StringBuilder sb = topAndSide("File Validators Index");
        sb.append("\n<h1>Validate a File as a Void Description.</h1>");
        addFileLine(sb,  ValidationType.VOID, RDFFormat.TURTLE);
        addFileLine(sb,  ValidationType.VOID, RDFFormat.RDFXML);
        addFileLine(sb,  ValidationType.VOID, RDFFormat.NTRIPLES);
        sb.append("\n<h1>Validate a File as a Linkset.</h1>");
        addFileLine(sb,  ValidationType.LINKS, RDFFormat.TURTLE);
        addFileLine(sb,  ValidationType.LINKS, RDFFormat.RDFXML);
        addFileLine(sb,  ValidationType.LINKS, RDFFormat.NTRIPLES);
        if (IpConfig.isAdminIPAddress(hsr.getRemoteAddr())){
            sb.append("\n<h1>Validate a File as the minimum to load a linkset.</h1>");
            addFileLine(sb,  ValidationType.LINKSMINIMAL, RDFFormat.TURTLE);
            addFileLine(sb,  ValidationType.LINKSMINIMAL, RDFFormat.RDFXML);
            addFileLine(sb,  ValidationType.LINKSMINIMAL, RDFFormat.NTRIPLES);
            sb.append("\n<h1>Validate a File as RDF.</h1>");
            addFileLine(sb,  ValidationType.ANY_RDF, RDFFormat.TURTLE);
            addFileLine(sb,  ValidationType.ANY_RDF, RDFFormat.RDFXML);
            addFileLine(sb,  ValidationType.ANY_RDF, RDFFormat.NTRIPLES);
        }
        sb.append(END);
        return Response.ok(sb.toString(), MediaType.TEXT_HTML).build();
    }

    private Response validate(String info, String mimeType, ValidationType validationType) throws IDMapperException, UnsupportedEncodingException {
       String report = null;
       try{
            if (info != null && !info.isEmpty()){
                RDFFormat format = getRDFFormatByMimeType(mimeType);
                report = linksetInterface.validateString("Webservice Call", info, format, StoreType.TEST, validationType, true);
            }
        } catch (Exception e){
            report = e.toString();
        }
        StringBuilder sb = topAndSide(validationType.getName() + " Validator");
        addForm(sb, validationType, info, report);
        sb.append(END);
        return Response.ok(sb.toString(), MediaType.TEXT_HTML).build();
    }

    private Response validate(InputStream input, RDFFormat format, ValidationType validationType) throws IDMapperException, UnsupportedEncodingException {
       String report = null;
       try{
            if (input != null && input.available() > 10){
                report = linksetInterface.validateInputStream("Webservice Call", input, format, StoreType.TEST, validationType, true);
            }
        } catch (Exception e){
            report = e.toString();
        }
        StringBuilder sb = topAndSide(validationType.getName() + " Validator");
        addFileForm(sb, validationType, report);
        sb.append(END);
        return Response.ok(sb.toString(), MediaType.TEXT_HTML).build();
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/getMappingInfo")
    public Response getMappingInfo() throws IDMapperException, UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        List<MappingSetInfo> mappingSetInfos = urlMapper.getMappingSetInfos();
        Collections.sort(mappingSetInfos, this);
        sb.append("<?xml version=\"1.0\"?>");
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
                + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">");
        sb.append("<meta http-equiv=\"content-type\" content=\"text/html; charset=ISO-8859-1\"/>");
        sb.append("<head><title>OPS IMS</title></head><body>");
        sb.append("<h1>Open PHACTS Identity Mapping Counter per NameSpaces</h1>");
        sb.append("\n<p>Warning there many not be Distint mappings but just a sum of the mappings from all mapping files.");
        sb.append("So if various sources include the same mapping it will be counted multiple times. </p>");
        sb.append("\n<p>");
        sb.append("<table border=\"1\">");
        sb.append("<tr>");
        sb.append("<th>Source Data Source</th>");
        sb.append("<th>Target Data source</th>");
        sb.append("<th>Sum of Mappings</th>");
        sb.append("<th>Id</th>");
        sb.append("<th>Transative</th>");
        sb.append("</tr>");
        for (MappingSetInfo info:mappingSetInfos){
            sb.append("<tr>");
            sb.append("<td><a href=\"");
            sb.append(RdfConfig.getTheBaseURI());
            sb.append("dataSource/");
            sb.append(info.getSourceSysCode());
            sb.append("\">");
            sb.append(info.getSourceSysCode());
            sb.append("</a></td>");
            sb.append("<td><a href=\"");
            sb.append(RdfConfig.getTheBaseURI());
            sb.append("dataSource/");
            sb.append(info.getTargetSysCode());
            sb.append("\">");
            sb.append(info.getTargetSysCode());
            sb.append("</a></td>");
            sb.append("<td align=\"right\">");
            sb.append(formatter.format(info.getNumberOfLinks()));
            sb.append("</td>");
            sb.append("<td><a href=\"");
            sb.append(RdfConfig.getTheBaseURI());
            sb.append("mappingSet/");
            sb.append(info.getId());
            sb.append("\">");
            sb.append(info.getId());
            sb.append("</a></td>");
            sb.append("<td>");
            sb.append(info.isTransitive());
            sb.append("</td>");
            sb.append("</tr>");
        }
        sb.append("</table>"); 
        return Response.ok(sb.toString(), MediaType.TEXT_HTML).build();
    }
    
    @Override
    public int compare(MappingSetInfo o1, MappingSetInfo o2) {
        int test = o1.getSourceSysCode().compareTo(o2.getSourceSysCode());
        if (test != 0) return test;
        return o1.getTargetSysCode().compareTo(o2.getTargetSysCode());
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/getMappingTotal")
    public Response mappingTotal() throws IDMapperException, UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        List<MappingSetInfo> rawProvenaceinfos = urlMapper.getMappingSetInfos();
        SourceTargetCounter sourceTargetCounter = new SourceTargetCounter(rawProvenaceinfos);
        sb.append("<?xml version=\"1.0\"?>");
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
                + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">");
        sb.append("<meta http-equiv=\"content-type\" content=\"text/html; charset=ISO-8859-1\"/>");
        sb.append("<head><title>OPS IMS</title></head><body>");
        sb.append("<h1>Open PHACTS Identity Mapping Counter per NameSpaces</h1>");
        sb.append("\n<p>Warning there many not be Distint mappings but just a sum of the mappings from all mapping files.");
        sb.append("So if various sources include the same mapping it will be counted multiple times. </p>");
        sb.append("\n<p>");
        sb.append("<table border=\"1\">");
        sb.append("<tr>");
        sb.append("<th>Source Data Source</th>");
        sb.append("<th>Target Data Source</th>");
        sb.append("<th>Sum of Mappings</th>");
        sb.append("</tr>");
        for (MappingSetInfo info:sourceTargetCounter.getSummaryInfos()){
            sb.append("<tr>");
            sb.append("<td><a href=\"");
            sb.append(RdfConfig.getTheBaseURI());
            sb.append("dataSource/");
            sb.append(info.getSourceSysCode());
            sb.append("\">");
            sb.append(info.getSourceSysCode());
            sb.append("</a></td>");
            sb.append("<td><a href=\"");
            sb.append(RdfConfig.getTheBaseURI());
            sb.append("dataSource/");
            sb.append(info.getTargetSysCode());
            sb.append("\">");
            sb.append(info.getTargetSysCode());
            sb.append("</a></td>");
            sb.append("<td align=\"right\">");
            sb.append(formatter.format(info.getNumberOfLinks()));
            sb.append("</td>");
            sb.append("</tr>");
        }
        sb.append("</table>"); 
        return Response.ok(sb.toString(), MediaType.TEXT_HTML).build();
    }
    
    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/graphviz")
    public Response graphvizDot() throws IDMapperException, UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        List<MappingSetInfo> rawProvenaceinfos = urlMapper.getMappingSetInfos();
        SourceTargetCounter sourceTargetCounter = new SourceTargetCounter(rawProvenaceinfos);
        sb.append("digraph G {");
        for (MappingSetInfo info:sourceTargetCounter.getSummaryInfos()){
            if (info.getSourceSysCode().compareTo(info.getTargetSysCode()) < 0 ){
                sb.append("\"");
                sb.append(info.getSourceSysCode());
                sb.append("\" -> \"");
                sb.append(info.getTargetSysCode());
                sb.append("\" [dir = both, label=\"");
                sb.append(formatter.format(info.getNumberOfLinks())); 
                sb.append("\"");
                if (info.isTransitive()){
                    sb.append(", style=dashed");
                }
                sb.append("];\n");
            }
        }
        sb.append("}"); 
        return Response.ok(sb.toString(), MediaType.TEXT_HTML).build();
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/mappingSet")
    public String mappingSet() throws IDMapperException {
        throw new BridgeDBException("Parameter id is missing");
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/mappingSet/{id}")
    public String mappingSet(@PathParam("id") String idString) throws IDMapperException {
        if (idString == null || idString.isEmpty()){
            throw new BridgeDBException("Parameter id is missing!");
        }
        Integer id = Integer.parseInt(idString);
        return new RdfReader(StoreType.LIVE).getLinksetRDF(id);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/linkset")
    public String linkset() throws IDMapperException {
        throw new BridgeDBException("Parameter id is missing");
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/linkset/{id}")
    public String linksetSet(@PathParam("id") String idString) throws IDMapperException {
        if (idString == null || idString.isEmpty()){
            throw new BridgeDBException("Parameter id is missing!");
        }
        Integer id = Integer.parseInt(idString);
        return new RdfReader(StoreType.LIVE).getLinksetRDF(id);
    }
    
    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/linkset/{id}/{resource}")
    public String linksetSet(@PathParam("id") String idString, @PathParam("resource") String resource) throws IDMapperException {
        throw new BridgeDBException("id= "+ idString + " resource = " + resource);
        //if (idString == null || idString.isEmpty()){
       //     throw new IDMapperException("Parameter id is missing!");
        //}
        //Integer id = Integer.parseInt(idString);
        //return new RdfReader(StoreType.LIVE).getLinksetRDF(id);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/void")
    public String voidInfo() throws IDMapperException {
        throw new BridgeDBException("Parameter id is missing");
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Path("/void/{id}")
    public String voidInfo(@PathParam("id") String idString) throws IDMapperException {
        if (idString == null || idString.isEmpty()){
            throw new BridgeDBException("Parameter id is missing");
        }
        Integer id = Integer.parseInt(idString);
        return new RdfReader(StoreType.LIVE).getVoidRDF(id);
    }

    private StringBuilder topAndSide(String header){
        StringBuilder sb = new StringBuilder(HEADER);
        sb.append(BODY);
        sb.append(TOP_LEFT);
        sb.append(header);
        sb.append(TOP_RIGHT);
        sb.append(SIDE_BAR);
        return sb;
    }
    
    private void addForm(StringBuilder sb, ValidationType validationType, String info, String report) throws BridgeDBException{
        addValidationExplanation(sb, validationType);
        addFormStart(sb,  validationType);
        if (report != null){
            addReport(sb, validationType, report);
        }
        //sb.append(FORM_OUTPUT_FORMAT);
        sb.append(FORM_MINE_TYPE);
        sb.append(FORM_INFO_START);
        if (info != null && !info.isEmpty()){
            sb.append(info);
        }
            
        sb.append(FORM_INFO_END);
        sb.append("\n<p>");
        sb.append(FORM_SUBMIT);        
        sb.append(FORM_NOTE);      
        sb.append("</p>");
    }
    
    private void addFileForm(StringBuilder sb, ValidationType validationType, String report) throws BridgeDBException{
        addValidationExplanation(sb, validationType);
        if (report != null){
            addReport(sb, validationType, report);
        }
        addFileLine(sb,  validationType, RDFFormat.TURTLE);
        addFileLine(sb,  validationType, RDFFormat.RDFXML);
        addFileLine(sb,  validationType, RDFFormat.NTRIPLES);
    }

    private void addValidationExplanation(StringBuilder sb, ValidationType validationType) throws BridgeDBException{
        sb.append("\n<p>Use this page to validate a ");
        switch (validationType){
            case VOID: {
                sb.append("VOID descripition.");
                break;
            }
            case LINKS: {
                sb.append("Linkset.");
                break;
            }
            case LINKSMINIMAL: {
                sb.append("linkset you are too lazy to add a full header to.");
                sb.append("<br>WARNING: Loading with Minimal void does not excuss you from providing a full header later.");
                break;
            }
            case ANY_RDF: {
                sb.append("Any RDF which will act as a parent for void or linkset.");
                sb.append("<br>WARNING: Loading RDF void does not excuss you from providing a full void later.");
                break;
            } default:{
                throw new BridgeDBException("Unexpected validationType" + validationType);
            }
        }
        sb.append(".</p>");       
        sb.append("\n<p>This is an early prototype and subject to change!</p> ");
    }

    private void addFormStart(StringBuilder sb, ValidationType validationType) throws BridgeDBException{
        sb.append("<form method=\"post\" action=\"/OPS-IMS/validate");
        sb.append(validationType.getName());
        sb.append("\">");        
    }
    
    private void addFileLine(StringBuilder sb, ValidationType validationType, RDFFormat format) throws BridgeDBException{
        addFormStart(sb, validationType, format);
        sb.append("Select ");
        sb.append(format.getName());
        sb.append(" to validate as a ");
        sb.append(validationType.getName());
        sb.append("<input type=\"file\" name=\"file\" size=\"45\" />");
        sb.append(FORM_SUBMIT);   
        sb.append("<br>");
    }
    
    private void addFormStart(StringBuilder sb, ValidationType validationType, RDFFormat format) throws BridgeDBException{
        String formatSt;
        if (format == RDFFormat.TURTLE){
            formatSt = "Turtle";
        } else if (format == RDFFormat.RDFXML){
            formatSt = "RdfXml";
        } else if (format == RDFFormat.NTRIPLES){
            formatSt = "NTriples";
        } else {
            throw new BridgeDBException("Unexpected format" + format);
        }
        sb.append("\n<form method=\"post\" action=\"/OPS-IMS/");
        sb.append("validate");
        sb.append(formatSt);
        sb.append(validationType.getName());
        sb.append("\" enctype=\"multipart/form-data\">");        
    }
    
    private void addReport(StringBuilder sb, ValidationType validationType, String report){
        int lines = 1;
        for (int i=0; i < report.length(); i++) {
            if (report.charAt(i) == '\n') lines++;
        }
        sb.append("<h2>Report as a ");
        sb.append(validationType.getName());
        sb.append("</h2>");
        sb.append("\n<p><textarea readonly style=\"width:100%;\" rows=");
        sb.append(lines);
        sb.append(">");
        sb.append(report);
        sb.append("</textarea></p>\n");       
    }
    
    private final String HEADER_START = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
            + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">"
            + "<html xmlns:v=\"urn:schemas-microsoft-com:vml\">\n"
            + "<head>\n"
            + " <title>"
            + "     Manchester University OpenPhacts Void Validator"
            + "	</title>\n"
            + "	<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\"></meta>\n"
            + "	<script>"
            + "		function getObj(id) {"
            + "			return document.getElementById(id)"
            + "		}"
            + "		function DHTML_TextHilight(id) {"
            + "			getObj(id).classNameOld = getObj(id).className;"
            + "			getObj(id).className = getObj(id).className + \"_hilight\";"
            + "		}"
            + "		function DHTML_TextRestore(id) {"
            + "			if (getObj(id).classNameOld != \"\")"
            + "				getObj(id).className = getObj(id).classNameOld;"
            + "		}"
            + "	</script>\n";
    private final String TOGGLER ="<script language=\"javascript\">\n"
            + "function getItem(id)\n"
            + "{\n"
            + "    var itm = false;\n"
            + "    if(document.getElementById)\n"
            + "        itm = document.getElementById(id);\n"
            + "    else if(document.all)\n"
            + "        itm = document.all[id];\n"
            + "     else if(document.layers)\n"
            + "        itm = document.layers[id];\n"
            + "    return itm;\n"
            + "}\n\n"
            + "function toggleItem(id)\n"
            + "{\n"
            + "    itm = getItem(id);\n"
            + "    if(!itm)\n"
            + "        return false;\n"
            + "    if(itm.style.display == 'none')\n"
            + "        itm.style.display = '';\n"
            + "    else\n"
            + "        itm.style.display = 'none';\n"
            + "    return false;\n"
            + "}\n\n"
            + "function hideDetails()\n"
            + "{\n"
            + "     toggleItem('ops')\n"
            + "     toggleItem('sparql')\n"
            + "     return true;\n"
            + "}\n\n"
            + "</script>\n";
    private final String HEADER_END = "	<style type=\"text/css\">"
            + "		.texthotlink, .texthotlink_hilight {"
            + "			width: 150px;"
            + "			font-size: 85%;"
            + "			padding: .25em;"
            + "			cursor: pointer;"
            + "			color: black;"
            + "			font-family: Arial, sans-serif;"
            + "		}"
            + "		.texthotlink_hilight {"
            + "			background-color: #fff6ac;"
            + "		}"
            + "		.menugroup {"
            + "			font-size: 90%;"
            + "			font-weight: bold;"
            + "			padding-top: .25em;"
            + "		}"
            + "		input { background-color: #EEEEFF; }"
            + "		body, td {"
            + "			background-color: white;"
            + "			font-family: sans-serif;"
            + "		}"
            + "	</style>\n"
            + "</head>\n";            
    private final String HEADER = HEADER_START + HEADER_END;
    private final String TOGGLE_HEADER = HEADER_START + TOGGLER + HEADER_END;
    private final String BODY ="<body style=\"margin: 0px\">";
    private final String TOP_LEFT ="	<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\">\n"
            + "		<tr valign=\"top\">\n"
            + "			<td style=\"background-color: white;\">"
            + "				<a href=\"http://www.openphacts.org/\">"
            + "                 <img style=\"border: none; padding: 0px; margin: 0px;\" "
            + "                     src=\"http://www.openphacts.org/images/stories/banner.jpg\" "
            + "                     alt=\"Open PHACTS\" height=\"50\">"
            + "                 </img>"
            + "             </a>"
            + "			</td>\n"
            + "			<td style=\"font-size: 200%; font-weight: bold; font-family: Arial;\">\n";
    private final String TOP_RIGHT = "         </td>"
            + "			<td style=\"background-color: white;\">"
            + "				<a href=\"http://www.cs.manchester.ac.uk//\">"
            + "                 <img style=\"border: none; padding: 0px; margin: 0px;\" align=\"right\" "
            + "                     src=\"http://www.manchester.ac.uk/media/corporate/theuniversityofmanchester/assets/images/logomanchester.gif\" "
            + "                    alt=\"The University of Manchester\" height=\"50\">"
            + "                 </img>"
            + "             </a>"
            + "			</td>"
            + "		</tr>"
            + "	</table>";
    private final String SIDE_BAR = "	<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\">"
            + "		<tr valign=\"top\">"
            + "			<td style=\"border-top: 1px solid #D5D5FF\">"
            + "				<div class=\"menugroup\">Query Expander</div>"
            + "				<div id=\"menuQueryExpanderHome_text\" class=\"texthotlink\" "
            + "                   onmouseout=\"DHTML_TextRestore('menuQueryExpanderHome_text'); return true; \" "
            + "                   onmouseover=\"DHTML_TextHilight('menuQueryExpanderHome_text'); return true; \" "
            + "                   onclick=\"document.location = &quot;/QueryExpander&quot;;\">Home</div>"
            + "				<div id=\"menuQueryExpanderAPI_text\" class=\"texthotlink\" "
            + "                   onmouseout=\"DHTML_TextRestore('menuQueryExpanderAPI_text'); return true; \" "
            + "                   onmouseover=\"DHTML_TextHilight('menuQueryExpanderAPI_text'); return true; \" "
            + "                   onclick=\"document.location = &quot;/QueryExpander/api&quot;;\">API</div>"
            + "				<div id=\"menuQueryExpanderExamples_text\" class=\"texthotlink\" "
            + "                   onmouseout=\"DHTML_TextRestore('menuQueryExpanderExamples_text'); return true; \" "
            + "                   onmouseover=\"DHTML_TextHilight('menuQueryExpanderExamples_text'); return true; \" "
            + "                   onclick=\"document.location = &quot;/QueryExpander/examples&quot;;\">Examples</div>"
            + "				<div id=\"menuQueryExpanderURISpacesPerGraph_text\" class=\"texthotlink\" "
            + "                   onmouseout=\"DHTML_TextRestore('menuQueryExpanderURISpacesPerGraph_text'); return true; \" "
            + "                   onmouseover=\"DHTML_TextHilight('menuQueryExpanderURISpacesPerGraph_text'); return true; \" "
            + "                   onclick=\"document.location = &quot;/QueryExpander/URISpacesPerGraph&quot;;\">"
            + "                   URISpaces per Graph</div>"
            + "				<div id=\"menuQueryExpanderMapURI_text\" class=\"texthotlink\" "
            + "                   onmouseout=\"DHTML_TextRestore('menuQueryExpanderMapURI_text'); return true; \" "
            + "                   onmouseover=\"DHTML_TextHilight('menuQueryExpanderMapURI_text'); return true; \" "
            + "                   onclick=\"document.location = &quot;/QueryExpander/mapURI&quot;;\">"
            + "                   Check Mapping for an URI</div>"            
            + "				<div class=\"menugroup\">OPS Identity Mapping Service</div>"
            + "				<div id=\"menuOpsHome_text\" class=\"texthotlink\" "
            + "                   onmouseout=\"DHTML_TextRestore('menuOpsHome_text'); return true; \" "
            + "                   onmouseover=\"DHTML_TextHilight('menuOpsHome_text'); return true; \" "
            + "                   onclick=\"document.location = &quot;/OPS-IMS&quot;;\">Home</div>"
            + "				<div id=\"menuOpsInfo_text\" class=\"texthotlink\" "
            + "                   onmouseout=\"DHTML_TextRestore('menuOpsInfo_text'); return true; \" "
            + "                   onmouseover=\"DHTML_TextHilight('menuOpsInfo_text'); return true; \" "
            + "                   onclick=\"document.location = &quot;/OPS-IMS/getMappingInfo&quot;;\">"
            + "                   Mappings Summary</div>"
            + "				<div id=\"menuGraphviz_text\" class=\"texthotlink\" "
            + "                   onmouseout=\"DHTML_TextRestore('menuGraphviz_text'); return true; \" "
            + "                   onmouseover=\"DHTML_TextHilight('menuGraphviz_text'); return true; \" "
            + "                   onclick=\"document.location = &quot;/OPS-IMS/graphviz&quot;;\">"
            + "                   Mappings Summary in Graphviz format</div>"
            + "				<div id=\"menuOpsApi_text\" class=\"texthotlink\" "
            + "                   onmouseout=\"DHTML_TextRestore('menuOpsApi_text'); return true; \" "
            + "                   onmouseover=\"DHTML_TextHilight('menuOpsApi_text'); return true; \" "
            + "                   onclick=\"document.location = &quot;/OPS-IMS/api&quot;;\">API</div>"
            + "				<div id=\"menuOpsValidateVoid_text\" class=\"texthotlink\" "
            + "                   onmouseout=\"DHTML_TextRestore('menuOpsValidateVoid_text'); return true; \" "
            + "                   onmouseover=\"DHTML_TextHilight('menuOpsValidateVoid_text'); return true; \" "
            + "                   onclick=\"document.location = &quot;/OPS-IMS/validateVoid&quot;;\">Validate Void</div>"
            + "				<div id=\"menuOpsValidateLinkSet_text\" class=\"texthotlink\" "
            + "                   onmouseout=\"DHTML_TextRestore('menuOpsValidateLinkSet_text'); return true; \" "
            + "                   onmouseover=\"DHTML_TextHilight('menuOpsValidateLinkSet_text'); return true; \" "
            + "                   onclick=\"document.location = &quot;/OPS-IMS/validateLinkSet&quot;;\">Validate LinkSet</div>"
            + "			</td>"
            + "			<td width=\"5\" style=\"border-right: 1px solid #D5D5FF\"></td>"
            + "			<td style=\"border-top: 1px solid #D5D5FF; width:100%\">";
    private final String FORM_OUTPUT_FORMAT = " \n<p>Output Format:"
            + "     <select size=\"1\" name=\"format\">"
            + "         <option value=\"html\">HTML page</option>"
            + "         <option value=\"xml\">XML/JASON</option>"
            + " 	</select>"
            + " </p>";
    private final String FORM_MINE_TYPE = " \n<p>Mime Type:"
            + "     <select size=\"1\" name=\"mimeType\">"
            + "         <option value=\"application/x-turtle\">Turtle (mimeType=application/x-turtle; ext=ttl)</option>"
            + "         <option value=\"text/plain\">N-Triples (mimeType=text/plain; ext=nt)</option>"
            + "         <option value=\"application/rdf+xml\">RDF/XML (mimeType=application/rdf+xml; ext=rdf, rdfs, owl, xml</option>"
            + " 	</select>"
            + " </p>";
    private final String FORM_INFO_START = "\n<p><textarea rows=\"15\" name=\"info\" style=\"width:100%; background-color: #EEEEFF;\">";
    private final String FORM_INFO_END = "</textarea></p>";
    private final String FORM_SUBMIT = " <input type=\"submit\" value=\"Validate!\"></input></form>";
    private final String FORM_NOTE ="    Note: If the new page does not open click on the address and press enter</p>"
            + "</form>";
    private final String URI_MAPPING_FORM = "<form method=\"get\" action=\"/QueryExpander/mapURI\">"
            + " \n<p>Input URI (URI to be looked up in Identity Mapping Service.)"
            + "     (see <a href=\"/QueryExpander/api#inputURI\">API</a>)</p>"
            + " \n<p><input type=\"text\" name=\"inputURI\" style=\"width:100%\"/></p>"
            + " \n<p>Graph/Context (Graph value to limit the returned URIs)"
            + "     (see <a href=\"/QueryExpander/api#graph\">API</a>)</p>"
            + " \n<p><input type=\"text\" name=\"graph\" style=\"width:100%\"/></p>"
            + " \n<p><input type=\"submit\" value=\"Expand!\"></input> "
            + "    Note: If the new page does not open click on the address and press enter</p>"
            + "</form>";
    private final String MAIN_END = "			</td>"
            + "		</tr>"
            + "	</table>"
            + "	<div style=\"border-top: 1px solid #D5D5FF; padding: .5em; font-size: 80%;\">"
            + "		This site is run by <a href=\"https://wiki.openphacts.org/index.php/User:Christian\">Christian Brenninkmeijer</a>."
            + "	</div>";
    private final String BODY_END = "</body>"
            + "</html>";
    private final String END = MAIN_END + BODY_END;

    //Code from  http://www.mkyong.com/webservices/jax-rs/file-upload-example-in-jersey/
    @GET
	@Path("/checkIpAddress")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response checkIpAddress(@Context HttpServletRequest hsr) throws IOException, IDMapperException {
                if (logger.isDebugEnabled()){
                    logger.debug("checkIpAddress called");
                }
                logger.debug("Client IP = " + hsr.getRemoteAddr()); 
       
        StringBuilder sb = new StringBuilder();
        StringBuilder sbInnerPure;
        StringBuilder sbInnerEncoded;

        sb.append("<?xml version=\"1.0\"?>");
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
                + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">");
        sb.append("<meta http-equiv=\"content-type\" content=\"text/html; charset=ISO-8859-1\"/>");
        sb.append("<head><title>OPS IMS</title></head><body>");
        sb.append("<h1>test</h1>");
        sb.append("\n<p>IP Address:");
        sb.append(hsr.getRemoteAddr());
        sb.append("</P>");
        String owner = IpConfig.checkIPAddress(hsr.getRemoteAddr());
        if (owner == null){
            sb.append("<h1>Unknown</h1>");
            sb.append("Sorry you are not known to this system.");
            sb.append("<br>You access attempt has been logged.");
            sb.append("<br>Please register your IP address by contacting an Administrator.");
        } else {
            sb.append("<h1>Welcome ");
            sb.append(owner);
            sb.append("</h1>");            
        }
        return Response.ok(sb.toString(), MediaType.TEXT_HTML).build();
	}

    //Code from  http://www.mkyong.com/webservices/jax-rs/file-upload-example-in-jersey/
    @POST
	@Path("/uploadTest")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile(
        //TODO work out why the FormDataContentDisposition is null
		 @FormDataParam("file") InputStream uploadedInputStream,
         @FormDataParam("file") FormDataContentDisposition fileDetail,
         @Context HttpServletRequest hsr
       ) throws IOException {
                if (logger.isDebugEnabled()){
                    logger.debug("uploadFile called");
                    if (uploadedInputStream == null){
                        logger.debug("NO uploadedInputStream");
                    } else {
                        try {
                            logger.debug("uploadedInputStream.available = " + uploadedInputStream.available());
                        } catch (IOException ex) {
                            logger.error("unable to get inputStream.available:", ex);
                        }
                    }
                    if (fileDetail == null){
                        logger.debug("fileDetail == null");
                    } else {
                        logger.debug("fileDetail = " + fileDetail);
                    }
                }
      
        StringBuilder sb = new StringBuilder();
        StringBuilder sbInnerPure;
        StringBuilder sbInnerEncoded;

        sb.append("<?xml version=\"1.0\"?>");
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
                + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">");
        sb.append("<meta http-equiv=\"content-type\" content=\"text/html; charset=ISO-8859-1\"/>");
        sb.append("<head><title>OPS IMS</title></head><body>");
        sb.append("<h1>test</h1>");
        sb.append("\n<p>File name:");
        sb.append(fileDetail);
        sb.append("\n<p>The IP Address:");
        sb.append(hsr.getRemoteAddr());
        sb.append("</P>");
        
        InputStreamReader reader = new InputStreamReader(uploadedInputStream);
        BufferedReader buffer = new BufferedReader(reader);
        int count = 0;
        while (buffer.ready() && count < 5){
            sb.append("<br>");
            sb.append(buffer.readLine());
            count++;
        }
        sb.append("<br>");
        return Response.ok(sb.toString(), MediaType.TEXT_HTML).build();
	}

    @POST
	@Path("/uploadTest2")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile2(
        //TODO work out why the FormDataContentDisposition is null
		 @FormDataParam("file") InputStream uploadedInputStream,
         @FormDataParam("file") FormDataContentDisposition fileDetail,
         @FormParam(MIME_TYPE)String mimeType
       ) throws IOException {
 
                if (logger.isDebugEnabled()){
                    logger.debug("uploadFile2 called");
                    if (uploadedInputStream == null){
                        logger.debug("NO uploadedInputStream");
                    } else {
                        try {
                            logger.debug("uploadedInputStream.available = " + uploadedInputStream.available());
                        } catch (IOException ex) {
                            logger.error("unable to get inputStream.available:", ex);
                        }
                    }
                    if (fileDetail == null){
                        logger.debug("fileDetail == null");
                    } else {
                        logger.debug("fileDetail = " + fileDetail);
                    }
                    if (mimeType == null){
                        logger.debug("NO mimeType");
                    } else {
                        logger.debug("mimeType = " + mimeType);
                    }

                }
       
        StringBuilder sb = new StringBuilder();
        StringBuilder sbInnerPure;
        StringBuilder sbInnerEncoded;

        sb.append("<?xml version=\"1.0\"?>");
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
                + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">");
        sb.append("<meta http-equiv=\"content-type\" content=\"text/html; charset=ISO-8859-1\"/>");
        sb.append("<head><title>OPS IMS</title></head><body>");
        sb.append("<h1>test</h1>");
        sb.append("\n<p>File name:");
        sb.append(fileDetail);
        sb.append("</P>");
        InputStreamReader reader = new InputStreamReader(uploadedInputStream);
        BufferedReader buffer = new BufferedReader(reader);
        int count = 0;
        while (buffer.ready() && count < 5){
            sb.append("<br>");
            sb.append(buffer.readLine());
            count++;
        }
        sb.append(uploadedInputStream.toString());
        return Response.ok(sb.toString(), MediaType.TEXT_HTML).build();
	}
}


