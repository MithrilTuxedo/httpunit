package com.meterware.httpunit.javascript;
/********************************************************************************************************************
 * $Id$
 *
 * Copyright (c) 2002, Russell Gold
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
import com.meterware.httpunit.*;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

import java.util.ArrayList;


/**
 *
 * @author <a href="mailto:russgold@acm.org">Russell Gold</a>
 **/
public class ScriptingTest extends HttpUnitTest {

    public static void main( String args[] ) {
        TestRunner.run( suite() );
    }


    public static TestSuite suite() {
        return new TestSuite( ScriptingTest.class );
    }


    public ScriptingTest( String name ) {
        super( name );
    }


    public void testJavaScriptURLWithValue() throws Exception {
        defineResource(  "OnCommand.html",  "<html><head></head>" +
                                            "<body>" +
                                            "<a href='javascript:\"You made it!\"'>go</a>" +
                                            "</body></html>" );
        WebConversation wc = new WebConversation();
        WebResponse response = wc.getResponse( getHostPath() + "/OnCommand.html" );
        response.getLinks()[0].click();
        assertEquals( "New page", "You made it!", wc.getCurrentPage().getText() );
        assertEquals( "New URL", "javascript:\"You made it!\"", wc.getCurrentPage().getURL().toExternalForm() );
    }


    public void testJavaScriptURLWithNoValue() throws Exception {
        defineResource(  "OnCommand.html",  "<html><head></head>" +
                                            "<body>" +
                                            "<a href='javascript:alert( \"Hi there!\" )'>go</a>" +
                                            "</body></html>" );
        WebConversation wc = new WebConversation();
        WebResponse response = wc.getResponse( getHostPath() + "/OnCommand.html" );
        response.getLinks()[0].click();
        assertEquals( "Alert message", "Hi there!", wc.popNextAlert() );
        assertEquals( "Current page URL", getHostPath() + "/OnCommand.html", wc.getCurrentPage().getURL().toExternalForm() );
    }


    public void testInitialJavaScriptURL() throws Exception {
        WebConversation wc = new WebConversation();
        GetMethodWebRequest request = new GetMethodWebRequest( "javascript:alert( 'Hi there!' )" );
        assertEquals( "Javascript URL", "javascript:alert( 'Hi there!' )", request.getURL().toExternalForm() );
        WebResponse response = wc.getResponse( request );
        assertEquals( "Alert message", "Hi there!", wc.popNextAlert() );
    }


    public void testJavaScriptURLWithVariables() throws Exception {
        defineResource(  "OnCommand.html",  "<html><head></head>" +
                                            "<body>" +
                                            "<a href='javascript:\"Our winner is... \" + document.the_form.winner.value'>go</a>" +
                                            "<form name='the_form'>" +
                                            "  <input name=winner type=text value='George of the Jungle'>" +
                                            "</form></body></html>" );
        WebConversation wc = new WebConversation();
        WebResponse response = wc.getResponse( getHostPath() + "/OnCommand.html" );
        response.getLinks()[0].click();
        assertEquals( "New page", "Our winner is... George of the Jungle", wc.getCurrentPage().getText() );
    }


    public void testJavaScriptURLWithQuestionMark() throws Exception {
        defineResource( "/appname/HandleAction/report?type=C", "You made it!" );
        defineResource(  "OnCommand.html",  "<html><head></head>" +
                                            "<body>" +
                                            "<a href=\"javascript:redirect('/appname/HandleAction/report?type=C')\">go</a>" +
                                            "<script language='JavaScript'>" +
                                            "  function redirect( url ) { window.location=url; }" +
                                            "</script>" +
                                            "</form></body></html>" );
        WebConversation wc = new WebConversation();
        WebResponse response = wc.getResponse( getHostPath() + "/OnCommand.html" );
        response.getLinks()[0].click();
        assertEquals( "New page", "You made it!", wc.getCurrentPage().getText() );
    }


    public void testJavaScriptURLWithIncludedFunction() throws Exception {
        defineResource( "saycheese.js", "function sayCheese() { alert( \"Cheese!\" ); }" );
        defineResource( "OnCommand.html", "<html><head><script language='JavaScript' src='saycheese.js'>" +
                                          "</script></head>" +
                                          "<body>" +
                                          "<a href=\"javascript:sayCheese()\">go</a>" +
                                          "</body></html>" );
        WebConversation wc = new WebConversation();
        WebResponse response = wc.getResponse( getHostPath() + "/OnCommand.html" );
        response.getLinkWith( "go" ).click();
        assertEquals( "Alert message", "Cheese!", wc.popNextAlert() );
    }


