package org.codice.ddf.catalog.ui.metacard;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZipSplitter implements Splitter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ZipSplitter.class);

  @Override
  public Stream<StorableResource> split(StorableResource storableResource) throws IOException {
    ZipIterator zipIterator = new ZipIterator(storableResource.getInputStream());
    return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(zipIterator, Spliterator.NONNULL), true)
        .onClose(zipIterator::close);
  }

  private static class ZipIterator implements Iterator<StorableResource>, AutoCloseable {

    private ZipInputStream zipInputStream;
    private ZipEntry zipEntry;

    ZipIterator(InputStream inputStream) throws IOException {
      zipInputStream = new ZipInputStream(inputStream);
      zipEntry = zipInputStream.getNextEntry();
    }

    @Override
    public boolean hasNext() {
      return zipEntry != null;
    }

    @Override
    public StorableResource next() {
      try {
        //        ByteArrayInputStream byteArrayInputStream =
        //            new ByteArrayInputStream(IOUtils.readFully(zipInputStream, (int)
        // zipEntry.getSize()));
        String filename = zipEntry.getName();
        StorableResource storableResource = new StorableResource(zipInputStream, filename);
        zipEntry = zipInputStream.getNextEntry();
        return storableResource;
      } catch (IOException e) {
        String message = "Failed to get the next item in the ZIP file.";
        LOGGER.debug(message, e);
        return new StorableResource(message);
      }
    }

    @Override
    public void close() {
      if (zipInputStream != null) {
        IOUtils.closeQuietly(zipInputStream);
        zipInputStream = null;
        zipEntry = null;
      }
    }
  }
}
