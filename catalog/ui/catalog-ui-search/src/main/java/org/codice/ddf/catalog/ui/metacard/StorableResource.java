package org.codice.ddf.catalog.ui.metacard;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.platform.util.TemporaryFileBackedOutputStream;

/** Call {@link #close()} if {@link #isError()} returns {@code false}. */
public class StorableResource implements AutoCloseable {

  private TemporaryFileBackedOutputStream temporaryFileBackedOutputStream;
  private String mimeType;
  private String filename;
  private String errorMessage;
  private List<InputStream> openInputStreams = new LinkedList<>();

  public StorableResource(InputStream inputStream, @Nullable String mimeType, String filename)
      throws IOException {
    temporaryFileBackedOutputStream = new TemporaryFileBackedOutputStream();
    IOUtils.copy(inputStream, temporaryFileBackedOutputStream);
    this.mimeType = mimeType;
    this.filename = filename;
  }

  public StorableResource(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public StorableResource(InputStream inputStream, String filename) throws IOException {
    this(inputStream, null, filename);
  }

  /**
   * The caller is not responsible for calling {@link InputStream#close()} on the returned stream.
   * However, the caller is responsible for calling {@link #close()}.
   *
   * @return
   * @throws IOException
   */
  public InputStream getInputStream() throws IOException {
    InputStream inputStream = temporaryFileBackedOutputStream.asByteSource().openStream();
    openInputStreams.add(inputStream);
    return inputStream;
  }

  public Optional<String> getMimeType() {
    return Optional.ofNullable(mimeType);
  }

  public String getFilename() {
    return filename;
  }

  public boolean isError() {
    return errorMessage != null;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  @Override
  public void close() {
    if (temporaryFileBackedOutputStream != null) {
      IOUtils.closeQuietly(temporaryFileBackedOutputStream);
      temporaryFileBackedOutputStream = null;
    }
    openInputStreams.forEach(IOUtils::closeQuietly);
    openInputStreams.clear();
  }
}
