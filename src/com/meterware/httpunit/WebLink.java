package com.meterware.httpunit;
/********************************************************************************************************************
* $Id$
*
* Copyright (c) 2000-2002, Russell Gold
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
import com.meterware.httpunit.scripting.ScriptableDelegate;
import com.meterware.httpunit.scripting.NamedDelegate;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * This class represents a link in an HTML page. Users of this class may examine the
 * structure of the link (as a DOM), or create a {@link WebRequest} to simulate clicking
 * on the link.
 *
 * @author <a href="mailto:russgold@acm.org">Russell Gold</a>
 * @author <a href="mailto:benoit.xhenseval@avondi.com>Benoit Xhenseval</a>
 **/
public class WebLink extends FixedURLWebRequestSource {

    private Scriptable _scriptable;


    /**
     * Returns the URL referenced by this link. This may be a relative URL.
     **/
    public String getURLString() {
        String href = NodeUtils.getNodeAttribute( getNode(), "href" );
        final int hashIndex = href.indexOf( '#' );
        if (hashIndex < 0) {
            return href;
        } else {
            return href.substring( 0, hashIndex );
        }
    }


    /**
     * Returns the text value of this link.
     **/
    public String asText() {
        if (getNode().getNodeName().equals( "area" )) {
            return NodeUtils.getNodeAttribute( getNode(), "alt" );
        } else if (!getNode().hasChildNodes()) {
            return "";
        } else {
            return NodeUtils.asText( getNode().getChildNodes() );
        }
    }


    /**
     * Submits a request as though the user had clicked on this link. Will also fire the 'onClick' event if defined.
     **/
    public WebResponse click() throws IOException, SAXException {
        String event = NodeUtils.getNodeAttribute( getNode(), "onclick" );
        if (event.length() == 0 || getScriptableObject().doEvent( event )) return submitRequest();
        return getBaseResponse();
    }


    /**
     * Simulates moving the mouse over the link. Will fire the 'onMouseOver' event if defined.
     **/
    public void mouseOver() {
        String event = NodeUtils.getNodeAttribute( getNode(), "onmouseover" );
        if (event.length() > 0) getScriptableObject().doEvent( event );
    }


    public class Scriptable extends ScriptableDelegate implements NamedDelegate {

        public String getName() {
            return WebLink.this.getName();
        }


        public Object get( String propertyName ) {
            if (propertyName.equalsIgnoreCase( "href" )) {
                return getReference().toExternalForm();
            } else {
               return super.get( propertyName );
            }
        }


        private URL getReference() {
            try {
                return getRequest().getURL();
            } catch (MalformedURLException e) {
                return WebLink.this.getBaseURL();
            }
        }
    }


//----------------------------------------- WebRequestSource methods ---------------------------------------------------


    /**
     * Returns the scriptable delegate.
     */
    ScriptableDelegate getScriptableDelegate() {
        return getScriptableObject();
    }


//--------------------------------------------------- package members --------------------------------------------------


    /**
     * Contructs a web link given the URL of its source page and the DOM extracted
     * from that page.
     **/
    WebLink( WebResponse response, URL baseURL, String parentTarget, Node node ) {
        super( response, node, baseURL, NodeUtils.getNodeAttribute( node, "href" ), parentTarget );
    }


    /**
     * Returns an object which provides scripting access to this link.
     **/
    Scriptable getScriptableObject() {
        if (_scriptable == null) _scriptable = new Scriptable();
        return _scriptable;
    }


}
