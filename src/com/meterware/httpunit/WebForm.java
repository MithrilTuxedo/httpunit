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

import java.util.*;

import org.w3c.dom.*;

/**
 * This class represents a form in an HTML page. Users of this class may examine the parameters
 * defined for the form, the structure of the form (as a DOM), or the text of the form. They
 * may also create a {@link WebRequest} to simulate the submission of the form.
 **/
public class WebForm {


    /**
     * Returns the target for this link.
     **/
    public String getTarget() {
        if (_node.getAttributes().getNamedItem( "target" ) == null) {
            return _parentTarget;
        } else {
            return getValue( _node.getAttributes().getNamedItem( "target" ) );
        }
    }


    /**
     * Returns an array containing the names of the parameters defined for this form,
     * in the order in which they appear.
     **/
    public String[] getParameterNames() {
        Vector parameterNames = new Vector();
        NamedNodeMap[] parameters = getParameters();

        for (int i = 0; i < parameters.length; i++) {
            String name = getValue( parameters[i].getNamedItem( "name" ) );
            if (!parameterNames.contains( name )) parameterNames.addElement( name );
        }

        HTMLSelectElement[] selections = getSelections();
        for (int i = 0; i < selections.length; i++) {
            parameterNames.addElement( selections[i].getName() );
        }

        HTMLTextAreaElement[] textAreas = getTextAreas();
        for (int i = 0; i < textAreas.length; i++) {
            parameterNames.addElement( textAreas[i].getName() );
        }

        String[] result = new String[ parameterNames.size() ];
        parameterNames.copyInto( result );
        return result;
    }


    /**
     * Creates and returns a web request which will simulate the submission of this form.
     **/
    public WebRequest getRequest() {
        NamedNodeMap nnm = _node.getAttributes();
        String action = getValue( nnm.getNamedItem( "action" ) );
        WebRequest result;

        if (getValue( nnm.getNamedItem( "method" ) ).equalsIgnoreCase( "post" )) {
            result = new PostMethodWebRequest( _baseURL, action, getTarget(), this );
        } else {
            result = new GetMethodWebRequest( _baseURL, action, getTarget(), this );
        }

        String[] parameterNames = getParameterNames();
        for (int i = 0; i < parameterNames.length; i++) {
            if (getParameterDefaults().get( parameterNames[i] ) != null) {
                Object value = getParameterDefaults().get( parameterNames[i] );
                if (value instanceof String) {
                    result.setParameter( parameterNames[i], (String) value);
                } else {
                    result.setParameter( parameterNames[i], (String[]) value);
                }
            }
        }
        return result;

    }


    /**
     * Returns the default value of the named parameter.
     **/
    public String getParameterValue( String name ) {
        Object result = getParameterDefaults().get( name );
        if (result instanceof String) return (String) result;
        if (result instanceof String[] && ((String[]) result).length > 0) return ((String[]) result)[0];
        return "";
    }


    /**
     * Returns the multiple default values of the named parameter.
     **/
    public String[] getParameterValues( String name ) {
        Object result = getParameterDefaults().get( name );
        if (result instanceof String) return new String[] { (String) result };
        if (result instanceof String[]) return (String[]) result;
        return new String[0];
    }


    /**
     * Returns the displayed options defined for the specified parameter name.
     **/
    public String[] getOptions( String name ) {
        Object result = getParameterOptions().get( name );
        if (result instanceof String[]) return (String[]) result;
        return new String[0];
    }


    /**
     * Returns the option values defined for the specified parameter name.
     **/
    public String[] getOptionValues( String name ) {
        Object result = getParameterOptionValues().get( name );
        if (result instanceof String[]) return (String[]) result;
        return new String[0];
    }


    /**
     * Returns true if the named parameter accepts multiple values.
     **/
    public boolean isMultiValuedParameter( String name ) {
        return TYPE_MULTI_VALUED.equals( getParameterTypes().get( name ) );
    }


