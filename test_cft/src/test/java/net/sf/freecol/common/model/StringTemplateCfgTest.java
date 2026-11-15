package net.sf.freecol.common.model;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import javax.xml.stream.XMLStreamException;
import junit.framework.TestCase;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.StringTemplate.TemplateType;

public class StringTemplateCfgTest extends TestCase {
    public static class ChildTemplate extends StringTemplate {
        public ChildTemplate() { super(); }
    }

    private String xml(StringTemplate value) throws Exception {
        StringWriter out = new StringWriter();
        try (FreeColXMLWriter writer = new FreeColXMLWriter(out,
                FreeColXMLWriter.WriteScope.toSave(), false)) {
            value.toXML(writer);
            writer.flush();
        }
        return out.toString();
    }

    private StringTemplate read(String text) throws Exception {
        try (FreeColXMLReader reader = new FreeColXMLReader(new StringReader(text))) {
            reader.nextTag();
            return new StringTemplate(reader);
        }
    }

    private void child(StringTemplate target, String text) throws Exception {
        try (FreeColXMLReader reader = new FreeColXMLReader(new StringReader(text))) {
            reader.nextTag();
            target.readChild(reader);
        }
    }

    private StringTemplate allocatedEmpty(String id) {
        StringTemplate value = StringTemplate.template(id);
        value.addName("%x%", "A");
        value.entryList().clear();
        return value;
    }

    public void testTC001() throws Exception {
        StringTemplate t = new StringTemplate();
        assertSame(null, t.getId());
        assertSame(null, t.getDefaultId());
        assertEquals("KEY", t.getTemplateType().name());
        assertTrue(t.isEmpty());
        assertEquals("stringTemplate", t.getXMLTagName());
    }

    public void testTC002() throws Exception {
        StringTemplate t = new StringTemplate("base", "fallback", TemplateType.LABEL);
        assertEquals("base", t.getId());
        assertEquals("fallback", t.getDefaultId());
        assertEquals("LABEL", t.getTemplateType().name());
        assertTrue(t.isEmpty());
    }

    public void testTC003() throws Exception {
        assertEquals("NAME", StringTemplate.name("Alice").getTemplateType().name());
        assertEquals("Alice", StringTemplate.name("Alice").getId());
        assertEquals("KEY", StringTemplate.key("title").getTemplateType().name());
        assertEquals("TEMPLATE", StringTemplate.template("base").getTemplateType().name());
        assertEquals("LABEL", StringTemplate.label(",").getTemplateType().name());
        assertSame(null, StringTemplate.key("title").getDefaultId());
    }

    public void testTC004() throws Exception {
        Named n = () -> "named.key";
        assertEquals("named.key", StringTemplate.key(n).getId());
        assertEquals("KEY", StringTemplate.key(n).getTemplateType().name());
        assertEquals("named.key", StringTemplate.template(n).getId());
        assertEquals("TEMPLATE", StringTemplate.template(n).getTemplateType().name());
    }

    public void testTC005() throws Exception {
        StringTemplate source = StringTemplate.template("base");
        source.setDefaultId("fallback");
        source.addName("%x%", "A");
        StringTemplate copy = StringTemplate.copy("copy", source);
        assertEquals("copy", copy.getId());
        assertEquals("TEMPLATE", copy.getTemplateType().name());
        assertEquals("fallback", copy.getDefaultId());
        assertSame(source.entryList(), copy.entryList());
        source.addName("%y%", "B");
        assertEquals(2, copy.entryList().size());
    }

    public void testTC006() throws Exception {
        StringTemplate t = StringTemplate.key("base");
        assertSame(t, t.setDefaultId("fallback", StringTemplate.class));
        assertEquals("fallback", t.getDefaultId());
    }

    public void testTC007() throws Exception {
        ChildTemplate t = new ChildTemplate();
        assertSame(t, t.setDefaultId("childFallback", ChildTemplate.class));
        assertEquals("childFallback", t.getDefaultId());
    }

    public void testTC008() throws Exception {
        StringTemplate t = StringTemplate.key("base");
        assertSame(null, t.setDefaultId("fallback", ChildTemplate.class));
        assertEquals("fallback", t.getDefaultId());
    }

