// BridgeDb,
// An abstraction layer for identifer mapping services, both local and online.
// Copyright 2006-2009 BridgeDb developers
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

package org.bridgedb.webservice.biomart.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

import java.net.URL;
import java.net.URLConnection;

import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.bridgedb.impl.InternalUtils;
import org.bridgedb.webservice.biomart.IDMapperBiomart;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

/**
 * BioMart service class, adapted from BioMart client in Cytoscape.
 */
public final class BiomartClient {
    public static final String DEFAULT_BASE_URL = "http://www.biomart.org/biomart/martservice";
    
    private final String baseURL;
    private static final String RESOURCE = "filterconversion.txt";

    // caches the result of getRegistry()
    private Map<String, Database> marts = null;

    private Map<String, Map<String, String>> filterConversionMap;

    private static final int BUFFER_SIZE = 81920;

    /**
     * Creates a new BiomartStub object from given URL.
     *
     * @param baseURL base url of martservice, for example "http://www.biomart.org/biomart/martservice" (default)
     * @throws IOException if failed to read local resource
     */
    public BiomartClient(String baseURL) throws IOException {
        this.baseURL = baseURL + "?";
        loadConversionFile();
    }

    /**
     * Conversion map from filter to attribute.
     * @throws IOException if failed to read local resource
     */
    private void loadConversionFile() throws IOException {
        filterConversionMap = new HashMap<String, Map<String, String>>();

        InputStreamReader inFile = new InputStreamReader(IDMapperBiomart.class.getResource(RESOURCE).openStream());

        BufferedReader inBuffer = new BufferedReader(inFile);

        String line;
        String trimed;
        String oldName = null;
        Map<String, String> oneEntry = new HashMap<String, String>();

        String[] dbparts;

        while ((line = inBuffer.readLine()) != null) {
            trimed = line.trim();
            dbparts = trimed.split("\\t");

            if (dbparts[0].equals(oldName) == false) {
                oneEntry = new HashMap<String, String>();
                oldName = dbparts[0];
                filterConversionMap.put(oldName, oneEntry);
            }

            oneEntry.put(dbparts[1], dbparts[2]);
        }

        inFile.close();
        inBuffer.close();
    }

    /**
     * Convert filter to attribute according to the conversion file.
     * @param dsName dataset name.
     * @param dbName database name
     * @param filterID filter ID
     * @return converted attribute
     * @throws IOException if failed to read from webservice
     */
    private Attribute filterToAttribute(String dsName, String dbName,
                String filterID) throws IOException
    {
        if (filterConversionMap.get(dbName) == null) {
            return null;
        } else {
            String attrName = filterConversionMap.get(dbName).get(filterID);
            return this.getAttribute(dsName, attrName);
        }
    }

    /**
     *
     * @param dataset dataset name
     * @param filter filter name
     * @return Attribute converted from filter
     * @throws IOException if failed to read from webservice
     */
    public Attribute filterToAttribute(String dataset, String filter) throws IOException 
    {
        Attribute attr;
        if (dataset.contains("REACTOME")) {
            attr = filterToAttribute(dataset, "REACTOME", filter);
        } else if (dataset.contains("UNIPROT")) {
            attr = filterToAttribute(dataset, "UNIPROT", filter);
        } else if (dataset.contains("VARIATION")) {
            attr = getAttribute(dataset, filter + "_stable_id");
        } else {
            attr = getAttribute(dataset, filter);
        }

        return attr;
    }

    /**
     *  Get the registry information from the base URL.
     *
     * @return  Map of registry information.  Key value is "name" field.
     * @throws ParserConfigurationException if failed new document builder
     * @throws SAXException if failed to parse registry
     * @throws IOException if failed to read from URL
     */
    public Map<String, Database> getRegistry()
            throws IOException, ParserConfigurationException, SAXException {
        // If already loaded, just return it.
        if (marts != null)
            return marts;

        // Initialize database map.
        marts = new HashMap<String, Database>();

        // Prepare URL for the registry status
        final String reg = "type=registry";
        final URL targetURL = new URL(baseURL + reg);

        // Get the result as XML document.
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();

        InputStream is = InternalUtils.getInputStream(targetURL);

        final Document registry = builder.parse(is);

        // Extract each datasource
        NodeList locations = registry.getElementsByTagName("MartURLLocation");
        int locSize = locations.getLength();
        NamedNodeMap attrList;
        int attrLen;
        String dbID;

        for (int i = 0; i < locSize; i++) {
            attrList = locations.item(i).getAttributes();
            attrLen = attrList.getLength();

            // First, get the key value
            dbID = attrList.getNamedItem("name").getNodeValue();

            Map<String, String> entry = new HashMap<String, String>();

            for (int j = 0; j < attrLen; j++) {
                entry.put(attrList.item(j).getNodeName(), attrList.item(j).getNodeValue());
            }

            marts.put(dbID, new Database(dbID,entry));
        }

        is.close();
        is = null;

        return marts;
    }

    /**
     * Get filter.
     * @param datasetName dataset name
     * @param filterName filter name
     * @throws IOException if failed to read webservice
     * @return filter
     */
    public Filter getFilter(String datasetName, String filterName) throws IOException
    {
        if (datasetName==null || filterName==null) {
            throw new java.lang.IllegalArgumentException("datasetName and filterName cannot be null");
        }

        Map<String, Filter> map = getDataset(datasetName).getFilters();
        if (map==null) {
            return null;
        }

        return map.get(filterName);
    }

    /**
     * get Attribute.
     * @param datasetName dataset name
     * @param attrName attribute name
     * @throws IOException if failed to read webservice
     * @return attribute
     */
    public Attribute getAttribute(String datasetName, String attrName) throws IOException
    {
        if (datasetName==null || attrName==null) {
            throw new NullPointerException("datasetName and attrName cannot be null");
        }

        Dataset dataset = getDataset(datasetName);
        Map<String, Attribute> map = dataset.getAttributes();
        if (map==null) {
            return null;
        }

        return map.get(attrName);
    }

    /**
     * Send the XML query to Biomart, and get the result as table.
     * @param xmlQuery query xml
     * @return result {@link BufferedReader}
     * @throws IOException if failed to read webservice
     */
    public BufferedReader sendQuery(String xmlQuery) throws IOException {

        //System.out.println("=======Query = " + xmlQuery);

        URL url = new URL(baseURL);
        URLConnection uc = url.openConnection();
        uc.setDoOutput(true);
        uc.setRequestProperty("User-Agent", "Java URLConnection");

        OutputStream os = uc.getOutputStream();

        final String postStr = "query=" + xmlQuery;
        PrintStream ps = new PrintStream(os);

        // Post the data
        ps.print(postStr);
        os.close();
        ps.close();
        ps = null;
        os = null;

        return new BufferedReader(new InputStreamReader(uc.getInputStream()), BUFFER_SIZE);
    }

    /**
     * get Database/mart.
     * @param dbname database name
     * @return database
     */
    public Database getMart(final String dbname) {
        return marts.get(dbname);
    }

    /**
     * Scans all available marts for a Dataset.
     * @param dsname dataset name
     * @return dataset or null if none was found
     * @throws IOException if failed to read webservice
     */
    public Dataset getDataset(final String dsname) throws IOException 
    {
    	for (Database db : marts.values())
    	{
    		Map<String, Dataset> datasets = db.getAvailableDatasets();
    		if (datasets.containsKey(dsname)) return datasets.get(dsname);
    	}
    	return null; // not found.
    }    
}