    /**
     * Returns true if the named parameter accepts free-form text.
     **/
    public boolean isTextParameter( String name ) {
        return TYPE_TEXT.equals( getParameterTypes().get( name ) );
    }


    /**
     * Returns a copy of the domain object model subtree associated with this form.
     **/
    public Node getDOMSubtree() {
        return _node.cloneNode( /* deep */ true );
    }


//---------------------------------- package members --------------------------------

    /**
     * Contructs a web form given the URL of its source page and the DOM extracted
     * from that page.
     **/
    WebForm( URL baseURL, String parentTarget, Node node ) {
        _node         = node;
        _baseURL      = baseURL;
        _parentTarget = parentTarget;
    }


//---------------------------------- private members --------------------------------

    /** The type of a parameter which accepts any text. **/
    private final static Integer TYPE_TEXT = new Integer(1);

    /** The type of a parameter which accepts single predefined values. **/
    private final static Integer TYPE_SCALAR = new Integer(2);

    /** The type of a parameter which accepts multiple predefined values. **/
    private final static Integer TYPE_MULTI_VALUED = new Integer(3);


    /** The URL of the page containing this form. **/
    private URL            _baseURL;

    /** The DOM node representing the form. **/
    private Node           _node;

    /** The attributes of the form parameters. **/
    private NamedNodeMap[] _parameters;

    /** The parameters with their default values. **/
    private Hashtable      _defaults;

    /** The parameters with their displayed options. **/
    private Hashtable      _options;

    /** The parameters with their options. **/
    private Hashtable      _optionValues;

    /** The parameters mapped to the type of data which they accept. **/
    private Hashtable      _dataTypes;

    /** The target in which the parent response is to be rendered. **/
    private String         _parentTarget;

    /** The selections in this form. **/
    private HTMLSelectElement[] _selections;


    /** The text areas in this form. **/
    private HTMLTextAreaElement[] _textAreas;


    private String getValue( Node node ) {
        return (node == null) ? "" : node.getNodeValue();
    }


    private Hashtable getParameterDefaults() {
        if (_defaults == null) {
            NamedNodeMap[] parameters = getParameters();
            Hashtable defaults = new Hashtable();
            for (int i = 0; i < parameters.length; i++) {
                String name  = getValue( parameters[i].getNamedItem( "name" ) );
                String value = getValue( parameters[i].getNamedItem( "value" ) );
                String type  = getValue( parameters[i].getNamedItem( "type" ) ).toUpperCase();
                if (type == null || type.length() == 0) type = "TEXT";

                if (type.equals( "TEXT" ) || type.equals( "HIDDEN" ) || type.equals( "PASSWORD" )) {
                    defaults.put( name, value );
                } else if (type.equals( "RADIO" ) && parameters[i].getNamedItem( "checked" ) != null) {
                    defaults.put( name, value );
                } else if (type.equals( "CHECKBOX" ) && parameters[i].getNamedItem( "checked" ) != null) {
                    if (value.length() == 0) value = "on";
                    String[] currentDefaults = (String[]) defaults.get( name );
                    if (currentDefaults == null) {
                        defaults.put( name, new String[] { value } );
                    } else {
                        defaults.put( name, withNewValue( currentDefaults, value ) );
                    }
                }
            }
            HTMLSelectElement[] selections = getSelections();
            for (int i = 0; i < selections.length; i++) {
                defaults.put( selections[i].getName(), selections[i].getSelected() );
            }
            HTMLTextAreaElement[] textAreas = getTextAreas();
            for (int i = 0; i < textAreas.length; i++) {
                defaults.put( textAreas[i].getName(), textAreas[i].getValue() );
            }
            _defaults = defaults;
        }
        return _defaults;
    }


    private Hashtable getParameterOptions() {
        if (_options == null) {
            Hashtable options = new Hashtable();
            HTMLSelectElement[] selections = getSelections();
            for (int i = 0; i < selections.length; i++) {
                options.put( selections[i].getName(), selections[i].getOptions() );
            }
            _options = options;
        }
        return _options;
    }


