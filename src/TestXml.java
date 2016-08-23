import xml.XmlNode;
import xml.XmlParser;

import java.io.FileInputStream;
import java.io.IOException;

public class TestXml {
    public static void main(String[] args) throws IOException {
        XmlNode node = new XmlParser(new FileInputStream("plant_catalog.xml")).parseFile().getRoot();
        System.out.println(node .toString());
    }
}