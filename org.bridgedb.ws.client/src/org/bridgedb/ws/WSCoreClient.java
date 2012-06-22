/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bridgedb.ws;

import java.util.List;

import com.sun.jersey.api.client.Client;

import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.bridgedb.IDMapperException;
import org.bridgedb.ws.bean.CapabilitiesBean;
import org.bridgedb.ws.bean.DataSourceBean;
import org.bridgedb.ws.bean.FreeSearchSupportedBean;
import org.bridgedb.ws.bean.MappingSupportedBean;
import org.bridgedb.ws.bean.PropertyBean;
import org.bridgedb.ws.bean.URLExistsBean;
import org.bridgedb.ws.bean.URLMappingBean;
import org.bridgedb.ws.bean.URLSearchBean;
import org.bridgedb.ws.bean.XrefMapBean;
import org.bridgedb.ws.bean.XrefBean;
import org.bridgedb.ws.bean.XrefExistsBean;

/**
 *
 * @author Christian
 */
public class WSCoreClient implements WSCoreInterface{

    protected final String serviceAddress;

    protected final WebResource webResource;

    public WSCoreClient(String serviceAddress) {
        this.serviceAddress = serviceAddress;
        ClientConfig config = new DefaultClientConfig();
        Client client = Client.create(config);
        webResource = client.resource(serviceAddress);        
    }
    
    //*** URLMapper functions methods *****
    @Override
    public List<URLMappingBean> mapByURLs(List<String> sourceURL, List<String> provenanceId, List<String> targetNameSpace) 
            throws IDMapperException {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        for (String asourceURL:sourceURL){
            params.add("sourceURL", asourceURL);
        }
        for (String id:provenanceId){
            params.add("provenanceId", id);
        }
        for (String target:targetNameSpace){
            params.add("targetNameSpace", target);
        }
        //Make service call
        List<URLMappingBean> result = 
                webResource.path("mapByURLs")
                .queryParams(params)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .get(new GenericType<List<URLMappingBean>>() {});
        return result;
    }

    @Override
    public URLExistsBean urlExists(String URL) throws IDMapperException {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.add("URL", URL);
        //Make service call
        URLExistsBean result = 
                webResource.path("URLExists")
                .queryParams(params)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .get(new GenericType<URLExistsBean>() {});
        return result;
    }

    @Override
    public URLSearchBean URLSearch(String text, Integer limit) throws IDMapperException {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.add("text", text);
        params.add("limit", limit.toString());
        //Make service call
        URLSearchBean result = 
                webResource.path("URLSearch")
                .queryParams(params)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .get(new GenericType<URLSearchBean>() {});
        return result;
    }
    
    @Override
    public List<XrefBean> freeSearch(String text, Integer limit) throws IDMapperException {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.add("text", text);
        params.add("limit", limit.toString());
        //Make service call
        List<XrefBean> result = 
                webResource.path("freeSearch")
                .queryParams(params)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .get(new GenericType<List<XrefBean>>() {});
        return result;
    }

    @Override
    public DataSourceBean getDataSoucre(String code) throws IDMapperException {
        //Make service call
        DataSourceBean result = 
                webResource.path("getDataSource/" + code)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .get(new GenericType<DataSourceBean>() {});
        return result;
    }

    @Override
    public List<PropertyBean> getKeys() {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        //Make service call
        List<PropertyBean> result = 
                webResource.path("getKeys")
                .queryParams(params)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .get(new GenericType<List<PropertyBean>>() {});
        return result;
    }

    @Override
    public PropertyBean getProperty(String key) {
        //Make service call
        PropertyBean result = 
                webResource.path("property/" + key)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .get(new GenericType<PropertyBean>() {});
        return result;
    }

    @Override
    public List<DataSourceBean> getSupportedSrcDataSources() throws IDMapperException {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        //Make service call
        List<DataSourceBean> result = 
                webResource.path("getSupportedSrcDataSources")
                .queryParams(params)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .get(new GenericType<List<DataSourceBean>>() {});
        return result;
    }

    @Override
    public List<DataSourceBean> getSupportedTgtDataSources() throws IDMapperException {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        //Make service call
        List<DataSourceBean> result = 
                webResource.path("getSupportedTgtDataSources")
                .queryParams(params)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .get(new GenericType<List<DataSourceBean>>() {});
        return result;
    }

    @Override
    public FreeSearchSupportedBean isFreeSearchSupported() {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        //Make service call
        FreeSearchSupportedBean result = 
                webResource.path("isFreeSearchSupported")
                .queryParams(params)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .get(new GenericType<FreeSearchSupportedBean>() {});
        return result;
    }

    @Override
    public MappingSupportedBean isMappingSupported(String sourceSysCode, String targetCode) throws IDMapperException {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.add("sourceSysCode", sourceSysCode);
        params.add("targetCode", targetCode);
        //Make service call
        MappingSupportedBean result = 
                webResource.path("isMappingSupported")
                .queryParams(params)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .get(new GenericType<MappingSupportedBean>() {});
        return result;
    }

    @Override
    public List<XrefMapBean> mapID(List<String> id, List<String> scrCode, List<String> provenanceId, 
            List<String> targetCodes) throws IDMapperException {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        for (String one:id){
            params.add("id", one);
        }
        for (String one:scrCode){
            params.add("code", one);
        }
        for (String provId:provenanceId){
            params.add("provenanceId", provId);
        }
        for (String target:targetCodes){
            params.add("targetCode", target);
        }
        //Make service call
        List<XrefMapBean> result = 
                webResource.path("mapID")
                .queryParams(params)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .get(new GenericType<List<XrefMapBean>>() {});
         return result;
    }

    @Override
    public XrefExistsBean xrefExists(String id, String scrCode) throws IDMapperException {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.add("id", id);
        params.add("code", scrCode);
        //Make service call
        XrefExistsBean result = 
                webResource.path("xrefExists")
                .queryParams(params)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .get(new GenericType<XrefExistsBean>() {});
        return result;
    }

    @Override
    public CapabilitiesBean getCapabilities() {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        //Make service call
        CapabilitiesBean result = 
                webResource.path("getCapabilities")
                .queryParams(params)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .get(new GenericType<CapabilitiesBean>() {});
        return result;
    }

}