    public void testSingleCommandOnLoad() throws Exception {
        defineResource(  "OnCommand.html",  "<html><head></head>" +
                                            "<body onLoad='alert(\"Ouch!\")'></body>" );
        WebConversation wc = new WebConversation();
        WebResponse response = wc.getResponse( getHostPath() + "/OnCommand.html" );
        assertNotNull( "No alert detected", wc.getNextAlert() );
        assertEquals( "Alert message", "Ouch!", wc.popNextAlert() );
        assertNull( "Alert should have been removed", wc.getNextAlert() );
    }


    public void testOnLoadErrorBypass() throws Exception {
        defineResource(  "OnCommand.html",  "<html><head></head>" +
                                            "<body onLoad='noSuchFunction()'>" +
                                            "<img src=sample.jpg>" +
                                            "</body>" );
        WebConversation wc = new WebConversation();
        HttpUnitOptions.setExceptionsThrownOnScriptError( false );
        HttpUnitOptions.clearScriptErrorMessages();

        WebResponse response = wc.getResponse( getHostPath() + "/OnCommand.html" );
        assertEquals( "Number of images on page", 1, response.getImages().length );
        assertEquals( "Number of script failures logged", 1, HttpUnitOptions.getScriptErrorMessages().length );
    }


    public void testConfirmationDialog() throws Exception {
        defineWebPage( "OnCommand", "<a href='NextPage' id='go' onClick='return confirm( \"go on?\" );'>" );
        defineResource( "NextPage", "Got the next page!" );

        WebConversation wc = new WebConversation();
        WebResponse wr = wc.getResponse( getHostPath() + "/OnCommand.html" );
        wc.setDialogResponder( new DialogAdapter() {
            public boolean getConfirmation( String confirmationPrompt ) {
                assertEquals( "Confirmation prompt", "go on?", confirmationPrompt );
                return false;
            }
        } );
        wr.getLinkWithID( "go" ).click();
        assertEquals( "Current page", wr, wc.getCurrentPage() );
        wc.setDialogResponder( new DialogAdapter() );
        wr.getLinkWithID( "go" ).click();
        assertEquals( "Page after confirmation", "Got the next page!", wc.getCurrentPage().getText() );
    }


    public void testPromptDialog() throws Exception {
        defineWebPage( "OnCommand", "<a href='NextPage' id='go' onClick='return \"yes\" == prompt( \"go on?\", \"no\" );'>" );
        defineResource( "NextPage", "Got the next page!" );

        WebConversation wc = new WebConversation();
        WebResponse wr = wc.getResponse( getHostPath() + "/OnCommand.html" );
        wr.getLinkWithID( "go" ).click();
        assertEquals( "Current page", wr, wc.getCurrentPage() );

        wc.setDialogResponder( new DialogAdapter() {
            public String getUserResponse( String prompt, String defaultResponse ) {
                assertEquals( "Confirmation prompt", "go on?", prompt );
                assertEquals( "Default response", "no", defaultResponse );
                return "yes";
            }
        } );
        wr.getLinkWithID( "go" ).click();
        assertEquals( "Page after confirmation", "Got the next page!", wc.getCurrentPage().getText() );
    }


    public void testFunctionCallOnLoad() throws Exception {
        defineResource(  "OnCommand.html",  "<html><head><script language='JavaScript'>" +
                                            "<!-- hide this\n" +
                                            "function sayCheese() { alert( \"Cheese!\" ); }" +
                                            "// end hiding -->\n" +
                                            "</script></head>" +
                                            "<body'><script language='JavaScript'>\n" +
                                            "<!-- hide this\n" +
                                            "sayCheese();" +
                                            "// end hiding -->" +
                                            "</script></body></html>" );
        WebConversation wc = new WebConversation();
        WebResponse response = wc.getResponse( getHostPath() + "/OnCommand.html" );
        assertEquals( "Alert message", "Cheese!", wc.popNextAlert() );
    }


