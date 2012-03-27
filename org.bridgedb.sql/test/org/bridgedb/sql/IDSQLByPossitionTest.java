/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bridgedb.sql;

import org.bridgedb.IDMapperException;
import org.bridgedb.iterator.XrefByPossitionTest;
import org.junit.BeforeClass;

/**
 * Warning: These tests depend on the data loaded in the IDMapperSQLTest.
 * So the first time this is run (or run after base tests change) this these test may cause errors.
 * Once IDMapperSQLTest is run once these should be fine until the test data changes again.
 * @author Christian
 */
public class IDSQLByPossitionTest extends XrefByPossitionTest {
    
    @BeforeClass
    public static void setupIDMapper() throws IDMapperException{
        SQLAccess sqlAccess = TestSqlFactory.createTestIDSQLAccess();
        IDMapperSQL iDMapperSQL = new IDMapperSQL(sqlAccess);
        xrefByPossition = iDMapperSQL;
    }
     
}
