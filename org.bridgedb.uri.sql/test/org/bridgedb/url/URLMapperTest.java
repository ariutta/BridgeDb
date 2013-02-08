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
package org.bridgedb.url;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bridgedb.Xref;
import org.bridgedb.rdf.RdfConfig;
import org.bridgedb.rdf.UriPattern;
import org.bridgedb.sql.SQLListener;
import org.bridgedb.statistics.MappingSetInfo;
import org.bridgedb.statistics.OverallStatistics;
import org.bridgedb.tools.metadata.constants.OwlConstants;
import org.bridgedb.tools.metadata.constants.SkosConstants;
import org.bridgedb.utils.BridgeDBException;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the URLMapper interface (and by loading the URLListener interface)
 *
 * Should be passable by any implementation of URLMapper that has the test data loaded.
 * 
 * @author Christian
 */
public abstract class URLMapperTest extends URLListenerTest{
                    
    @Test
    public void testMapIDOneToManyNoDataSources() throws BridgeDBException{
        report("MapIDOneToManyNoDataSources");
        Set<String> results = urlMapper.mapUri(map1URL1, RdfConfig.getProfileURI(0));
        assertTrue(results.contains(map1URL2));
        assertTrue(results.contains(map1URL3));
        assertFalse(results.contains(map2URL1));
        assertFalse(results.contains(map2URL2));
        assertFalse(results.contains(map2URL2));
    }
    
    @Test
    public void testToURLsOneToManyNoDataSources() throws BridgeDBException{
        report("MapXrefOneToManyNoDataSources");
        Set<String> results = urlMapper.mapUri(map1xref1, RdfConfig.getProfileURI(0));
        assertTrue(results.contains(map1URL2));
        assertTrue(results.contains(map1URL3));
        assertFalse(results.contains(map2URL1));
        assertFalse(results.contains(map2URL2));
        assertFalse(results.contains(map2URL2));
    }

    @Test
    public void testMapFullOneToManyNoDataSources() throws BridgeDBException{
        report("MapFullOneToManyNoDataSources");
        Set<Mapping> results = urlMapper.mapFull(map3URL3, RdfConfig.getProfileURI(0));
        Set<String> mappedTo = new HashSet<String>();
        for (Mapping URLMapping:results){
            if (URLMapping.getTargetURL().contains(map3URL3)){
                assertNull(URLMapping.getId());
                assertNull(URLMapping.getMappingSetId());        
                assertNull(URLMapping.getPredicate() );
            } else {
                mappedTo.addAll(URLMapping.getTargetURL());
                String[] predicates = {TEST_PREDICATE, SkosConstants.EXACT_MATCH.stringValue(), 
                    OwlConstants.EQUIVALENT_CLASS.stringValue()};
                assertThat(URLMapping.getPredicate(), isIn(predicates));
                assertNotNull(URLMapping.getId());
                assertNotNull(URLMapping.getMappingSetId());
            }
            assertTrue(URLMapping.getSourceURL().contains(map3URL3));
        }
        String[] expectedMatches = {map3URL1, map3URL2, map3URL2a};
        assertThat(mappedTo, hasItems(expectedMatches));
        assertThat(mappedTo, not(hasItems(map1URL1)));
        assertThat(mappedTo, not(hasItems(map2URL2)));
    }

    @Test
    public void testMapXrefFullOneToManyNoDataSources() throws BridgeDBException{
        report("MapXrefFullOneToManyNoDataSources");
        Set<Mapping> results = urlMapper.mapFull(map3xref3, RdfConfig.getProfileURI(0));
        Set<String> mappedTo = new HashSet<String>();
        for (Mapping URLMapping:results){
            if (URLMapping.getTargetURL().contains(map3URL3)){
                assertNull(URLMapping.getId());
                assertNull(URLMapping.getMappingSetId());        
                assertNull(URLMapping.getPredicate() );
            } else {
                mappedTo.addAll(URLMapping.getTargetURL());
                String[] predicates = {TEST_PREDICATE, SkosConstants.EXACT_MATCH.stringValue(), 
                    OwlConstants.EQUIVALENT_CLASS.stringValue()};
                assertThat(URLMapping.getPredicate(), isIn(predicates));
                assertNotNull(URLMapping.getId());
                assertNotNull(URLMapping.getMappingSetId());
            }
            assertEquals(map3xref3, URLMapping.getSource());
        }
        String[] expectedMatches = {map3URL1, map3URL2, map3URL2a};
        assertThat(mappedTo, hasItems(expectedMatches));
        assertThat(mappedTo, not(hasItems(map1URL1)));
        assertThat(mappedTo, not(hasItems(map2URL2)));
    }

