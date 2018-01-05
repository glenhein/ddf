package org.codice.ddf.spatial.ogc.wfs.v1_1_0.catalog.source;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.gml2.GMLReader;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import nl.pdok.gml3.exceptions.GML3ParseException;
import nl.pdok.gml3.impl.gml3_1_1_2.GML3112ParserImpl;
import org.junit.Test;
import org.xml.sax.SAXException;

public class GmlGeometeryTest {

  @Test
  public void testGml3112Parser() throws GML3ParseException {

    String xml =
        "<MultiLineString xmlns=\"http://www.opengis.net/gml\" srsName=\"urn:x-ogc:def:crs:EPSG:26713\" srsDimension=\"2\">\n"
            + "        <lineStringMember>\n"
            + "                <LineString><posList>598566.26906782 4914058.52150682 598557.69477679 4914085.33977038 598494.67747205 4914254.76475676 598513.91541973 4914348.11723536 598593.95441378 4914440.37147034 598656.43584271 4914500.87603708 598659.42742456 4914521.61846645 598652.10605456 4914532.58195028 598648.43115534 4914544.16261933 598635.63083991 4914558.16462797 598554.58586588 4914636.67913744 598498.3814486 4914752.4469346 598489.81000042 4914778.04541272 598421.49284933 4914873.66244196 598409.19817361 4914931.57794141 598404.90249957 4914948.64642947 598340.17638384 4915068.66645978 598342.49317686 4915118.07263127 598346.0824185 4915117.02463364</posList></LineString>\n"
            + "        </lineStringMember>\n"
            + "</MultiLineString>";

    GML3112ParserImpl p = new GML3112ParserImpl();

    Geometry geo = p.toJTSGeometry(xml);
  }

  @Test
  public void testGmlReader() throws ParserConfigurationException, SAXException, IOException {

    String xml =
        "<MultiLineString xmlns=\"http://www.opengis.net/gml\" srsName=\"urn:x-ogc:def:crs:EPSG:26713\" srsDimension=\"2\">\n"
            + "        <lineStringMember>\n"
            + "                <LineString><posList>598566.26906782 4914058.52150682 598557.69477679 4914085.33977038 598494.67747205 4914254.76475676 598513.91541973 4914348.11723536 598593.95441378 4914440.37147034 598656.43584271 4914500.87603708 598659.42742456 4914521.61846645 598652.10605456 4914532.58195028 598648.43115534 4914544.16261933 598635.63083991 4914558.16462797 598554.58586588 4914636.67913744 598498.3814486 4914752.4469346 598489.81000042 4914778.04541272 598421.49284933 4914873.66244196 598409.19817361 4914931.57794141 598404.90249957 4914948.64642947 598340.17638384 4915068.66645978 598342.49317686 4915118.07263127 598346.0824185 4915117.02463364</posList></LineString>\n"
            + "        </lineStringMember>\n"
            + "</MultiLineString>";

    GMLReader gmlReader = new GMLReader();
    gmlReader.read(xml, null);
  }
}