    public void testTC009() throws Exception {
        StringTemplate t = StringTemplate.template("base");
        assertTrue(t.isEmpty());
        t.addName("%x%", "A");
        assertFalse(t.isEmpty());
        t.entryList().clear();
        assertTrue(t.isEmpty());
    }

    public void testTC010() throws Exception {
        StringTemplate t = StringTemplate.template("base");
        assertTrue(t.entryList().isEmpty());
        t.addName("%x%", "A");
        assertEquals(1, t.entryList().size());
        assertSame(t.entryList(), t.entryList());
    }

    public void testTC011() throws Exception {
        StringTemplate t = StringTemplate.template("base");
        assertSame(null, t.getReplacement("%x%"));
        t.addName("%x%", "A");
        assertEquals("A", t.getReplacement("%x%").getId());
        assertSame(null, t.getReplacement("%missing%"));
        StringTemplate label = StringTemplate.label(",").addName("B");
        assertEquals("B", label.getReplacement(null).getId());
    }

    public void testTC012() throws Exception {
        StringTemplate t = StringTemplate.template("base");
        StringTemplate replacement = StringTemplate.name("Alice");
        assertSame(t, t.add("%x%", "value.key"));
        assertEquals(1, t.entryList().size());
        assertEquals("KEY", t.entryList().get(0).getValue().getTemplateType().name());
        assertEquals("value.key", t.entryList().get(0).getValue().getId());
    }

    public void testTC013() throws Exception {
        StringTemplate t = StringTemplate.label(",");
        StringTemplate replacement = StringTemplate.name("Alice");
        assertSame(t, t.add("value.key"));
        assertEquals(1, t.entryList().size());
        assertEquals("KEY", t.entryList().get(0).getValue().getTemplateType().name());
        assertEquals("value.key", t.entryList().get(0).getValue().getId());
    }

    public void testTC014() throws Exception {
        StringTemplate t = StringTemplate.template("base");
        StringTemplate replacement = StringTemplate.name("Alice");
        assertSame(t, t.addName("%x%", "Alice"));
        assertEquals(1, t.entryList().size());
        assertEquals("NAME", t.entryList().get(0).getValue().getTemplateType().name());
        assertEquals("Alice", t.entryList().get(0).getValue().getId());
    }

    public void testTC015() throws Exception {
        StringTemplate t = StringTemplate.template("base");
        StringTemplate replacement = StringTemplate.name("Alice");
        assertSame(t, t.addName("%x%", (FreeColObject)StringTemplate.name("object.id")));
        assertEquals(1, t.entryList().size());
        assertEquals("KEY", t.entryList().get(0).getValue().getTemplateType().name());
        assertEquals("object.id.name", t.entryList().get(0).getValue().getId());
    }

    public void testTC016() throws Exception {
        StringTemplate t = StringTemplate.label(",");
        StringTemplate replacement = StringTemplate.name("Alice");
        assertSame(t, t.addName("Alice"));
        assertEquals(1, t.entryList().size());
        assertEquals("NAME", t.entryList().get(0).getValue().getTemplateType().name());
        assertEquals("Alice", t.entryList().get(0).getValue().getId());
    }

    public void testTC017() throws Exception {
        StringTemplate t = StringTemplate.template("base");
        StringTemplate replacement = StringTemplate.name("Alice");
        assertSame(t, t.addStringTemplate("%x%", replacement));
        assertEquals(1, t.entryList().size());
        assertEquals("NAME", t.entryList().get(0).getValue().getTemplateType().name());
        assertEquals("Alice", t.entryList().get(0).getValue().getId());
    }

    public void testTC018() throws Exception {
        StringTemplate t = StringTemplate.label(",");
        StringTemplate replacement = StringTemplate.name("Alice");
        assertSame(t, t.addStringTemplate(replacement));
        assertEquals(1, t.entryList().size());
        assertEquals("NAME", t.entryList().get(0).getValue().getTemplateType().name());
        assertEquals("Alice", t.entryList().get(0).getValue().getId());
    }

