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
import java.io.*;
import java.net.*;
import java.util.*;
import org.xml.sax.*;


/**
 * The context for a series of HTTP requests. This class manages cookies used to maintain
 * session context, computes relative URLs, and generally emulates the browser behavior
 * needed to build an automated test of a web site.
 *
 * @author Russell Gold
 * @author Jan Ohrstrom
 * @author Seth Ladd 
 **/
public class WebConversation {

    
    /**
     * Creates a new web conversation.
     **/
    public WebConversation() {
    }


    /**
     * Returns the name of the currently active frames.
     **/
    public String[] getFrameNames() {
        Vector names = new Vector();
        for (Enumeration e = _frameContents.keys(); e.hasMoreElements();) {
            names.addElement( e.nextElement() );
        }

        String[] result = new String[ names.size() ];
        names.copyInto( result );
        return result;
    }


    /**
     * Returns the response associated with the specified frame name.
     **/
    public WebResponse getFrameContents( String frameName ) {
        WebResponse response = (WebResponse) _frameContents.get( frameName );
        if (response == null) throw new NoSuchFrameException( frameName );
        return response;
    }


    /**
     * Submits a GET method request and returns a response.
     **/
    public WebResponse getResponse( String urlString ) throws MalformedURLException, IOException, SAXException {
        return getResponse( new GetMethodWebRequest( urlString ) );
    }


    /**
     * Submits a web request and returns a response, using all state developed so far as stored in
     * cookies as requested by the server.
     **/
    public WebResponse getResponse( WebRequest request ) throws MalformedURLException, IOException, SAXException {
        HttpURLConnection connection = (HttpURLConnection) openConnection( request.getURL() );
        request.completeRequest( connection );
        updateCookies( connection );

        if (connection.getHeaderField( "Location" ) != null) {
            delay( HttpUnitOptions.getRedirectDelay() );
            return getResponse( new RedirectWebRequest( request, connection.getHeaderField( "Location" ) ) );
        } else if (connection.getHeaderField( "WWW-Authenticate" ) != null) {
            throw new AuthorizationRequiredException( connection.getHeaderField( "WWW-Authenticate" ) );
        } else if (connection.getResponseCode() == HttpURLConnection.HTTP_INTERNAL_ERROR) {
            throw new HttpInternalErrorException( request.getURLString() );
        } else if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
            throw new HttpNotFoundException( request.getURLString() );        
        } else if (connection.getResponseCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
            throw new HttpException( connection.getResponseCode(), request.getURLString() );        
        } else {
            WebResponse result = new WebResponse( this, request.getTarget(), request.getURL(), connection );
            if (result.isHTML()) {
                removeSubFrames( request.getTarget() );
                _frameContents.put( request.getTarget(), result );
                createSubFrames( request.getTarget(), result.getFrameNames() );
                WebRequest[] requests = result.getFrameRequests();
                for (int i = 0; i < requests.length; i++) getResponse( requests[i] );
            }
            return result;
        }
    }


    private void delay( int numMilliseconds ) {
        if (numMilliseconds == 0) return;
        try {
            Thread.sleep( numMilliseconds );
        } catch (InterruptedException e) {
            // ignore the exception
        }
    }


    private void createSubFrames( String targetName, String[] frameNames ) {
        _subFrames.put( targetName, frameNames );
        for (int i = 0; i < frameNames.length; i++) {
            _frameContents.put( frameNames[i], WebResponse.BLANK_RESPONSE );
        }
    }


    private void removeSubFrames( String targetName ) {
        String[] names = (String[]) _subFrames.get( targetName );
        if (names == null) return;
        for (int i = 0; i < names.length; i++) {
            removeSubFrames( names[i] );
            _frameContents.remove( names[i] );
            _subFrames.remove( names[i] );
        }
    }


    /**
     * Defines a cookie to be sent to the server on every request.
     **/
    public void addCookie(String name, String value) {
	_cookies.put( name, value );
    }


    /**
     * Returns the name of all the active cookies which will be sent to the server.
     **/
    public String[] getCookieNames() {
        String[] names = new String[ _cookies.size() ];
        int i = 0;
        for (Enumeration e = _cookies.keys(); e.hasMoreElements();) {
            names[i++] = (String) e.nextElement();
        }
        return names;
    }


    /**
     * Returns the value of the specified cookie.
     **/
    public String getCookieValue( String name ) {
        return (String) _cookies.get( name );
    }

    
    /**
     * Specifies the user agent identification. Used to trigger browser-specific server behavior.
     **/    
    public void setUserAgent(String userAgent) {
	_userAgent = userAgent;
    }
    
        
    /**
     * Returns the current user agent setting.
     **/
    public String getUserAgent() {
	return _userAgent;
    }


    /**
     * Sets a username and password for a basic authentication scheme.
     **/
    public void setAuthorization( String userName, String password ) {
        _authorization = "Basic " + Base64.encode( userName + ':' + password );
    }


//---------------------------------- private members --------------------------------

    /** The currently defined cookies. **/
    private Hashtable _cookies = new Hashtable();


    /** The current user agent. **/
    private String _userAgent;


    /** The authorization header value. **/
    private String _authorization;


    /** A map of frame names to current contents. **/
    private Hashtable _frameContents = new Hashtable();


    /** A map of frame names to frames nested within them. **/
    private Hashtable _subFrames = new Hashtable();

    
    static {
        HttpURLConnection.setFollowRedirects( false );
    }

    private HttpURLConnection openConnection( URL url ) throws MalformedURLException, IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setUseCaches( false );
        sendAuthorization( connection );
	sendUserAgent( connection );
        sendCookies( connection );
        return connection;
    }


    private void sendAuthorization( URLConnection connection ) {
        if (_authorization == null) return;
        connection.setRequestProperty( "Authorization", _authorization );
    }

    
    private void sendUserAgent ( URLConnection connection ) {
	if (getUserAgent() == null) return;
	connection.setRequestProperty( "User-Agent" , getUserAgent() );
    }

    
    private void sendCookies( URLConnection connection ) {
    	if (_cookies.size() == 0) return;
    	
    	StringBuffer sb = new StringBuffer();
    	for (Enumeration e = _cookies.keys(); e.hasMoreElements();) {
    		String name = (String) e.nextElement();
    		sb.append( name ).append( '=' ).append( _cookies.get( name ) );
    		if (e.hasMoreElements()) sb.append( ';' );
    	}
    	connection.setRequestProperty( "Cookie", sb.toString() );
    }
    
    
    private void updateCookies( URLConnection connection ) {
        for (int i = 1; true; i++) {
            String key = connection.getHeaderFieldKey( i );
            if (key == null) break;
            if (HttpUnitOptions.isLoggingHttpHeaders()) {
                System.out.println( "Header:: " + connection.getHeaderFieldKey( i ) + ": " + connection.getHeaderField(i) );
            }
            if (!key.equalsIgnoreCase( "Set-Cookie" )) continue;
            StringTokenizer st = new StringTokenizer( connection.getHeaderField( i ), "=;" );
            String name = st.nextToken();
            String value = st.nextToken();
            _cookies.put( name, value );
        };
    }
}



class RedirectWebRequest extends WebRequest {

    RedirectWebRequest( WebRequest baseRequest, String relativeURL ) throws MalformedURLException {
        super( baseRequest, relativeURL );
    }    
    
}



class NoSuchFrameException extends RuntimeException {

    NoSuchFrameException( String frameName ) {
        _frameName = frameName;
    }


    public String getMessage() {
        return "No frame named " + _frameName + " is currently active";
    }


    private String _frameName;
}
