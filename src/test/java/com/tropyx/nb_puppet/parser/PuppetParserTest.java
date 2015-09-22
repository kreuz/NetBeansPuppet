/*
 * Copyright (C) 2015 mkleint
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.tropyx.nb_puppet.parser;

import java.util.List;
import javax.swing.text.BadLocationException;
import org.junit.Test;
import org.netbeans.editor.BaseDocument;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;

/**
 *
 * @author mkleint
 */
public class PuppetParserTest extends NbTestCase {

    public PuppetParserTest(String name) {
        super(name);
    }

    @Test
    public void testSimpleClassParse() throws Exception {
        PuppetParserResult result = doParse("class aaa { }");
        PClass c = assertAndGetClassElement(result);
        assertEquals("aaa", c.getName());
    }

    @Test
    public void testSimpleClassParse2() throws Exception {
        PuppetParserResult result = doParse("class aaa::param { }");
        PClass c = assertAndGetClassElement(result);
        assertEquals("aaa::param", c.getName());
    }

    @Test
    public void testInheritedClassParse() throws Exception {
        PuppetParserResult result = doParse("class aaa inherits aaa::params { }");
        PClass c = assertAndGetClassElement(result);
        assertEquals("aaa", c.getName());
        assertEquals("aaa::params", c.getInherits().getName());
    }

    @Test
    public void testInheritedClassWithEmptyParamsParse() throws Exception {
        PuppetParserResult result = doParse("class aaa() inherits aaa::params { }");
        PClass c = assertAndGetClassElement(result);
        assertEquals("aaa", c.getName());
        assertEquals("aaa::params", c.getInherits().getName());
    }

    @Test
    public void testClassWithSingleParamParse() throws Exception {
        PuppetParserResult result = doParse("class aaa ( $bb = '' ) { }");
        PClass c = assertAndGetClassElement(result);
        assertEquals("aaa", c.getName());
        assertNotNull(c.getParams());
        assertEquals(1, c.getParams().length);
        PClassParam p = c.getParams()[0];
        assertEquals("$bb", p.getVariable().getName());
        assertEquals("Any", p.getTypeType());
    }

    @Test
    public void testClassWithMultiParamsParse() throws Exception {
        PuppetParserResult result = doParse("class aaa ( $bb = '',Regexp $cc = /aaa/, $dd=$aa::aa,) { }");
        PClass c = assertAndGetClassElement(result);
        assertEquals("aaa", c.getName());
        assertNotNull(c.getParams());
        assertEquals(3, c.getParams().length);
        PClassParam p = c.getParams()[0];
        assertEquals("$bb", p.getVariable().getName());
        assertEquals("Any", p.getTypeType());
        p = c.getParams()[1];
        assertEquals("$cc", p.getVariable().getName());
        assertEquals("Regexp", p.getTypeType());
        p = c.getParams()[2];
        assertEquals("$dd", p.getVariable().getName());
        assertEquals("Any", p.getTypeType());
    }

    @Test
    public void testClassWithBlobParamParse() throws Exception {
        PuppetParserResult result = doParse("class aaa ( $bb = { aa => 'aa', cc => 'cc' }, $dd = 'dd,' ) { }");
        PClass c = assertAndGetClassElement(result);
        assertEquals("aaa", c.getName());
        assertNotNull(c.getParams());
        assertEquals(2, c.getParams().length);
        PClassParam p = c.getParams()[0];
        assertEquals("$bb", p.getVariable().getName());
        assertEquals("Any", p.getTypeType());
        p = c.getParams()[1];
        assertEquals("$dd", p.getVariable().getName());
        assertEquals("Any", p.getTypeType());
    }


    @Test
    public void testClassIncludeParse() throws Exception {
        PuppetParserResult result = doParse(
                "class aaa::param { "
             +  " include bbb::param"
             + " }");
        PClass c = assertAndGetClassElement(result);
        assertEquals("aaa::param", c.getName());
        assertEquals(1, c.getIncludes().size());
        assertEquals("bbb::param", c.getIncludes().get(0).getName());
    }

    @Test
    public void testClassIncludesParse() throws Exception {
        PuppetParserResult result = doParse(
                "class aaa::param { "
             +  " include bbb::param"
             +  " include ccc::param"
             + " }");
        PClass c = assertAndGetClassElement(result);
        assertEquals("aaa::param", c.getName());
        assertEquals(2, c.getIncludes().size());
        assertEquals("bbb::param", c.getIncludes().get(0).getName());
        assertEquals("ccc::param", c.getIncludes().get(1).getName());
    }