    public void testIncludedFunction() throws Exception {
        defineResource( "saycheese.js", "function sayCheese() { alert( \"Cheese!\" ); }" );
        defineResource( "OnCommand.html", "<html><head><script language='JavaScript' src='saycheese.js'>" +
                                          "</script></head>" +
                                          "<body onLoad='sayCheese()'></body>" );
        WebConversation wc = new WebConversation();
        WebResponse response = wc.getResponse( getHostPath() + "/OnCommand.html" );
        assertEquals( "Alert message", "Cheese!", wc.popNextAlert() );
    }


    public void testWindowOpen() throws Exception {
        defineResource( "Target.html", "You made it!" );
        defineResource( "OnCommand.html", "<html><head><title>Amazing!</title></head>" +
                        "<body><script language='JavaScript'>var otherWindow;</script>" +
                        "<a href='#' onClick=\"otherWindow = window.open( '" + getHostPath() + "/Target.html', 'sample' );\">go</a>" +
                        "<a href='#' onClick=\"otherWindow.close();\">go</a>" +
                        "<a href='#' onClick=\"alert( 'window is ' + (otherWindow.closed ? '' : 'not ') + 'closed' );\">go</a>" +
                        "</body></html>" );
        final ArrayList windowsOpened = new ArrayList();
        WebConversation wc = new WebConversation();
        wc.addWindowListener( new WebWindowListener() {
            public void windowOpened( WebClient client, WebWindow window ) { windowsOpened.add( window ); }
            public void windowClosed( WebClient client, WebWindow window ) {}
        } );
        WebResponse response = wc.getResponse( getHostPath() + "/OnCommand.html" );
        response.getLinks()[0].click();

        assertFalse( "No window opened", windowsOpened.isEmpty() );
        final WebWindow openedWindow = (WebWindow) windowsOpened.get( 0 );
        assertEquals( "New window message", "You made it!", openedWindow.getCurrentPage().getText() );
        assertEquals( "New window name", "sample", openedWindow.getName() );
        response.getLinks()[2].click();
        assertEquals( "Alert message", "window is not closed", wc.popNextAlert() );
        response.getLinks()[1].click();
        assertTrue( "Window was not closed", openedWindow.isClosed() );
        response.getLinks()[2].click();
        assertEquals( "Alert message", "window is closed", wc.popNextAlert() );
    }


    public void testWindowOpenNoContents() throws Exception {
        defineResource( "OnCommand.html", "<html><head><title>Amazing!</title></head>" +
                        "<body>" +
                        "<a href='#' onClick=\"window.open( null, 'sample' );\">go</a>" +
                        "</body></html>" );
        final ArrayList windowsOpened = new ArrayList();
        WebConversation wc = new WebConversation();
        wc.addWindowListener( new WebWindowListener() {
            public void windowOpened( WebClient client, WebWindow window ) { windowsOpened.add( window ); }
            public void windowClosed( WebClient client, WebWindow window ) {}
        } );
        WebResponse response = wc.getResponse( getHostPath() + "/OnCommand.html" );
        response.getLinks()[0].click();

        assertFalse( "No window opened", windowsOpened.isEmpty() );
        final WebWindow openedWindow = (WebWindow) windowsOpened.get( 0 );
        assertEquals( "New window message", "", openedWindow.getCurrentPage().getText() );
        assertEquals( "New window name", "sample", openedWindow.getName() );
        assertEquals( "Window by name", openedWindow, wc.getOpenWindow( "sample" ) );
    }


    public void testWindowReopen() throws Exception {
        defineResource( "Target.html", "You made it!" );
        defineResource( "Revise.html", "You changed it!" );
        defineResource( "OnCommand.html", "<html><head><title>Amazing!</title></head>" +
                        "<body>" +
                        "<a href='#' onClick=\"window.open( '" + getHostPath() + "/Target.html', 'sample' );\">go</a>" +
                        "<a href='#' onClick=\"window.open( '" + getHostPath() + "/Revise.html', 'sample' );\">go</a>" +
                        "</body></html>" );
        final ArrayList windowsOpened = new ArrayList();
        WebConversation wc = new WebConversation();
        wc.addWindowListener( new WebWindowListener() {
            public void windowOpened( WebClient client, WebWindow window ) { windowsOpened.add( window ); }
            public void windowClosed( WebClient client, WebWindow window ) {}
        } );
        WebResponse response = wc.getResponse( getHostPath() + "/OnCommand.html" );
        response.getLinks()[0].click();
        assertEquals( "New window message", "You made it!", ((WebWindow) windowsOpened.get( 0 )).getCurrentPage().getText() );
        response.getLinks()[1].click();

        assertEquals( "Number of window openings", 1, windowsOpened.size() );
        assertEquals( "Changed window message", "You changed it!", ((WebWindow) windowsOpened.get( 0 )).getCurrentPage().getText() );
    }


