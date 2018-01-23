/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.spatial.ogc.wfs.catalog.mapper.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

public class TestMetacardMapperImpl {

  private static final String EXAMPLE_FEATURE_TYPE = "{http://www.example.com}featureType1";

  private static final String NON_EXISTENT_FEATURE = "feature.prop.nonexistent";

  private static final String FEATURE_NAME = "feature.name";

  private static final String FEATURE_MODIFIED = "feature.modified";

  private static final String FEATURE_CALLSIGN = "feature.callSign";

  private static final String FEATURE_MISSIONID = "feature.missionId";

  private static final String METACARD_TITLE = "title";

  private static final String METACARD_MODIFIED = "modified";

  private static final String METACARD_CONTENTSTORE = "ext.content-store-name";

  private static final String METACARD_KEYWORD_1 = "topic.keyword1";

  private static final String METACARD_KEYWORD_2 = "topic.keyword2";

  private static final String FEATURE_CONSTANT_VALUE = "ConstantFoo";

  private String[][] MAPPINGS = {
    {METACARD_TITLE, FEATURE_NAME, "{{" + FEATURE_NAME + "}}"},
    {
      METACARD_MODIFIED,
      FEATURE_MODIFIED,
      "{{dateFormat " + FEATURE_MODIFIED + " format=\"yyyy-MM-dd\"}}"
    },
    {METACARD_CONTENTSTORE, "", FEATURE_CONSTANT_VALUE},
    {METACARD_KEYWORD_1, FEATURE_CALLSIGN, "CallSign = {{" + FEATURE_CALLSIGN + "}}"},
    {METACARD_KEYWORD_2, FEATURE_MISSIONID, "MissionID = {{" + FEATURE_MISSIONID + "}}"}
  };

  private MetacardMapperImpl metacardMapper;

  @Before
  public void setup() {
    this.metacardMapper = new MetacardMapperImpl();
    metacardMapper.setFeatureType(EXAMPLE_FEATURE_TYPE);

    for (String[] mapping : MAPPINGS) {
      metacardMapper.addAttributeMapping(mapping[0], mapping[1], mapping[2]);
    }
  }

  @Test
  public void testGetFeatureProperty() {
    assertThat(
        metacardMapper
            .getEntry(e -> e.getAttributeName().equals(METACARD_TITLE))
            .get()
            .getFeatureProperty(),
        is(FEATURE_NAME));
    assertThat(
        metacardMapper
            .getEntry(e -> e.getAttributeName().equals(METACARD_MODIFIED))
            .get()
            .getFeatureProperty(),
        is(FEATURE_MODIFIED));
    assertThat(
        metacardMapper
            .getEntry(e -> e.getAttributeName().equals(METACARD_KEYWORD_1))
            .get()
            .getFeatureProperty(),
        is(FEATURE_CALLSIGN));
    assertThat(
        metacardMapper
            .getEntry(e -> e.getAttributeName().equals(METACARD_KEYWORD_2))
            .get()
            .getFeatureProperty(),
        is(FEATURE_MISSIONID));
    assertThat(
        metacardMapper
            .getEntry(e -> e.getAttributeName().equals(METACARD_CONTENTSTORE))
            .get()
            .getFeatureProperty(),
        is(""));
  }

  @Test
  public void testGetMetacardAttribute() {
    assertThat(
        metacardMapper
            .getEntry(e -> e.getFeatureProperty().equals(FEATURE_NAME))
            .get()
            .getAttributeName(),
        is(METACARD_TITLE));
    assertThat(
        metacardMapper
            .getEntry(e -> e.getFeatureProperty().equals(FEATURE_MODIFIED))
            .get()
            .getAttributeName(),
        is(METACARD_MODIFIED));
    assertThat(
        metacardMapper
            .getEntry(e -> e.getFeatureProperty().equals(FEATURE_CALLSIGN))
            .get()
            .getAttributeName(),
        is(METACARD_KEYWORD_1));
    assertThat(
        metacardMapper
            .getEntry(e -> e.getFeatureProperty().equals(FEATURE_MISSIONID))
            .get()
            .getAttributeName(),
        is(METACARD_KEYWORD_2));
  }

  @Test
  public void testGetEntry() {
    Optional o = metacardMapper.getEntry(e -> e.getFeatureProperty().equals(FEATURE_NAME));
    assertThat(o.isPresent(), is(true));

    o = metacardMapper.getEntry(e -> e.getFeatureProperty().equals(NON_EXISTENT_FEATURE));
    assertThat(o.isPresent(), is(false));
  }

  @Test
  public void testTemplate() {}

  @Test
  public void testSetAttributeMappingsList() {
    metacardMapper.setAttributeMappings(
        Collections.singletonList(
            "{\"attributeName\": \"topic.keyword\", \"featureName\": \"MissionId\", \"template\": \"{{myFeature.missionid}}\"}"));
    List<MetacardMapper.Entry> mappingList = metacardMapper.getMappingEntryList();
    assertThat(mappingList, hasSize(1));
    assertThat(mappingList.get(0), is(instanceOf(FeatureAttributeEntry.class)));
    FeatureAttributeEntry featureAttributeEntry = (FeatureAttributeEntry) mappingList.get(0);
    assertThat(featureAttributeEntry.getAttributeName(), is("topic.keyword"));
    assertThat(featureAttributeEntry.getFeatureProperty(), is("MissionId"));
    assertThat(featureAttributeEntry.getTemplateText(), is("{{myFeature.missionid}}"));
  }

  @Test
  public void testSetAttributeMappingsListWithBadJson() {
    metacardMapper.setAttributeMappings(
        Arrays.asList(
            "{\"attributeName\": \"topic.keyword\", \"featureName\": \"MissionId\", \"template\": \"{{myFeature.missionid}}\"}",
            "{\"attributeName\": \"topic.other\", \"featureName\": \"Other\", \"template\" \"{{myFeature.other}}\""));
    List<MetacardMapper.Entry> mappingList = metacardMapper.getMappingEntryList();
    assertThat(mappingList, hasSize(1));
    assertThat(mappingList.get(0), is(instanceOf(FeatureAttributeEntry.class)));
    FeatureAttributeEntry featureAttributeEntry = (FeatureAttributeEntry) mappingList.get(0);
    assertThat(featureAttributeEntry.getAttributeName(), is("topic.keyword"));
    assertThat(featureAttributeEntry.getFeatureProperty(), is("MissionId"));
    assertThat(featureAttributeEntry.getTemplateText(), is("{{myFeature.missionid}}"));
  }
}