    private Hashtable getParameterOptionValues() {
        if (_optionValues == null) {
            Hashtable options = new Hashtable();
            NamedNodeMap[] parameters = getParameters();
            for (int i = 0; i < parameters.length; i++) {
                String name  = getValue( parameters[i].getNamedItem( "name" ) );
                String value = getValue( parameters[i].getNamedItem( "value" ) );
                String type  = getValue( parameters[i].getNamedItem( "type" ) ).toUpperCase();
                if (type == null || type.length() == 0) type = "TEXT";

                if (type.equals( "RADIO" ) || type.equals( "CHECKBOX" )) {
                    if (value.length() == 0 && type.equals( "CHECKBOX" )) value = "on";
                    String[] radioOptions = (String[]) options.get( name );
                    if (radioOptions == null) {
                        options.put( name, new String[] { value } );
                    } else {
                        options.put( name, withNewValue( radioOptions, value ) );
                    }
                }
            }
            HTMLSelectElement[] selections = getSelections();
            for (int i = 0; i < selections.length; i++) {
                options.put( selections[i].getName(), selections[i].getOptionValues() );
            }
            _optionValues = options;
        }
        return _optionValues;
    }


    private String[] withNewValue( String[] group, String value ) {
        String[] result = new String[ group.length+1 ];
        System.arraycopy( group, 0, result, 1, group.length );
        result[0] = value;
        return result;
    }


    private Hashtable getParameterTypes() {
        if (_dataTypes == null) {
            NamedNodeMap[] parameters = getParameters();
            Hashtable types = new Hashtable();
            for (int i = 0; i < parameters.length; i++) {
                String name  = getValue( parameters[i].getNamedItem( "name" ) );
                String type  = getValue( parameters[i].getNamedItem( "type" ) ).toUpperCase();
                if (type == null || type.length() == 0) type = "TEXT";

                if (type.equals( "TEXT" ) || type.equals( "HIDDEN" ) || type.equals( "PASSWORD" )) {
                    types.put( name, TYPE_TEXT );
                } else if (type.equals( "RADIO" )) {
                    types.put( name, TYPE_SCALAR );
                } else if (type.equals( "CHECKBOX" )) {
                    types.put( name, TYPE_MULTI_VALUED );
                }
            }
            HTMLSelectElement[] selections = getSelections();
            for (int i = 0; i < selections.length; i++) {
                types.put( selections[i].getName(), selections[i].isMultiSelect() ? TYPE_MULTI_VALUED : TYPE_SCALAR );
            }
            HTMLTextAreaElement[] textAreas = getTextAreas();
            for (int i = 0; i < textAreas.length; i++) {
                types.put( textAreas[i].getName(), TYPE_TEXT );
            }
            _dataTypes = types;
        }
        return _dataTypes;
    }


    /**
     * Returns an array of select control descriptors for this form.
     **/
    private HTMLSelectElement[] getSelections() {
        if (_selections == null) {
            NodeList nl = ((Element) _node).getElementsByTagName( "select" );
            HTMLSelectElement[] result = new HTMLSelectElement[ nl.getLength() ];
            for (int i = 0; i < result.length; i++) {
                result[i] = new HTMLSelectElement( nl.item(i) );
            }
            _selections = result;
        }
        return _selections;
    }

    /**
     * Returns an array of select control descriptors for this form.
     **/
    private HTMLTextAreaElement[] getTextAreas() {
        if (_textAreas == null) {
            NodeList nl = ((Element) _node).getElementsByTagName( "textarea" );
            HTMLTextAreaElement[] result = new HTMLTextAreaElement[ nl.getLength() ];
            for (int i = 0; i < result.length; i++) {
                result[i] = new HTMLTextAreaElement( nl.item(i) );
            }
            _textAreas = result;
        }
        return _textAreas;
    }