    public void testOpenedWindowProperties() throws Exception {
        defineResource( "Target.html", "<html><head><script language='JavaScript'>" +
                                       "function show_properties() {" +
                                       "   alert( 'name=' + window.name );" +
                                       "   alert( 'opener name=' + window.opener.name );" +
                                       "}" +
                                       "</script></head><body onload='show_properties()'>" +
                                       "</body></html>" );
        defineResource( "OnCommand.html", "<html><head><title>Amazing!</title></head>" +
                        "<body onload=\"window.name='main'; alert ('opener ' + (window.opener ? 'found' : 'not defined') );\">" +
                        "<a href='#' onClick=\"window.open( '" + getHostPath() + "/Target.html', 'sample' );\">go</a>" +
                        "</body></html>" );
        final ArrayList windowsOpened = new ArrayList();
        WebConversation wc = new WebConversation();
        WebResponse response = wc.getResponse( getHostPath() + "/OnCommand.html" );
        assertEquals( "main window name", "main", wc.getMainWindow().getName() );
        assertEquals( "main window alert", "opener not defined", wc.popNextAlert() );
        response.getLinks()[0].click();

        assertEquals( "1st alert", "name=sample", wc.popNextAlert() );
        assertEquals( "2nd alert", "opener name=main", wc.popNextAlert() );
    }


    public void testFrameProperties() throws Exception {
        HttpUnitOptions.setExceptionsThrownOnScriptError( false );
        defineWebPage( "Linker",  "This is a trivial page with <a href=Target.html>one link</a>" );
        defineResource( "Target.html", "<html><head><script language='JavaScript'>" +
                                       "function show_properties() {" +
                                       "   alert( 'name=' + window.name );" +
                                       "   alert( 'top url=' + window.top.location );" +
                                       "   alert( '1st frame=' + top.frames[0].name );" +
                                       "   alert( '2nd frame=' + window.parent.blue.name );" +
                                       "}" +
                                       "</script></head><body>" +
                                       "<a href=# onclick='show_properties()'>show</a>" +
                                       "</body></html>" );
        defineWebPage( "Form",    "This is a page with a simple form: " +
                                  "<form action=submit><input name=name><input type=submit></form>" +
                                  "<a href=Linker.html target=red>a link</a>");
        defineResource( "Frames.html",
                        "<html><head><title>Initial</title></head>" +
                        "<frameset cols='20%,80%'>" +
                        "    <frame src='Linker.html' name='red'>" +
                        "    <frame src=Target.html name=blue>" +
                        "</frameset></html>" );

        WebConversation wc = new WebConversation();
        WebResponse response = wc.getResponse( getHostPath() + "/Frames.html" );
        WebResponse blue = wc.getFrameContents( "blue" );
        blue.getLinkWith( "show" ).click();

        assertEquals( "1st alert", "name=blue", wc.popNextAlert() );
        assertEquals( "2nd alert", "top url=" + getHostPath() + "/Frames.html", wc.popNextAlert() );
        assertEquals( "3rd alert", "1st frame=red", wc.popNextAlert() );
        assertEquals( "4th alert", "2nd frame=blue", wc.popNextAlert() );
    }


    public void testLocationProperty() throws Exception {
        defineResource( "Target.html", "You made it!" );
        defineResource( "OnCommand.html", "<html><head><title>Amazing!</title></head>" +
                                          "<body onLoad='alert(\"Window location is \" + window.location);alert(\"Document location is \" + document.location)'>" +
                                          "<a href='#' onMouseOver=\"window.location='" + getHostPath() + "/Target.html';\">go</a>" +
                                          "<a href='#' onMouseOver=\"document.location='" + getHostPath() + "/Target.html';\">go</a>" +
                                          "</body>" );
        WebConversation wc = new WebConversation();
        WebResponse response = wc.getResponse( getHostPath() + "/OnCommand.html" );
        assertEquals( "Alert message", "Window location is " + getHostPath() + "/OnCommand.html", wc.popNextAlert() );
        assertEquals( "Alert message", "Document location is " + getHostPath() + "/OnCommand.html", wc.popNextAlert() );
        response.getLinks()[0].mouseOver();
        assertEquals( "2nd page URL", getHostPath() + "/Target.html", wc.getCurrentPage().getURL().toExternalForm() );
        assertEquals( "2nd page", "You made it!", wc.getCurrentPage().getText() );

        response = wc.getResponse( getHostPath() + "/OnCommand.html" );
        response.getLinks()[1].mouseOver();
        assertEquals( "3rd page URL", getHostPath() + "/Target.html", wc.getCurrentPage().getURL().toExternalForm() );
        assertEquals( "3rd page", "You made it!", wc.getCurrentPage().getText() );
    }


