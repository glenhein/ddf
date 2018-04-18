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

import static spark.Spark.post;

import ddf.catalog.CatalogFramework;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.impl.CreateStorageRequestImpl;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolutionException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Response;
import spark.servlet.SparkApplication;

public class ListApplication implements SparkApplication {

  private static final Logger LOGGER = LoggerFactory.getLogger(ListApplication.class);

  private static final String LIST_TYPE_HEADER = "List-Type";

  private static final String DEFAULT_FILE_EXTENSION = "bin";

  private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

  private static final String DEFAULT_FILE_NAME = "file";

  private static final List<String> REFINEABLE_MIME_TYPES =
      Arrays.asList(DEFAULT_MIME_TYPE, "text/plain");

  private final MimeTypeMapper mimeTypeMapper;

  private final CatalogFramework catalogFramework;

  private final UuidGenerator uuidGenerator;

  public ListApplication(
      MimeTypeMapper mimeTypeMapper,
      CatalogFramework catalogFramework,
      UuidGenerator uuidGenerator) {
    this.mimeTypeMapper = mimeTypeMapper;
    this.catalogFramework = catalogFramework;
    this.uuidGenerator = uuidGenerator;
  }

  @Override
  public void init() {
    post(
        "/list/import",
        (request, response) -> {
          MultipartConfigElement multipartConfigElement = new MultipartConfigElement("");
          request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);

          String listType = request.headers(LIST_TYPE_HEADER);

          if (StringUtils.isBlank(listType)) {
            String exceptionMessage = String.format("The header %s must be set.", LIST_TYPE_HEADER);
            LOGGER.info(exceptionMessage);
            createBadRequestResponse(exceptionMessage, response);
            return null;
          }

          List<Part> parts = new ArrayList<>(request.raw().getParts());

          CreateInfo createInfo = parseAttachments(parts);

          if (createInfo == null) {
            String exceptionMessage = "Unable to parse the attachments.";
            LOGGER.info(exceptionMessage);
            createBadRequestResponse(exceptionMessage, response);
            return null;
          }

          Splitter splitter = lookupSplitter(createInfo.contentType);

          List<String> ids = new LinkedList<>();
          List<String> errorMessages = new LinkedList<>();

          try (Stream<StorableResource> stream = createStream(createInfo, splitter)) {
            stream
                .peek(storableResource -> appendMessageIfError(errorMessages, storableResource))
                .filter(storableResource -> !storableResource.isError())
                .forEach(
                    storableResource ->
                        storeAndClose(ids::add, errorMessages::add, storableResource));
          }

          // TODO add the error message to the notification icon!

          response.header("Added-IDs", ids.stream().collect(Collectors.joining(",")));

          IOUtils.closeQuietly(createInfo.getStream());

          return "";
        });
  }

  private void storeAndClose(
      Consumer<String> idConsumer,
      Consumer<String> errorMessageConsumer,
      StorableResource storableResource) {

    try /*(StorableResource sr = storableResource)*/ {
      store(getCreateInfo(storableResource), idConsumer, errorMessageConsumer);
    } catch (IOException e) {
      LOGGER.debug("Unable to create CreateInfo: ", e);
    }
  }

  private Stream<StorableResource> createStream(CreateInfo createInfo, Splitter splitter)
      throws IOException {
    return splitter.split(createStorableResource(createInfo));
  }

  private StorableResource createStorableResource(CreateInfo createInfo) throws IOException {
    return new StorableResource(createInfo.stream, createInfo.contentType, createInfo.filename);
  }

  private void appendMessageIfError(List<String> errorMessages, StorableResource storableResource) {
    if (storableResource.isError()) {
      errorMessages.add(storableResource.getErrorMessage());
    }
  }

  private CreateInfo getCreateInfo(StorableResource storableResource) throws IOException {
    CreateInfo createInfoItem = new CreateInfo();

    createInfoItem.setFilename(storableResource.getFilename());
    createInfoItem.setStream(storableResource.getInputStream());
    createInfoItem.setContentType(
        storableResource
            .getMimeType()
            .orElse(contentTypeFromFilename(storableResource.getFilename())));

    return createInfoItem;
  }

  private CreateInfo parseAttachments(List<Part> contentParts) {

    if (contentParts.size() == 1) {
      Part contentPart = contentParts.get(0);
      return parseAttachment(contentPart);
    }
    return null;
  }

  private CreateInfo parseAttachment(Part contentPart) {
    CreateInfo createInfo = new CreateInfo();
    InputStream stream;
    String filename;
    String contentType = null;

    try {
      stream = contentPart.getInputStream();
      if (stream != null && stream.available() == 0) {
        stream.reset();
      }
      createInfo.setStream(stream);
    } catch (IOException e) {
      LOGGER.info("IOException reading stream from file attachment in multipart body", e);
    }

    if (contentPart.getContentType() != null) {
      contentType = contentPart.getContentType();
    }

    filename = contentPart.getSubmittedFileName();

    if (StringUtils.isEmpty(filename)) {
      LOGGER.debug("No filename parameter provided - generating default filename");
      String fileExtension = DEFAULT_FILE_EXTENSION;
      try {
        fileExtension = mimeTypeMapper.getFileExtensionForMimeType(contentType); // DDF-2307
        if (StringUtils.isEmpty(fileExtension)) {
          fileExtension = DEFAULT_FILE_EXTENSION;
        }
      } catch (MimeTypeResolutionException e) {
        LOGGER.debug("Exception getting file extension for contentType = {}", contentType);
      }
      filename = DEFAULT_FILE_NAME + "." + fileExtension; // DDF-2263
      LOGGER.debug("No filename parameter provided - default to {}", filename);
    } else {
      filename = FilenameUtils.getName(filename);

      if (StringUtils.isEmpty(contentType) || REFINEABLE_MIME_TYPES.contains(contentType)) {
        contentType = contentTypeFromFilename(filename);
      }
    }

    createInfo.setContentType(contentType);
    createInfo.setFilename(filename);

    return createInfo;
  }

  private String contentTypeFromFilename(String filename) {
    String fileExtension = FilenameUtils.getExtension(filename);
    String contentType = null;
    try {
      contentType = mimeTypeMapper.getMimeTypeForFileExtension(fileExtension);
    } catch (MimeTypeResolutionException e) {
      LOGGER.debug("Unable to get contentType based on filename extension {}", fileExtension);
    }
    LOGGER.debug("Refined contentType = {}", contentType);
    return contentType;
  }

  private Splitter lookupSplitter(String mimeType) {
    // TODO search OSGI for a Splitter!

    // TODO if an OSGI splitter is not found, then return a default splitter

    // return Stream::of;

    return new ZipSplitter();
  }

  private void store(
      CreateInfo createInfo, Consumer<String> idConsumer, Consumer<String> errorMessageConsumer) {

    CreateStorageRequest streamCreateRequest =
        new CreateStorageRequestImpl(
            Collections.singletonList(
                new IncomingContentItem(
                    uuidGenerator,
                    createInfo.getStream(),
                    createInfo.getContentType(),
                    createInfo.getFilename(),
                    createInfo.getMetacard())),
            null);
    try {
      CreateResponse createResponse = catalogFramework.create(streamCreateRequest);

      createResponse.getCreatedMetacards().stream().map(Metacard::getId).forEach(idConsumer);

    } catch (IngestException e) {
      String errorMessage = "Error while storing entry in catalog: ";
      LOGGER.info(errorMessage, e);
      errorMessageConsumer.accept(errorMessage);
    } catch (SourceUnavailableException e) {
      String exceptionMessage = "Cannot create catalog entry because source is unavailable: ";
      LOGGER.info(exceptionMessage, e);
      throw new InternalServerErrorException(exceptionMessage);
    } catch (Error e) {
      LOGGER.info("", e);
    }
  }

  private void createBadRequestResponse(String entityMessage, Response response) {
    response.status(Status.BAD_REQUEST.getStatusCode());
    response.body("<pre>" + entityMessage + "</pre>");
    response.type(MediaType.TEXT_HTML);
  }

  protected static class IncomingContentItem extends ContentItemImpl {

    private InputStream inputStream;

    private IncomingContentItem(
        UuidGenerator uuidGenerator,
        InputStream inputStream,
        String mimeTypeRawData,
        String filename,
        Metacard metacard) {
      super(uuidGenerator.generateUuid(), null, mimeTypeRawData, filename, 0L, metacard);
      this.inputStream = inputStream;
    }

    @Override
    public InputStream getInputStream() {
      return inputStream;
    }
  }

  protected static class CreateInfo {
    InputStream stream = null;

    String filename = null;

    String contentType = null;

    Metacard metacard = null;

    public InputStream getStream() {
      return stream;
    }

    public void setStream(InputStream stream) {
      this.stream = stream;
    }

    String getFilename() {
      return filename;
    }

    void setFilename(String filename) {
      this.filename = filename;
    }

    String getContentType() {
      return contentType;
    }

    void setContentType(String contentType) {
      this.contentType = contentType;
    }

    public Metacard getMetacard() {
      return metacard;
    }

    public void setMetacard(Metacard metacard) {
      this.metacard = metacard;
    }
  }
}
