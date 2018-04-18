package org.codice.ddf.catalog.ui.metacard;

import java.io.IOException;
import java.util.stream.Stream;

public interface Splitter {

  /**
   * Callers must call {@link Stream#close()} on the stream and must call {@link
   * StorableResource#close()} on each item in the stream.
   *
   * @return
   * @throws IOException
   */
  Stream<StorableResource> split(StorableResource storableResource) throws IOException;
}