    public void testTC019() throws Exception {
        StringTemplate t = StringTemplate.key("base");
        try {
            t.add("%x%", "value.key");
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Cannot add"));
        }
    }

    public void testTC020() throws Exception {
        StringTemplate t = StringTemplate.key("base");
        try {
            t.add("value.key");
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Cannot add"));
        }
    }

    public void testTC021() throws Exception {
        StringTemplate t = StringTemplate.key("base");
        try {
            t.addName("%x%", "Alice");
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Cannot add"));
        }
    }

    public void testTC022() throws Exception {
        StringTemplate t = StringTemplate.key("base");
        try {
            t.addName("%x%", (FreeColObject)StringTemplate.name("object.id"));
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Cannot add"));
        }
    }

    public void testTC023() throws Exception {
        StringTemplate t = StringTemplate.key("base");
        try {
            t.addName("Alice");
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Cannot add"));
        }
    }

    public void testTC024() throws Exception {
        StringTemplate t = StringTemplate.key("base");
        try {
            t.addStringTemplate("%x%", StringTemplate.name("Alice"));
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Cannot add"));
        }
    }

    public void testTC025() throws Exception {
        StringTemplate t = StringTemplate.key("base");
        try {
            t.addStringTemplate(StringTemplate.name("Alice"));
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Cannot add"));
        }
    }

    public void testTC026() throws Exception {
        Named n = () -> "named.key";
        StringTemplate t = StringTemplate.template("base");
        assertSame(t, t.addNamed("%x%", n));
        assertEquals("named.key", t.getReplacement("%x%").getId());
        StringTemplate label = StringTemplate.label(",");
        assertSame(label, label.addNamed(n));
        assertEquals("named.key", label.entryList().get(0).getValue().getId());
    }

    public void testTC027() throws Exception {
        StringTemplate t = StringTemplate.template("base");
        assertSame(t, t.addAmount("%amount%", new BigDecimal("12.50")));
        assertEquals("NAME", t.getReplacement("%amount%").getTemplateType().name());
        assertEquals("12.50", t.getReplacement("%amount%").getId());
    }

    public void testTC028() throws Exception {
        StringTemplate t = StringTemplate.template("base");
        assertSame(t, t.addTagged("%tag%", "tag.key"));
        assertEquals("tag.key", t.getReplacement("%tag%").getId());
    }

    public void testTC029() throws Exception {
        StringTemplate t = StringTemplate.label(",");
        try {
            t.addStringTemplate((StringTemplate)null);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Null key and pair"));
        }
    }

    public void testTC030() throws Exception {
        StringTemplate t = StringTemplate.key("base");
        assertFalse(t.copyIn((FreeColObject)null));
        assertFalse(t.copyIn(new BuildingType("base", null)));
        assertFalse(t.copyIn(StringTemplate.name("other")));
    }

    public void testTC031() throws Exception {
        StringTemplate source = StringTemplate.template("base");
        source.setDefaultId("fallback");
        source.addName("%x%", "A");
        StringTemplate target = StringTemplate.key("base");
        assertTrue(target.copyIn(source));
        assertEquals("TEMPLATE", target.getTemplateType().name());
        assertEquals("fallback", target.getDefaultId());
        assertSame(source.entryList(), target.entryList());
    }

    public void testTC032() throws Exception {
        StringTemplate t = StringTemplate.template("base");
        t.setDefaultId("fallback");
        t.addName("%a%", "Alice");
        t.add("%b%", "value.key");
        String text = xml(t);
        assertTrue(text.contains("defaultId=\"fallback\""));
        assertTrue(text.contains("<pair"));
        StringTemplate value = read(text);
        assertTrue(t.equals(value));
        assertEquals("Alice", value.getReplacement("%a%").getId());
        assertEquals("value.key", value.getReplacement("%b%").getId());
    }

    public void testTC033() throws Exception {
        StringTemplate t = StringTemplate.label(",").addName("A").add("b.key");
        StringTemplate value = read(xml(t));
        assertEquals("LABEL", value.getTemplateType().name());
        assertEquals(2, value.entryList().size());
        assertSame(null, value.entryList().get(0).getKey());
        assertEquals("A", value.entryList().get(0).getValue().getId());
        assertEquals("b.key", value.entryList().get(1).getValue().getId());
    }

