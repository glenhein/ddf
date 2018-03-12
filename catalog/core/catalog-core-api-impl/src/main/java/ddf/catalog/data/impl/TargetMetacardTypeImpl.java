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
package ddf.catalog.data.impl;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import java.util.HashSet;
import java.util.Set;

/** DO NOT MERGE. FOR TEMPORARY TESTING ONLY! */
public class TargetMetacardTypeImpl extends MetacardTypeImpl {

  public static final String TARGET_METACARD_TYPE_NAME = "metacard.target";

  private static final Set<AttributeDescriptor> TARGET_DESCRIPTORS;

  static {
    TARGET_DESCRIPTORS = new HashSet<>();

    TARGET_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            Metacard.ID,
            true /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));

    TARGET_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            Metacard.TITLE,
            true /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));

    TARGET_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            Metacard.DESCRIPTION,
            true /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));

    TARGET_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            Core.LOCATION,
            false /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.GEO_TYPE));

    TARGET_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            "target.type",
            false /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));

    TARGET_DESCRIPTORS.add(
        new AttributeDescriptorImpl(
            "security.classification",
            false /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));
  }

  public TargetMetacardTypeImpl() {
    this(TARGET_METACARD_TYPE_NAME, TARGET_DESCRIPTORS);
  }

  public TargetMetacardTypeImpl(String name, Set<AttributeDescriptor> descriptors) {
    super(name, descriptors);
  }
}
