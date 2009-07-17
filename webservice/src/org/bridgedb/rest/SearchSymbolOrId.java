package org.bridgedb.rest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bridgedb.DataSource;
import org.bridgedb.Xref;
import org.bridgedb.rdb.IDMapperRdb;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

/**
 * Resource that handles the xref queries
 */
public class SearchSymbolOrId extends IDMapperResource {
	List<IDMapperRdb> mappers;
	String searchStr;
	int limit;
  	String org;
	
	protected void doInit() throws ResourceException {
		try {
		    System.out.println( "SearchSymbol.init() start" );
		    org = (String) getRequest().getAttributes().get( IDMapperService.PAR_ORGANISM );
		    mappers = getIDMappers(org);
		    System.out.println( "1" );
		    searchStr = (String) getRequest().getAttributes().get( IDMapperService.PAR_SEARCH_STR );
		    System.out.println( "2: " + searchStr );
	       	    String limitStr = (String)getRequest().getAttributes().get( IDMapperService.PAR_TARGET_LIMIT );
		    System.out.println( "3: " + limitStr );
	     	    limit = new Integer( limitStr ).intValue();

		    System.out.println( "SearchSymbol.doInit() done" );
		} catch(Exception e) {
			throw new ResourceException(e);
		}
	}

	@Get
	public String getSearchFreeResult() 
	{
	  System.out.println( "SearchSymbol.getSearchSymbolResult() start" );
	  try 
	  {
	    //The result set
	    Map<Xref, String> suggestions = new HashMap<Xref, String>();
	    
	    for(IDMapperRdb mapper : mappers ) {
	    	Set<Xref> tempset = new HashSet<Xref>();
	    	tempset.addAll( mapper.freeSearch( searchStr, limit ) );
	    	tempset.addAll( mapper.freeAttributeSearch( searchStr, "Symbol", limit ) );
	    	for (Xref x : tempset)
	    	{
	    		for (String s : mapper.getAttributes (x, "Symbol"))
	    		{
		    		suggestions.put (x, s);
		    		break; // only put the first
	    		}
	    	}
	    }
	    
        StringBuilder result = new StringBuilder();
	    for(Xref x : suggestions.keySet()) {
		result.append( x.getId() );
		result.append( "\t" );
		result.append( suggestions.get(x) );
		result.append( "\t" );
		result.append( x.getDataSource().getFullName() );
		result.append( "\n" );
	    }

	    return( result.toString() );
          } catch( Exception e ) {
	    e.printStackTrace();
	    setStatus( Status.SERVER_ERROR_INTERNAL );
	    return e.getMessage();
	  }
	}

}