    @Test
    public void testClassBracingParse() throws Exception {
        PuppetParserResult result = doParse(
                "class aaa::param { "
             +  " {  } "
             +  " { { } }"
             + " }"
             + "   include bbb:param");
        PClass c = assertAndGetClassElement(result);
        assertEquals("aaa::param", c.getName());
        assertEquals(0, c.getIncludes().size());
    }

    @Test
    public void testSimpleResourceParse() throws Exception {
        PuppetParserResult result = doParse(
                "class aaa::install { "
             +  " file { \"fff\":"
              + " ensure => present, "
             +  " path => \'aaaa\',"
             + " }"
             + " }");
        PClass c = assertAndGetClassElement(result);
        assertEquals("aaa::install", c.getName());
        PResource res = (PResource) c.getChildren().get(0);
        assertEquals("file", res.getResourceType());
        assertNotNull(res.getTitle());
        assertEquals(PString.STRING, res.getTitle().getType());
        assertEquals(2, res.getAtributes().size());
        assertEquals("ensure", res.getAtributes().get(0).getName());
//        assertEquals("present", res.getAtributes().get(0).getValue());
    }

    @Test
    public void testSimpleResourceParse2() throws Exception {
        PuppetParserResult result = doParse(
                "class aaa::install { "
             +  " file { $aaa::params::fff :"
              + " ensure => present, "
             +  " path => \'aaaa\',"
              + " foo => 644"
             + " }"
             + " }");
        PClass c = assertAndGetClassElement(result);
        assertEquals("aaa::install", c.getName());
        PResource res = (PResource) c.getChildren().get(0);
        assertEquals("file", res.getResourceType());
        assertNotNull(res.getTitle());
        assertEquals(PString.VARIABLE, res.getTitle().getType());
        assertEquals("$aaa::params::fff", ((PVariable)res.getTitle()).getName());
        assertEquals(3, res.getAtributes().size());
        assertEquals("foo", res.getAtributes().get(2).getName());
//        assertEquals("644", res.getAtributes().get(2).getValue());
    }

    @Test
    public void testClassVariableAssignmentParse() throws Exception {
        PuppetParserResult result = doParse(
                "class aaa::param { "
             +  " $aaa::fff::ss = $bbb\n"
             +  " $aaa::fff = 666\n"
             + " }");
        PClass c = assertAndGetClassElement(result);
        assertEquals("aaa::param", c.getName());
        List<PVariable> vars = c.getChildrenOfType(PVariable.class, true);
        assertEquals(1, vars.size());
        assertEquals("$bbb", vars.get(0).getName());
        List<PVariableDefinition> varDefs = c.getChildrenOfType(PVariableDefinition.class, true);
        assertEquals(2, varDefs.size());
        assertEquals("$aaa::fff::ss", varDefs.get(0).getName());
        assertEquals("$aaa::fff", varDefs.get(1).getName());

    }
    @Test
    public void testClassVariableAssignment2Parse() throws Exception {
        PuppetParserResult result = doParse(
                "class aaa::param { "
             +  " $aaa = { aaa => 'aaa' , bbb => 'bbb' }\n"
             +  " $aaa::fff::ss = hiera('aaa')\n"
             +  " $aaa::fff = [ 'aaa', 'bbb' ]\n"
             + " }");
        PClass c = assertAndGetClassElement(result);
        assertEquals("aaa::param", c.getName());
        List<PVariableDefinition> varDefs = c.getChildrenOfType(PVariableDefinition.class, true);
        assertEquals(3, varDefs.size());
        assertEquals("$aaa", varDefs.get(0).getName());
        assertEquals("$aaa::fff::ss", varDefs.get(1).getName());
        assertEquals("$aaa::fff", varDefs.get(2).getName());

    }


    private PClass assertAndGetClassElement(PuppetParserResult result) {
        PElement nd = result.getRootNode();
        assertNotNull(nd);
        List<PElement> children = nd.getChildren();
        assertNotNull(children);
        assertEquals(1, children.size());
        PElement ch = children.get(0);
        assertEquals(PElement.CLASS, ch.getType());
        PClass c = (PClass)ch;
        return c;
    }

    private PuppetParserResult doParse(String string) throws ParseException, BadLocationException {
        BaseDocument bd = new BaseDocument(false, "text/x-puppet-manifest");
        bd.insertString(0, string, null);

        Snapshot snap = Source.create(bd).createSnapshot();
        PuppetParser pp = new PuppetParser();
        UserTask ut = new UserTask() {

            @Override
            public void run(ResultIterator resultIterator) throws Exception {
            }
        };
        pp.parse(snap, ut, null);
        return (PuppetParserResult) pp.getResult(ut);
    }

    
}
