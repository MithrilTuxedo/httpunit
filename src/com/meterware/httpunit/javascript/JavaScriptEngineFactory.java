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
import com.meterware.httpunit.ScriptingEngineFactory;
import com.meterware.httpunit.WebResponse;

import java.lang.reflect.InvocationTargetException;

import org.mozilla.javascript.ClassDefinitionException;
import org.mozilla.javascript.NotAFunctionException;
import org.mozilla.javascript.PropertyException;
import org.mozilla.javascript.JavaScriptException;
import org.xml.sax.SAXException;

/**
 *
 * @author <a href="mailto:russgold@acm.org">Russell Gold</a>
 **/
public class JavaScriptEngineFactory implements ScriptingEngineFactory {

    public void associate( WebResponse response ) {
        try {
            JavaScript.run( response );
        } catch (Exception e) {
            throw new RuntimeException( e.toString() );
        }
    }
}