    public void testTC034() throws Exception {
        StringTemplate t = StringTemplate.key("title");
        String text = xml(t);
        assertFalse(text.contains("defaultId="));
        assertFalse(text.contains("<pair"));
        StringTemplate value = read(text);
        assertEquals("KEY", value.getTemplateType().name());
        assertEquals("title", value.getId());
        assertTrue(value.isEmpty());
    }

    public void testTC035() throws Exception {
        StringTemplate t = read("<stringTemplate id=\"base\"/>");
        assertEquals("TEMPLATE", t.getTemplateType().name());
        assertSame(null, t.getDefaultId());
        assertTrue(t.isEmpty());
    }

    public void testTC036() throws Exception {
        StringTemplate t = StringTemplate.template("base").addName("%x%", "A");
        try (FreeColXMLReader reader = new FreeColXMLReader(new StringReader("<stringTemplate id=\"base\"/>"))) {
            reader.nextTag();
            t.readFromXML(reader);
        }
        assertTrue(t.isEmpty());
        assertSame(null, t.getReplacement("%x%"));
    }

    public void testTC037() throws Exception {
        String text = "<stringTemplate id=\"base\" templateType=\"template\"><key value=\"%a%\"/><key value=\"%b%\"/><stringTemplate id=\"A\" templateType=\"name\"/><stringTemplate id=\"B\" templateType=\"name\"/></stringTemplate>";
        StringTemplate t = read(text);
        assertEquals(2, t.entryList().size());
        assertEquals("A", t.getReplacement("%a%").getId());
        assertEquals("B", t.getReplacement("%b%").getId());
    }

    public void testTC038() throws Exception {
        String text = "<stringTemplate id=\",\" templateType=\"label\"><stringTemplate id=\"A\" templateType=\"name\"/><stringTemplate id=\"B\" templateType=\"name\"/></stringTemplate>";
        StringTemplate t = read(text);
        assertEquals(2, t.entryList().size());
        assertSame(null, t.entryList().get(0).getKey());
        assertEquals("A", t.entryList().get(0).getValue().getId());
        assertEquals("B", t.entryList().get(1).getValue().getId());
    }

    public void testTC039() throws Exception {
        StringTemplate t = read("<stringTemplate id=\"base\"><key value=\"%x%\"/></stringTemplate>");
        assertEquals(1, t.entryList().size());
        assertSame(null, t.getReplacement("%x%"));
    }

    public void testTC040() throws Exception {
        StringTemplate t = StringTemplate.template("base");
        child(t, "<pair value=\"%x%\"/>");
        assertEquals(1, t.entryList().size());
        assertSame(null, t.getReplacement("%x%"));
    }

    public void testTC041() throws Exception {
        StringTemplate t = StringTemplate.label(",");
        child(t, "<pair><stringTemplate id=\"A\" templateType=\"name\"/></pair>");
        assertSame(null, t.entryList().get(0).getKey());
        assertEquals("A", t.entryList().get(0).getValue().getId());
    }

    public void testTC042() throws Exception {
        StringTemplate t = StringTemplate.template("base");
        try {
            child(t, "<pair/>");
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Null key and pair"));
        }
    }

    public void testTC043() throws Exception {
        StringTemplate t = StringTemplate.template("base");
        try {
            child(t, "<pair value=\"%x%\"><stringTemplate id=\"A\"/><stringTemplate id=\"B\"/></pair>");
            fail("Expected XMLStreamException");
        } catch (XMLStreamException e) {
            assertTrue(e.getMessage().contains("pair expected"));
        }
    }

    public void testTC044() throws Exception {
        StringTemplate t = StringTemplate.template("base");
        try {
            child(t, "<pair value=\"%x%\"><unexpected/></pair>");
            fail("Expected XMLStreamException");
        } catch (XMLStreamException e) {
            assertTrue(e.getMessage().contains("stringTemplate expected"));
        }
    }

    public void testTC045() throws Exception {
        StringTemplate t = StringTemplate.template("base");
        try {
            child(t, "<unexpected/>");
            fail("Expected XMLStreamException");
        } catch (XMLStreamException e) {
            assertNotNull(e.getMessage());
        }
    }

