/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-04 The eXist Project
 *  http://exist-db.org
 *  
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *  
 *  $Id$
 */
package org.exist.storage.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Vector;

import junit.textui.TestRunner;

import org.apache.xmlrpc.XmlRpc;
import org.apache.xmlrpc.XmlRpcClient;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLTestCase;
import org.exist.StandaloneServer;
import org.exist.xmldb.XmldbURI;
import org.mortbay.util.MultiException;
 

/**
 * @author jim fuller at webcomposite.com
 * 
 * Test XInclude Serialiser via REST/XMLRPC/WEBDAV/SOAP interfaces
 * 
 * @todo: need to refacotr to avoid catching unexpected exceptions
 * 
 * 
 */
public class XIncludeSerializerTest extends XMLTestCase {

	private static StandaloneServer server = null;
    
    private final static XmldbURI XINCLUDE_COLLECTION = XmldbURI.ROOT_COLLECTION_URI.append("xinclude_test");
    private final static XmldbURI XINCLUDE_NESTED_COLLECTION = XmldbURI.ROOT_COLLECTION_URI.append("xinclude_test/data");

    private final static String XMLRPC_URI = "http://127.0.0.1:8088/xmlrpc";
    private final static String REST_URI = "http://admin:admin@127.0.0.1:8088/db/xinclude_test";

    private final static String XML_DATA1 =
    	"<test xmlns:xi='http://www.w3.org/2001/XInclude'>" +
    	"<root>" +
    	"<xi:include href='metatags.xml'/>" +
    	"</root>" +
    	"</test>";
    
    private final static String XML_DATA2 =
    	"<html>" +
    	"<head>" +
    	"<metatag name='test' description='test'/>" +
    	"</head>" +
    	"</html>";

    private final static String XML_DATA3 =
    	"<test xmlns:xi='http://www.w3.org/2001/XInclude'>" +
    	"<root>" +
    	"<xi:include href='../xinclude_test/data/metatags.xml'/>" +
    	"</root>" +
    	"</test>";

    private final static String XML_DATA4 =
    	"<test xmlns:xi='http://www.w3.org/2001/XInclude'>" +
    	"<root>" +
    	"<xi:include href='data/metatags.xml'/>" +
    	"</root>" +
    	"</test>";
    
    private final static String XML_RESULT ="<test xmlns:xi='http://www.w3.org/2001/XInclude'>"+
    "<root>" +
    "<html>" +
    "<head>"+
    "<metatag name='test' description='test'/>" +
    "</head>" +
    "</html>" +
    "</root>"+
    "</test>";
      
