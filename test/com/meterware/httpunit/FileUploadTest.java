package com.meterware.httpunit;
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

import java.net.URL;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.*;

import java.io.*;

import org.w3c.dom.Document;

import javax.activation.DataSource;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;


/**
 * A unit test of the file upload simulation capability.
 **/
public class FileUploadTest extends HttpUnitTest {

    public static void main(String args[]) {
        junit.textui.TestRunner.run( suite() );
    }
	
	
    public static Test suite() {
        return new TestSuite( FileUploadTest.class );
    }


    public FileUploadTest( String name ) {
        super( name );
    }


    public void setUp() throws Exception {
        super.setUp();
    }
	
        
    public void testParametersMultipartEncoding() throws Exception {
        defineResource( "ListParams", new MimeEcho() );
        defineWebPage( "Default", "<form method=POST action = \"ListParams\" enctype=\"multipart/form-data\"> " +
                                  "<Input type=text name=age value=12>" +
                                  "<Input type=submit name=update value=age>" +
                                  "</form>" );
        WebConversation wc = new WebConversation();
        WebRequest request = new GetMethodWebRequest( getHostPath() + "/Default.html" );
        WebResponse simplePage = wc.getResponse( request );
        WebRequest formSubmit = simplePage.getForms()[0].getRequest();
        WebResponse encoding = wc.getResponse( formSubmit );
        assertEquals( "Parameters", "update=age&age=12", encoding.getText().trim() );
    }


    public void testFileParameterValidation() throws Exception {
        File file = new File( "temp.html" );

        defineWebPage( "Default", "<form method=POST action = \"ListParams\" enctype=\"multipart/form-data\"> " +
                                  "<Input type=file name=message>" +
                                  "<Input type=submit name=update value=age>" +
                                  "</form>" );
        WebConversation wc = new WebConversation();
        WebRequest request = new GetMethodWebRequest( getHostPath() + "/Default.html" );
        WebResponse simplePage = wc.getResponse( request );
        WebRequest formSubmit = simplePage.getForms()[0].getRequest();

        try {
            formSubmit.setParameter( "message", "text/plain" );
            fail( "Should not allow setting of a file parameter to a text value" );
        } catch (IllegalFileParameterException e) {
        }
    }


    public void testNonFileParameterValidation() throws Exception {
        File file = new File( "temp.html" );

        defineWebPage( "Default", "<form method=POST action = \"ListParams\" enctype=\"multipart/form-data\"> " +
                                  "<Input type=text name=message>" +
                                  "<Input type=submit name=update value=age>" +
                                  "</form>" );
        WebConversation wc = new WebConversation();
        WebRequest request = new GetMethodWebRequest( getHostPath() + "/Default.html" );
        WebResponse simplePage = wc.getResponse( request );
        WebRequest formSubmit = simplePage.getForms()[0].getRequest();

        try {
            formSubmit.selectFile( "message", file );
            fail( "Should not allow setting of a text parameter to a file value" );
        } catch (IllegalNonFileParameterException e) {
        }
    }


    public void testURLEncodingFileParameterValidation() throws Exception {
        File file = new File( "temp.html" );

        defineWebPage( "Default", "<form method=POST action = \"ListParams\"> " +
                                  "<Input type=file name=message>" +
                                  "<Input type=submit name=update value=age>" +
                                  "</form>" );
        WebConversation wc = new WebConversation();
        WebRequest request = new GetMethodWebRequest( getHostPath() + "/Default.html" );
        WebResponse simplePage = wc.getResponse( request );
        WebRequest formSubmit = simplePage.getForms()[0].getRequest();

        try {
            formSubmit.selectFile( "message", file );
            fail( "Should not allow setting of a file parameter in a form which specifies url-encoding" );
        } catch (MultipartFormRequiredException e) {
        }
    }