    @Test 
    public void testMapIDOneBad() throws BridgeDBException{
        report("MapIDOneBad");
        Set<String> results = urlMapper.mapUri(mapBadURL1, RdfConfig.getProfileURI(0));
        //According to Martijn and the OPS needs mappers should return the incoming URI where appropiate.
        //Still optional as I am not sure text does.
        //Not all mappers will have the pattern matching to notice this is an invalid URI
        assertTrue(results.size() <= 1);
    }

    @Test 
    public void testMapFullOneBad() throws BridgeDBException{
        report("MapFullOneBad");
        Set<Mapping> results = urlMapper.mapFull(mapBadURL1, RdfConfig.getProfileURI(0));
        assertTrue(results.size() <= 1);
    }

    @Test 
    public void testMapFullOneBadOneNameSpace() throws BridgeDBException{
        report("MapFullOneBadOneNameSpace");
        Set<Mapping> results = urlMapper.mapFull(mapBadURL1, RdfConfig.getProfileURI(0), uriPattern2);
        assertTrue(results.size() <= 1);
    }

    @Test
    public void testMapIDOneToManyWithOneDataSource() throws BridgeDBException{
        report("MapIDOneToManyWithOneDataSource");
        Set<String> results = urlMapper.mapUri(map1URL1, RdfConfig.getProfileURI(0), uriPattern2);
        assertTrue(results.contains(map1URL2));
        assertFalse(results.contains(map1URL3));
        assertFalse(results.contains(map2URL1));
        assertFalse(results.contains(map2URL2));
        assertFalse(results.contains(map2URL2));
    }
 
    @Test
    public void testMapToURLsOneToManyWithOneDataSource() throws BridgeDBException{
        report("MapIDOneToManyWithOneDataSource");
        Set<String> results = urlMapper.mapUri(map1xref1, RdfConfig.getProfileURI(0), uriPattern2);
        assertTrue(results.contains(map1URL2));
        assertFalse(results.contains(map1URL3));
        assertFalse(results.contains(map2URL1));
        assertFalse(results.contains(map2URL2));
        assertFalse(results.contains(map2URL2));
    }
 
    @Test
    public void testMapToSelfWithOneDataSource() throws BridgeDBException{
        report("MapToSelfWithOneDataSource");
        Set<String> results = urlMapper.mapUri(map1URL2, RdfConfig.getProfileURI(0), uriPattern2);
        assertTrue(results.contains(map1URL2));
        assertFalse(results.contains(map1URL3));
        assertFalse(results.contains(map2URL1));
        assertFalse(results.contains(map2URL2));
        assertFalse(results.contains(map2URL2));
    }

    @Test
    public void testMapIDOneToManyWithTwoDataSources() throws BridgeDBException{
        report("MapIDOneToManyWithTwoDataSources");
        Set<String> results = urlMapper.mapUri(map1URL1, RdfConfig.getProfileURI(0), uriPattern2, uriPattern3);
        assertTrue(results.contains(map1URL2));
        assertTrue(results.contains(map1URL3));
        assertFalse(results.contains(map2URL1));
        assertFalse(results.contains(map2URL2));
        assertFalse(results.contains(map2URL2));
    }
 
    @Test
    public void testMapIDOneToManyNoDataSources2() throws BridgeDBException{
        report("MapIDOneToManyNoDataSources");
        Set<String> results = urlMapper.mapUri(map2URL1, RdfConfig.getProfileURI(0));
        assertTrue(results.contains(map2URL2));
        assertTrue(results.contains(map2URL3));
        assertFalse(results.contains(map1URL2));
        assertFalse(results.contains(map3URL2));
     }