    public void testScriptDisabled() throws Exception {
        HttpUnitOptions.setScriptingEnabled( false );
        defineResource( "nothing.html", "Should get here" );
        defineResource(  "OnCommand.html",  "<html><head></head>" +
                                            "<body>" +
                                            "<form name='realform'><input name='color' value='blue'></form>" +
                                            "<a href='nothing.html' onClick=\"document.realform.color.value='green';return false;\">green</a>" +
                                            "</body></html>" );
        WebConversation wc = new WebConversation();
        WebResponse response = wc.getResponse( getHostPath() + "/OnCommand.html" );
        WebForm form = response.getFormWithName( "realform" );
        WebLink link = response.getLinks()[0];
        assertEquals( "initial parameter value", "blue", form.getParameterValue( "color" ) );
        link.click();
        assertEquals( "unchanged parameter value", "blue", form.getParameterValue( "color" ) );
        assertEquals( "Expected result", "Should get here", wc.getCurrentPage().getText() );
    }


    public void testNavigatorObject() throws Exception {
        defineResource(  "OnCommand.html",  "<html><head><script language='JavaScript'>" +
                                            "function viewProperties() { \n" +
                                            "  alert( 'appName=' + navigator.appName );\n" +
                                            "  alert( 'appCodeName=' + navigator.appCodeName )\n;" +
                                            "  alert( 'appVersion=' + navigator.appVersion )\n;" +
                                            "  alert( 'userAgent=' + navigator.userAgent )\n;" +
                                            "  alert( 'platform=' + navigator.platform )\n;" +
                                            "  alert( 'javaEnabled=' + navigator.javaEnabled() )\n;" +
                                            "  alert( '# plugins=' + navigator.plugins.length )\n;" +
                                            "}" +
                                            "</script></head>\n" +
                                            "<body onLoad='viewProperties()'>\n" +
                                            "</body></html>" );
        HttpUnitOptions.setExceptionsThrownOnScriptError( true );
        WebConversation wc = new WebConversation();
        wc.getClientProperties().setApplicationID( "Internet Explorer", "Mozilla", "4.0" );
        wc.getClientProperties().setPlatform( "JVM" );
        WebResponse response = wc.getResponse( getHostPath() + "/OnCommand.html" );
        assertEquals( "Alert message 1", "appName=Internet Explorer", wc.popNextAlert() );
        assertEquals( "Alert message 2", "appCodeName=Mozilla", wc.popNextAlert() );
        assertEquals( "Alert message 3", "appVersion=4.0", wc.popNextAlert() );
        assertEquals( "Alert message 4", "userAgent=Mozilla/4.0", wc.popNextAlert() );
        assertEquals( "Alert message 5", "platform=JVM", wc.popNextAlert() );
        assertEquals( "Alert message 6", "javaEnabled=false", wc.popNextAlert() );
        assertEquals( "Alert message 7", "# plugins=0", wc.popNextAlert() );
        assertNull( "Alert should have been removed", wc.getNextAlert() );
    }


    public void testScreenObject() throws Exception {
        defineResource(  "OnCommand.html",  "<html><head><script language='JavaScript'>" +
                                            "function viewProperties() { \n" +
                                            "  alert( 'dimensions=' + screen.availWidth + 'x' + screen.availHeight );\n" +
                                            "}" +
                                            "</script></head>\n" +
                                            "<body onLoad='viewProperties()'>\n" +
                                            "</body></html>" );
        HttpUnitOptions.setExceptionsThrownOnScriptError( true );
        WebConversation wc = new WebConversation();
        wc.getClientProperties().setAvailableScreenSize( 1024, 752 );
        WebResponse response = wc.getResponse( getHostPath() + "/OnCommand.html" );
        assertEquals( "Alert message 1", "dimensions=1024x752", wc.popNextAlert() );
        assertNull( "Alert should have been removed", wc.getNextAlert() );
    }


}
