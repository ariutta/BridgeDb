// BridgeDb,
// An abstraction layer for identifier mapping services, both local and online.
//
// Copyright      2012  Egon Willighagen <egonw@users.sf.net>
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
package org.bridgedb;

import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests the DataSource class
 *
 * @author Christian
 */
public class DataSourceTest {

	@Test
	public void testAsDataSource() {
		DataSource source = DataSource.register("X", "Affymetrix")
		    .asDataSource();
		Assert.assertNotNull(source);
	}

	@Test
	public void testBuilding() {
		DataSource source = DataSource.register("X", "Affymetrix").asDataSource();
		Assert.assertEquals("X", source.getSystemCode());
		Assert.assertEquals("Affymetrix", source.getFullName());
	}

	@Test
	public void testBuildingMainUrl() {
		DataSource source = DataSource.register("X", "Affymetrix")
		    .mainUrl("http://www.affymetrix.com")
		    .asDataSource();
		Assert.assertEquals("http://www.affymetrix.com", source.getMainUrl());
	}

	@Test
	public void testBuildingType() {
		DataSource source = DataSource.register("X", "Affymetrix")
		    .type("probe")
		    .asDataSource();
		Assert.assertEquals("probe", source.getType());
		Assert.assertFalse(source.isMetabolite());
	}

	@Test
	public void testBuildingPrimary() {
		DataSource source = DataSource.register("X", "Affymetrix")
		    .primary(false)
		    .asDataSource();
		Assert.assertFalse(source.isPrimary());
		source = DataSource.register("X", "Affymetrix")
			.primary(true)
			.asDataSource();
		Assert.assertTrue(source.isPrimary());
	}

	@Test
	public void testBuildingMetabolite() {
		DataSource source = DataSource.register("F", "MetaboLoci")
		    .type("metabolite")
		    .asDataSource();
		Assert.assertEquals("metabolite", source.getType());
		Assert.assertTrue(source.isMetabolite());
	}
    
/*    @Test
    public void testRegisterSecondFullName(){
        String sysCode = "testRegisterSecondFullName";
        String fullName1 = "testRegisterSecondFullName_FullName1";
        DataSource ds1 = DataSource.register(sysCode, fullName1).asDataSource();
        String fullName2 = "testRegisterSecondFullName_FullName2";
        DataSource ds2 = DataSource.register(sysCode, fullName2).asDataSource();
        Assert.assertEquals(ds1, ds2);
        Assert.assertEquals(fullName2, ds2.getFullName());
        Assert.assertEquals(fullName1, ds1.getAlternativeFullNames().iterator().next());      
        Assert.assertEquals(ds1, DataSource.getByFullName(fullName2));
        Assert.assertEquals(ds1, DataSource.getByFullName(fullName1));        
    }

    @Test
    public void testRegisterAlternativeFullName(){
        String sysCode = "testRegisterSecondFullName";
        String fullName1 = "testRegisterSecondFullName_FullName1";
        DataSource ds1 = DataSource.register(sysCode, fullName1).asDataSource();
        String fullName2 = "testRegisterSecondFullName_FullName2";
        DataSource ds2 = DataSource.register(sysCode, fullName2).asDataSource();
        Assert.assertEquals(ds1, ds2);
        Assert.assertEquals(fullName2, ds2.getFullName());
        Assert.assertEquals(fullName1, ds1.getAlternativeFullNames().iterator().next());        
    }
    
    @Test (expected = IllegalStateException.class)
    public void testRegisterAlternativeFullWhichIsAlreadyFullname(){
        String fullName1 = "testRegisterAlternativeFullWhichIsAlreadyFullname";
        DataSource ds = DataSource.register(null, fullName1)
                .alternativeFullName(fullName1)
                .asDataSource();
    }
    
    @Test (expected = IllegalStateException.class)
    public void testRegisterAlternativeFullNameToTwoDataSources(){
        String fullName1 = "testRegisterAlternativeFullNameToTwoDataSources1";
        String altName = "testRegisterAlternativeFullNameToTwoDataSourcesalt";        
        DataSource ds1 = DataSource.register(null, fullName1)
                .alternativeFullName(altName)
                .asDataSource();
        String fullName2 = "testRegisterAlternativeFullNameToTwoDataSources2";
        DataSource ds2 = DataSource.register(null, fullName2)
                .alternativeFullName(altName)
                .asDataSource();
    }
    
    @Test (expected = IllegalStateException.class)
    public void testRegisterAsFullNameAndThenAlternativeName(){
        String fullName1 = "testRegisterAsFullNameAndThenAlternativeName1";
        DataSource ds1 = DataSource.register(null, fullName1)
                .asDataSource();
        String fullName2 = "testRegisterAsFullNameAndThenAlternativeName2";
        DataSource ds2 = DataSource.register(null, fullName2)
                .alternativeFullName(fullName1)
                .asDataSource();
    }

    @Test 
    public void testRegisterAsAlternativeAndThenFullName(){
        String fullName1 = "testRegisterAsAlternativeAndThenFullName1";
        String altName = "testRegisterAsAlternativeAndThenFullNameAlt";        
        DataSource ds1 = DataSource.register(null, fullName1)
                .alternativeFullName(altName)
                .asDataSource();
        DataSource ds2 = DataSource.register(null, altName)
                .asDataSource();
        Assert.assertEquals(ds1, ds2);
        //ToDo dettermine which should be the full name of this DataSource.
        //Better would be to throw an error on registering the new one but reverse combatability issue!
        Assert.assertEquals(altName, ds1.getAlternativeFullNames().iterator().next());        
    }

    @Test (expected = IllegalStateException.class)
    public void testRegisterAsAlternativeAndThenFullNameDiffSysCocdes(){
        String sysCode1 = "testRegisterAsAlternativeAndThenFullNameDiffSysCocdesSysCode1";
        String fullName1 = "testRegisterAsAlternativeAndThenFullNameDiffSysCocdesFullName1";
        String altName = "testRegisterAsAlternativeAndThenFullNameDiffSysCocdesFullNameAlt";        
        String sysCode2 = "testRegisterAsAlternativeAndThenFullNameDiffSysCocdesSysCode2";
        DataSource ds1 = DataSource.register(sysCode1, fullName1)
                .alternativeFullName(altName)
                .asDataSource();
        DataSource ds2 = DataSource.register(sysCode2, altName)
                .asDataSource();
        Assert.assertEquals(ds1, ds2);
        Assert.assertEquals(fullName1, ds1.getFullName());        
        Assert.assertEquals(altName, ds2.getFullName());
        Assert.assertEquals(altName, ds1.getAlternativeFullNames().iterator().next());        
    }
*/
}