    public XIncludeSerializerTest() {
        try {
	    setUp();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }//xincludeserializertest



    /*
     * REST tests
     *
     */
    public void testABSSimpleREST() {
       try { 

    	    // path needs to indicate indent and wrap is off
        	String uri = REST_URI + "/test_simple.xml?_indent=no&_wrap=no";

        	// we use honest http
        	HttpURLConnection connect = getConnection(uri);
	    connect.setRequestMethod("GET");
	    connect.connect();

	    BufferedReader reader = new BufferedReader(new InputStreamReader(connect.getInputStream(), "UTF-8"));
	    String line;
	    StringBuffer out = new StringBuffer();
	    
	    while ((line = reader.readLine()) != null) {
	        out.append(line);
	        out.append("\r\n");
	    }
	    
	    String responseXML = out.toString();

        System.out.println("response XML:"+ responseXML);
        System.out.println("control XML" + XML_RESULT);

        Diff myDiff = new Diff(XML_RESULT, responseXML);
        assertTrue("pieces of XML are similar " + myDiff, myDiff.similar());
        assertTrue("but are they identical? " + myDiff, myDiff.identical());
        
        } catch (Exception e) {
            fail(e.getMessage());
        }
        
    }//testABSSimpleREST


    public void testRELSimpleREST1() {
        try { 
         	String uri = REST_URI + "/test_relative1.xml?_indent=no&_wrap=no";
      
         	HttpURLConnection connect = getConnection(uri);
 	    connect.setRequestMethod("GET");
 	    connect.connect();

 	    BufferedReader reader = new BufferedReader(new InputStreamReader(connect.getInputStream(), "UTF-8"));
 	    String line;
 	    StringBuffer out = new StringBuffer();
 	    while ((line = reader.readLine()) != null) {
 	        out.append(line);
 	        out.append("\r\n");
 	    }
 	    String responseXML = out.toString();

        System.out.println("response XML:"+ responseXML);
        System.out.println("control XML" + XML_RESULT);

         Diff myDiff = new Diff(XML_RESULT, responseXML);
         assertTrue("pieces of XML are similar " + myDiff, myDiff.similar());
         assertTrue("but are they identical? " + myDiff, myDiff.identical());
         
         } catch (Exception e) {
             fail(e.getMessage());
         }
         
     }//testRELSimpleREST1

    
    public void testRELSimpleREST2() {
        try { 
         // path needs to indicate indent and wrap is off
         	String uri = REST_URI + "/test_relative2.xml?_indent=no&_wrap=no";
      
         	HttpURLConnection connect = getConnection(uri);
 	    connect.setRequestMethod("GET");
 	    connect.connect();

 	    BufferedReader reader = new BufferedReader(new InputStreamReader(connect.getInputStream(), "UTF-8"));
 	    String line;
 	    StringBuffer out = new StringBuffer();
 	    while ((line = reader.readLine()) != null) {
 	        out.append(line);
 	        out.append("\r\n");
 	    }
 	    String responseXML = out.toString();

        System.out.println("response XML:"+ responseXML);
        System.out.println("control XML" + XML_RESULT);

         Diff myDiff = new Diff(XML_RESULT, responseXML);
         assertTrue("pieces of XML are similar " + myDiff, myDiff.similar());
         assertTrue("but are they identical? " + myDiff, myDiff.identical());
         
         } catch (Exception e) {
             fail(e.getMessage());
         }
         
     }//testRELSimpleREST

    
    // @TODO add full url test e.g. http://www.example.org/test.xml for xinclude
    
    
    // @TODO add simple and relative url with xpointer
    //ex. <xi:include href="../javascript.xml#xpointer(html/head)"/>

    
    // @TODO add simple and relative url with xpointer and namespaces
    // ex. <xi:include href="../javascript.xml#xmlns(x=http://www.w3.org/1999/xhtml)xpointer(/x:html/x:head)"/>
    
    
    /*
     * XML-RPC tests
     *
     */
    // @TODO check serialisation via this interface, simple and relative

    /*
     * WebDAV tests
     *
     */
    
    // @TODO check serialisation via this interface, simple and relative???
    // probably overkill
     
    /*
     * SOAP tests
     *
     */
    // probably overkill
 

   
    // @TODO check serialisation via this interface, simple and relative???
    // probably overkill
   
    /*
     * helper functions
     *
     */
    protected HttpURLConnection getConnection(String url) {

    	try {
            URL u = new URL(url);
            return (HttpURLConnection) u.openConnection();
        } catch (Exception e) {
            fail(e.getMessage());
        }
        return null;

    }//httpurlconnection
       
	protected XmlRpcClient getClient() {

		try {
			XmlRpc.setEncoding("UTF-8");
			XmlRpcClient xmlrpc = new XmlRpcClient(XMLRPC_URI);
			xmlrpc.setBasicAuthentication("admin", "");
			return xmlrpc;
	    } catch (Exception e) {            
	        fail(e.getMessage());  
	    }
	    return null;

	}//xmlrpcclient

   
   // @TODO create reader for xml 
 
    /*
     * SetUp / TearDown functions
     *
     */
    protected void setUp() {
        //Don't worry about closing the server : the shutdown hook will do the job
        try {
            if (server == null) {
                server = new StandaloneServer();
                if (!server.isStarted()) {
                    try {
                        System.out.println("Starting standalone server...");
                        String[] args = {};
                        server.run(args);
                        while (!server.isStarted()) {
                            Thread.sleep(1000);
                        }
                    } catch (MultiException e) {
                        boolean rethrow = true;
                        Iterator i = e.getExceptions().iterator();
                        while (i.hasNext()) {
                            Exception e0 = (Exception) i.next();
                            if (e0 instanceof BindException) {
                                System.out.println("A server is running already !");
                                rethrow = false;
                                break;
                            }
                        }
                        if (rethrow)
                            throw e;
                    }
                }
            }
            
			System.out.println("Creating collection " + XINCLUDE_COLLECTION);
			XmlRpcClient xmlrpc = getClient();
			Vector params = new Vector();
			params.addElement(XINCLUDE_COLLECTION.toString());
			Boolean resultColl1 = (Boolean)xmlrpc.execute("createCollection", params);

			params.clear();			
			params.addElement(XINCLUDE_NESTED_COLLECTION.toString());
			Boolean resultColl2 = (Boolean)xmlrpc.execute("createCollection", params);

			System.out.println("Loading test document data");
			params.clear();
			params.addElement(XML_DATA1);
			params.addElement("/db/xinclude_test/test_simple.xml");
			params.addElement(new Integer(1));			
			Boolean resultFile1 = (Boolean)xmlrpc.execute("parse", params);

			params.clear();
			params.addElement(XML_DATA2);
			params.addElement("/db/xinclude_test/metatags.xml");
			params.addElement(new Integer(1));			
			Boolean resultFile3 = (Boolean)xmlrpc.execute("parse", params);

			params.clear();
			params.addElement(XML_DATA2);
			params.addElement("/db/xinclude_test/data/metatags.xml");
			params.addElement(new Integer(1));			
			Boolean resultFile4 = (Boolean)xmlrpc.execute("parse", params);
			
			params.clear();
			params.addElement(XML_DATA3);
			params.addElement("/db/xinclude_test/test_relative1.xml");
			params.addElement(new Integer(1));			
			Boolean resultFile5 = (Boolean)xmlrpc.execute("parse", params);

			params.clear();
			params.addElement(XML_DATA4);
			params.addElement("/db/xinclude_test/test_relative2.xml");
			params.addElement(new Integer(1));			
			Boolean resultFile6 = (Boolean)xmlrpc.execute("parse", params);

        } catch (Exception e) {
            fail(e.getMessage());
        }
    }


    protected void tearDown() throws Exception {
    	try{
			XmlRpcClient xmlrpc = getClient();
			Vector params = new Vector();
			params.clear();
			params.addElement("/db/xinclude_test");
			Boolean resultRemove = (Boolean)xmlrpc.execute("removeCollection", params);
	} catch (Exception e) {
        fail(e.getMessage());	
    }
    }//tearDown

    
    public static void main(String[] args) {
    	TestRunner.run(XIncludeSerializerTest.class);
    }

}//XIncludeserializertest