    @Test
    public void testMapNoneExistingDataSource() throws BridgeDBException{
        report("MapNoneExistingDataSource");
        Set<String> results = urlMapper.mapUri(map1URL2, RdfConfig.getProfileURI(0), uriPatternBad);
        assertEquals(0,results.size());
    }

    @Test
    public void testURLSupported() throws Exception {
        report("URLSupported");
        assertTrue(urlMapper.uriExists(map1URL1));
        assertTrue(urlMapper.uriExists(map1URL2));
        assertTrue(urlMapper.uriExists(map1URL3));
        assertTrue(urlMapper.uriExists(map2URL1));
        assertTrue(urlMapper.uriExists(map2URL2));
        assertTrue(urlMapper.uriExists(map2URL3));
        assertTrue(urlMapper.uriExists(map3URL1));
        assertTrue(urlMapper.uriExists(map3URL2));
        assertTrue(urlMapper.uriExists(map3URL3));
        assertFalse(urlMapper.uriExists(mapBadURL1));
        System.out.println("1");
        assertFalse(urlMapper.uriExists(mapBadURL2));
        System.out.println("2");
        assertFalse(urlMapper.uriExists(mapBadURL3));
    }
        
    @Test
    public void testFreeSearchBad() throws BridgeDBException{
        org.junit.Assume.assumeTrue(urlMapper.getCapabilities().isFreeSearchSupported());       
        org.junit.Assume.assumeTrue(badID != null);
        report("FreeSearchBad");
        Set<String> results = urlMapper.urlSearch(badID, 10);
        assertTrue (results == null || results.isEmpty());
    }
    
    @Test
    public void testFreeSearchGood() throws BridgeDBException{
        org.junit.Assume.assumeTrue(urlMapper.getCapabilities().isFreeSearchSupported());       
        report("FreeSearchGood");
        Set<String> results = urlMapper.urlSearch(ds2Id3, 10);
        //Skip these if there are 10 or more possible ones. No Gurantee whiuch come back
        if (results.size() < 10){
            assertTrue (results.contains(map3URL2));
            assertTrue (results.contains(map3URL2a));
        }
        assertFalse (results.contains(map3URL1));
        assertFalse (results.contains(map1URL2));
    }
    
    @Test
    public void testFreeSearchGoodJust2() throws BridgeDBException{
        org.junit.Assume.assumeTrue(urlMapper.getCapabilities().isFreeSearchSupported());       
        report("FreeSearchGoodJust2");
        Set<String> results = urlMapper.urlSearch(ds2Id2, 2);
        assertEquals (2, results.size());
     }

    @Test
    public void testGetXrefGood() throws BridgeDBException {
        report("GetXrefGood");
        Xref result = urlMapper.toXref(map2URL2);
        assertEquals(map2xref2, result);
    }

    @Test 
    public void testGetXrefBad() throws BridgeDBException {
        report("GetXrefBad");
        Xref xref = urlMapper.toXref(mapBadURL1);
    }
    
    @Test
    public void testGetMapping() throws BridgeDBException {
        report("GetMapping");
        Set<Mapping> results = urlMapper.mapFull(map3URL3, RdfConfig.getProfileURI(0));
        Integer mappingId = null;
        Integer setId = null;
        for (Mapping URLMapping:results){
            if (URLMapping.getTargetURL().contains(map3URL2)){
                mappingId = URLMapping.getId();
                setId = URLMapping.getMappingSetId();        
            }
        }
        Mapping result = urlMapper.getMapping(mappingId);
        assertEquals(mappingId, result.getId());
        assertTrue(result.getSourceURL().contains(map3URL3));
        assertEquals(TEST_PREDICATE, result.getPredicate());
        assertTrue(result.getTargetURL().contains(map3URL2));
        assertEquals(setId, result.getMappingSetId());
        assertEquals(map3xref3.getId(), result.getSourceId());
        assertEquals(map3xref3.getDataSource().getSystemCode(), result.getSourceSysCode());
        assertEquals(map3xref2.getId(), result.getTargetId());
        assertEquals(map3xref2.getDataSource().getSystemCode(), result.getTargetSysCode());
    }
    