    public void testTC046() throws Exception {
        StringTemplate t = StringTemplate.key("base");
        assertTrue(t.equals(t));
        assertFalse(t.equals("base"));
        assertFalse(t.equals(null));
    }

    public void testTC047() throws Exception {
        assertTrue(StringTemplate.key("base").equals(StringTemplate.key("base")));
        assertFalse(StringTemplate.key("base").equals(StringTemplate.key("other")));
    }

    public void testTC048() throws Exception {
        assertFalse(StringTemplate.key("base").equals(StringTemplate.name("base")));
        StringTemplate a = StringTemplate.key("base");
        StringTemplate b = StringTemplate.key("base");
        a.setDefaultId("fallback");
        assertFalse(a.equals(b));
    }

    public void testTC049() throws Exception {
        StringTemplate a = StringTemplate.template("base");
        StringTemplate b = allocatedEmpty("base");
        assertFalse(a.equals(b));
        assertFalse(b.equals(a));
    }

    public void testTC050() throws Exception {
        StringTemplate a = StringTemplate.template("base").addName("%x%", "A");
        StringTemplate b = StringTemplate.template("base").addName("%x%", "A").addName("%y%", "B");
        assertFalse(a.equals(b));
    }

    public void testTC051() throws Exception {
        StringTemplate a = StringTemplate.template("base").addName("%x%", "A");
        StringTemplate b = StringTemplate.template("base").addName("%y%", "A");
        assertFalse(a.equals(b));
    }

    public void testTC052() throws Exception {
        StringTemplate a = StringTemplate.template("base").addName("%x%", "A");
        StringTemplate b = StringTemplate.template("base").addName("%x%", "B");
        assertFalse(a.equals(b));
    }

    public void testTC053() throws Exception {
        StringTemplate a = StringTemplate.template("base").addName("%x%", "A").addName("%y%", "B");
        StringTemplate b = StringTemplate.template("base").addName("%x%", "A").addName("%y%", "B");
        assertTrue(a.equals(b));
    }

    public void testTC054() throws Exception {
        assertTrue(allocatedEmpty("base").equals(allocatedEmpty("base")));
    }

    public void testTC055() throws Exception {
        StringTemplate a = StringTemplate.key("base");
        assertEquals(-1396205873, a.hashCode());
        a.setDefaultId("fallback");
        assertEquals(-634962542, a.hashCode());
        StringTemplate b = StringTemplate.template("base").addName("%x%", "A");
        assertEquals(-1722736580, b.hashCode());
        assertEquals(-1396205842, allocatedEmpty("base").hashCode());
    }

    public void testTC056() throws Exception {
        assertEquals(40817545, StringTemplate.label(",").addName("A").hashCode());
        StringTemplate partial = read("<stringTemplate id=\"base\"><key value=\"%x%\"/></stringTemplate>");
        assertEquals(-1722799045, partial.hashCode());
    }

    public void testTC057() throws Exception {
        assertEquals("NAME:Alice", StringTemplate.name("Alice").toString());
        StringTemplate key = StringTemplate.key("base");
        assertEquals("KEY:base", key.toString());
        key.setDefaultId("fallback");
        assertEquals("KEY:base(fallback)", key.toString());
    }

    public void testTC058() throws Exception {
        assertEquals("TEMPLATE:base[]", StringTemplate.template("base").toString());
        assertEquals("TEMPLATE:base[]", allocatedEmpty("base").toString());
        StringTemplate t = StringTemplate.template("base").addName("%a%", "A").add("%b%", "b");
        assertEquals("TEMPLATE:base[%a%=NAME:A%b%=KEY:b]", t.toString());
    }

    public void testTC059() throws Exception {
        StringTemplate t = StringTemplate.label(",");
        assertEquals("LABEL:,", t.toString());
        t.addName("A");
        t.add("b");
        assertEquals("LABEL:,[0:NAME:A 1:KEY:b]", t.toString());
        t.entryList().clear();
        assertEquals("LABEL:,]", t.toString());
    }
}
