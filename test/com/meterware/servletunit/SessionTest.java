package com.meterware.servletunit;
/********************************************************************************************************************
* $Id$
*
* Copyright (c) 2000, Russell Gold
*
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated 
* documentation files (the "Software"), to deal in the Software without restriction, including without limitation 
* the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
* to permit persons to whom the Software is furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in all copies or substantial portions 
* of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
* THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
* CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
* DEALINGS IN THE SOFTWARE.
*
*******************************************************************************************************************/
import java.io.*;
import java.net.HttpURLConnection;
import javax.servlet.ServletException;
import javax.servlet.http.*;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.meterware.httpunit.*;

/**
 * Tests the HttpSession implementation.
 **/
public class SessionTest extends TestCase {

    public static void main(String args[]) {
        junit.textui.TestRunner.run( suite() );
    }
    
    
    public static Test suite() {
        return new TestSuite( SessionTest.class );
    }


    public SessionTest( String name ) {
        super( name );
    }


    public void testNoInitialState() throws Exception {
        ServletUnitContext context = new ServletUnitContext();
        assertNull( "Session with incorrect ID", context.getSession( "12345" ) );
    }


    public void testCreateSession() throws Exception {
        ServletUnitContext context = new ServletUnitContext();
        ServletUnitHttpSession session = context.newSession();
        assertNotNull( "Session is null", session );
        assert( "Session is not marked as new", session.isNew() );
        ServletUnitHttpSession session2 = context.newSession();
        assert( "New session has the same ID", !session.getId().equals( session2.getId() ) );
        assert( "Different session returned", session.equals( context.getSession( session.getId() ) ) );
    }


    public void testSessionState() throws Exception {
        ServletUnitContext context = new ServletUnitContext();
        ServletUnitHttpSession session = context.newSession();
        long accessedAt = session.getLastAccessedTime();
        assert( "Session is not marked as new", session.isNew() );
        try { Thread.currentThread().sleep( 50 ); } catch (InterruptedException e) {};
        assertEquals( "Initial access time", accessedAt, context.getSession( session.getId() ).getLastAccessedTime() ); 
        session.access();
        assert( "Last access time not changed", accessedAt != context.getSession( session.getId() ).getLastAccessedTime() );
        assert( "Session is still marked as new", !context.getSession( session.getId() ).isNew() );

    }


}

