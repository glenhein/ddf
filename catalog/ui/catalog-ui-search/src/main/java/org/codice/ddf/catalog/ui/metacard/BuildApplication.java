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
package org.codice.ddf.catalog.ui.metacard;

import static spark.Spark.get;

import java.util.concurrent.TimeUnit;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import spark.servlet.SparkApplication;

public class BuildApplication implements SparkApplication {

  EndpointUtil endpointUtil;

  public BuildApplication(EndpointUtil endpointUtil) {
    this.endpointUtil = endpointUtil;
  }

  @Override
  public void init() {
    /** Get the available types that were explicitly configured. */
    get(
        "/builder/availabletypes",
        (request, response) -> {
          Long w = Long.parseLong(request.queryParams("wait"));

          Thread.sleep(TimeUnit.SECONDS.toMillis(w));

          return "{"
              + " \"availabletypes\": ["
              + "    {"
              + "      \"metacardType\": \"metacard.target\""
              + "    },"
              + "    {"
              + "      \"metacardType\": \"metacard.other\""
              + "    }"
              + "  ]"
              + "}";
        });
  }
}