    public void testFileMultipartEncoding() throws Exception {
        File file = new File( "temp.txt" );
        FileWriter fw = new FileWriter( file );
        PrintWriter pw = new PrintWriter( fw );
        pw.println( "Not much text" );
        pw.println( "But two lines" );
        pw.close();

        defineResource( "ListParams", new MimeEcho() );
        defineWebPage( "Default", "<form method=POST action = \"ListParams\" enctype=\"multipart/form-data\"> " +
                                  "<Input type=file name=message>" +
                                  "<Input type=submit name=update value=age>" +
                                  "</form>" );
        WebConversation wc = new WebConversation();
        WebRequest request = new GetMethodWebRequest( getHostPath() + "/Default.html" );
        WebResponse simplePage = wc.getResponse( request );
        WebRequest formSubmit = simplePage.getForms()[0].getRequest();
        formSubmit.selectFile( "message", file );
        WebResponse encoding = wc.getResponse( formSubmit );
        assertEquals( "update=age&message.name=temp.txt&message.lines=2", encoding.getText().trim() );

        file.delete();
    }
}


class StringDataSource implements DataSource {
    StringDataSource( String contentType, String contents ) {
        _contentType = contentType;
        _inputStream = new ByteArrayInputStream( contents.getBytes() );
    }


    public java.io.InputStream getInputStream() {
        return _inputStream;
    }


    public java.io.OutputStream getOutputStream() throws IOException {
        throw new IOException();
    }


    public java.lang.String getContentType() {
        return _contentType;
    }


    public java.lang.String getName() {
        return "test";
    }


    private String      _contentType;
    private InputStream _inputStream;

}


class MimeEcho extends PseudoServlet {
    public WebResource getPostResponse( Dictionary parameters, Dictionary headers ) {
        StringBuffer sb = new StringBuffer();
        try {
            String contentType = (String) headers.get( "CONTENT-TYPE" );
            String contents = (String) headers.get( PseudoServlet.CONTENTS );
            DataSource ds = new StringDataSource( contentType, contents );
            MimeMultipart mm = new MimeMultipart( ds );
            int numParts = mm.getCount();
            for (int i = 0; i < numParts; i++) {
                appendPart( sb, (MimeBodyPart) mm.getBodyPart(i) );
                if (i < numParts-1) sb.append( '&' );
            }
        } catch (MessagingException e) {
            sb.append( "Oops: " + e );
        } catch (IOException e) {
            sb.append( "Oops: " + e );
        }

        return new WebResource( sb.toString(), "text/plain" );
    }


    private void appendPart( StringBuffer sb, MimeBodyPart mbp ) throws IOException, MessagingException {
        String[] disposition = mbp.getHeader( "Content-Disposition" );
        String name = getHeaderAttribute( disposition[0], "name" );
        if (mbp.getFileName() == null) {
            appendFieldValue( name, sb, mbp );
        } else {
            appendFileSpecs( name, sb, mbp );
        }
    }


    private void appendFieldValue( String parameterName, StringBuffer sb, MimeBodyPart mbp ) throws IOException, MessagingException {
        sb.append( parameterName ).append( "=" ).append( mbp.getContent() );
    }


    private void appendFileSpecs( String parameterName, StringBuffer sb, MimeBodyPart mbp ) throws IOException, MessagingException {
        String filename = mbp.getFileName();
        BufferedReader br = new BufferedReader( new StringReader( mbp.getContent().toString() ) );
        int numLines = 0;
        while (br.readLine() != null) numLines++;

        sb.append( parameterName ).append( ".name=" ).append( filename ).append( "&" );
        sb.append( parameterName ).append( ".lines=" ).append( numLines );
    }


    private String getHeaderAttribute( String headerValue, String attributeName ) {
        StringTokenizer st = new StringTokenizer( headerValue, ";=", /* returnTokens */ true );

        int state = 0;
        String name = "";
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (token.equals( ";" )) {
                state = 1;   // next token is attribute name
            } else if (token.equals( "=" )) {
                if (state == 1) {
                    state = 2;   // next token is attribute value
                } else {
                    state = 0;   // reset and keep looking
                }
            } else if (state == 1) {
                name = token.trim();
            } else if (state == 2) {
                if (name.equalsIgnoreCase( attributeName )) {
                    return stripQuotes( token.trim() );
                }
            }
        }
        return "";
    }


    private String stripQuotes( String value ) {
        if (value.startsWith( "\"" ) && value.endsWith( "\"" )) {
            return value.substring( 1, value.length()-1 );
        } else {
            return value;
        }
    }
}