    /**
     * Returns an array of form parameter attributes for this form.
     **/
    private NamedNodeMap[] getParameters() {
        if (_parameters == null) {
            Vector list = new Vector();
            if (_node.hasChildNodes()) addFormParametersToList( _node.getChildNodes(), list );
            _parameters = new NamedNodeMap[ list.size() ];
            list.copyInto( _parameters );
        }

        return _parameters;
    }


    private void addFormParametersToList( NodeList children, Vector list ) {
        for (int i = 0; i < children.getLength(); i++) {
            addFormParametersToList( children.item(i), list );
        }
    }


    private void addFormParametersToList( Node child, Vector list ) {
        if (isFormParameter( child )) {
            list.addElement( child.getAttributes() );
        } else if (child.hasChildNodes()) {
            addFormParametersToList( child.getChildNodes(), list );
        }
    }


    private boolean isFormParameter( Node node ) {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return false;
        } else if (!node.getNodeName().equals( "input" )) {
            return false;
        } else {
            NamedNodeMap nnm = node.getAttributes();
            Node n = nnm.getNamedItem( "type" );
            if (n == null) {
                return true;
            } else if (n.getNodeValue().equalsIgnoreCase( "submit" )) {
                return false;
            } else if (n.getNodeValue().equalsIgnoreCase( "reset" )) {
                return false;
            } else {
                return true;
            }
        }
    }


    class HTMLSelectElement {
        HTMLSelectElement( Node node ) {
            if (!node.getNodeName().equalsIgnoreCase( "select" )) {
                throw new RuntimeException( "Not a select element" );
            }
            _node = node;
        }
    
    
        private Node _node;
    
    
        String getName() {
            NamedNodeMap nnm = _node.getAttributes();
            return getValue( nnm.getNamedItem( "name" ) );
        }


        String[] getSelected() {
            Vector selected = new Vector();
            NodeList nl = ((Element) _node).getElementsByTagName( "option" );
            for (int i = 0; i < nl.getLength(); i++) {
                if (nl.item(i).getAttributes().getNamedItem( "selected" ) != null) {
                    selected.addElement( getOptionValue( nl.item(i) ) );
                }
            }

            if (!isMultiSelect() && selected.size() == 0 && nl.getLength() > 0) {
                selected.addElement( getOptionValue( nl.item(0) ) );
            }

            String[] result = new String[ selected.size() ];
            selected.copyInto( result );
            return result;
        }


        String[] getOptions() {
            Vector options = new Vector();
            NodeList nl = ((Element) _node).getElementsByTagName( "option" );
            for (int i = 0; i < nl.getLength(); i++) {
                options.addElement( getValue( nl.item(i).getFirstChild() ) );
            }
            String[] result = new String[ options.size() ];
            options.copyInto( result );
            return result;
        }


        String[] getOptionValues() {
            Vector options = new Vector();
            NodeList nl = ((Element) _node).getElementsByTagName( "option" );
            for (int i = 0; i < nl.getLength(); i++) {
                options.addElement( getOptionValue( nl.item(i) ) );
            }
            String[] result = new String[ options.size() ];
            options.copyInto( result );
            return result;
        }


        boolean isMultiSelect() {
            return _node.getAttributes().getNamedItem( "multiple" ) != null;
        }


        private String getOptionValue( Node optionNode ) {
            NamedNodeMap nnm = optionNode.getAttributes();
            if (nnm.getNamedItem( "value" ) != null) {
                return getValue( nnm.getNamedItem( "value" ) );
            } else {
                return getValue( optionNode.getFirstChild() );
            }
        }
    }


    class HTMLTextAreaElement {
        HTMLTextAreaElement( Node node ) {
            if (!node.getNodeName().equalsIgnoreCase( "textarea" )) {
                throw new RuntimeException( "Not a textarea element" );
            }
            _node = node;
        }
    
    
        private Node _node;
    
    
        String getName() {
            NamedNodeMap nnm = _node.getAttributes();
            return WebForm.this.getValue( nnm.getNamedItem( "name" ) );
        }


        String getValue() {
            return NodeUtils.asText(_node.getChildNodes() );
        }
    }



}