    @Test
    public void testGetSampleMappings() throws BridgeDBException {
        report("GetSampleSourceURL");
        List<Mapping> results = urlMapper.getSampleMapping();
        assertEquals(5, results.size());
        for (Mapping mapping:results){
            Set<String> sources = mapping.getSourceURL();
            assertThat(sources.size(), greaterThan(0));
            Set<String> targets = mapping.getTargetURL();
            assertThat(targets.size(), greaterThan(0));
        }
    }

    @Test
    public void testGetOverallStatistics() throws BridgeDBException {
        report("GetOverallStatistics()");
        OverallStatistics results = urlMapper.getOverallStatistics();
        assertThat (results.getNumberOfMappings(), greaterThanOrEqualTo(18));
        assertThat (results.getNumberOfMappingSets(), greaterThanOrEqualTo(6));
        assertThat (results.getNumberOfSourceDataSources(), greaterThanOrEqualTo(3));
        assertThat (results.getNumberOfTargetDataSources(), greaterThanOrEqualTo(3));
        assertThat (results.getNumberOfPredicates(), greaterThanOrEqualTo(1));
    }

    @Test
    public void testGetMappingSetInfo() throws BridgeDBException {
        report("GetMappingSetInfo");
        MappingSetInfo result = urlMapper.getMappingSetInfo(mappingSet2_3);
        assertEquals(DataSource2.getSystemCode(), result.getSourceSysCode());
        assertEquals(DataSource3.getSystemCode(), result.getTargetSysCode());
    }

    @Test
    public void testGetMappingSetInfos() throws BridgeDBException {
        report("GetMappingSetInfo All");
        List<MappingSetInfo> results = urlMapper.getMappingSetInfos(null, null);
        assertThat (results.size(), greaterThanOrEqualTo(6));
    }

    @Test
    public void testGetMappingSetInfosBySourceAndTarget() throws BridgeDBException {
        report("GetMappingSetInfos source and target");
        List<MappingSetInfo> results = 
                urlMapper.getMappingSetInfos(DataSource2.getSystemCode(), DataSource1.getSystemCode());
        assertThat (results.size(), greaterThanOrEqualTo(1));
        for (MappingSetInfo info:results){
            assertEquals(DataSource2.getSystemCode(), info.getSourceSysCode());
            assertEquals(DataSource1.getSystemCode(), info.getTargetSysCode());
        }
    }

    @Test
    public void testGetMappingSetInfosByTarget() throws BridgeDBException {
        report("GetMappingSetInfos target");
        List<MappingSetInfo> results = 
                urlMapper.getMappingSetInfos(null, DataSource3.getSystemCode());
        assertThat (results.size(), greaterThanOrEqualTo(2));
        for (MappingSetInfo info:results){
            assertEquals(DataSource3.getSystemCode(), info.getTargetSysCode());
        }
    }
    @Test
    public void testGetMappingSetInfosBySource() throws BridgeDBException {
        report("GetMappingSetInfos source");
        List<MappingSetInfo> results = 
                urlMapper.getMappingSetInfos(DataSource1.getSystemCode(), null);
        assertThat (results.size(), greaterThanOrEqualTo(2));
        for (MappingSetInfo info:results){
            assertEquals(DataSource1.getSystemCode(), info.getSourceSysCode());
        }
    }
    
    @Test
    public void testGetUriSpaces() throws BridgeDBException {
        report("GetUriSpaces");
        Set<String> results = urlMapper.getUriPatterns(map2xref3.getDataSource().getSystemCode());
        assertTrue (results.contains(uriPattern3.toString()));
    }
    
    @Test
    public void testGetSqlCompatVersion() throws BridgeDBException {
        report("GetSqlCompatVersion");
        int result = urlMapper.getSqlCompatVersion();
        assertEquals(SQLListener.SQL_COMPAT_VERSION, result);
    }
}
