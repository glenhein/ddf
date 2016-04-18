/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.camel.component.catalog.content;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.impl.DefaultProducer;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.impl.CreateStorageRequestImpl;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolutionException;

public class ContentProducer extends DefaultProducer {
    public static final int KB = 1024;

    public static final int MB = 1024 * KB;

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ContentProducer.class);

    private static final int DEFAULT_FILE_BACKED_OUTPUT_STREAM_THRESHOLD = 1 * MB;

    private ContentEndpoint endpoint;

    private static final String FILE_ABSOLUTE = "CamelFileAbsolute";

    private static final String FILE_ABSOLUTE_PATH = "CamelFileAbsolutePath";

    private static final String FILE_RELATIVE_PATH = "CamelFileRelativePath";

    private static final List<String> CAMEL_DEFAULT_HEADERS = Arrays.asList(Exchange.BREADCRUMB_ID,
            FILE_ABSOLUTE,
            FILE_ABSOLUTE_PATH,
            Exchange.FILE_CONTENT_TYPE,
            Exchange.FILE_LAST_MODIFIED,
            Exchange.FILE_LENGTH,
            Exchange.FILE_NAME,
            Exchange.FILE_NAME_CONSUMED,
            Exchange.FILE_NAME_ONLY,
            Exchange.FILE_PARENT,
            Exchange.FILE_PATH,
            FILE_RELATIVE_PATH);

    /**
     * Constructs the {@link org.apache.camel.Producer} for the custom Camel ContentComponent. This producer would
     * map to a Camel <code>&lt;to&gt;</code> route node with a URI like <code>content:framework</code>
     *
     * @param endpoint the Camel endpoint that created this consumer
     */
    public ContentProducer(ContentEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;

        LOGGER.debug("INSIDE: ContentProducer constructor");
    }

    @Override
    public void process(Exchange exchange)
            throws ContentComponentException, SourceUnavailableException, IngestException {
        LOGGER.debug("ENTERING: process");

        if (!exchange.getPattern()
                .equals(ExchangePattern.InOnly)) {
            return;
        }

        Message in = exchange.getIn();
        Object body = in.getBody();
        File ingestedFile;

        Map<String, Object> headerMap = exchange.getIn()
                .getHeaders();
        LOGGER.debug("Headers : {} ", headerMap);

        if (body instanceof GenericFile) {
            GenericFile<File> genericFile = (GenericFile<File>) body;
            ingestedFile = genericFile.getFile();
        } else {
            LOGGER.warn(
                    "Unable to cast message body to Camel GenericFile, so unable to process ingested file");
            throw new ContentComponentException(
                    "Unable to cast message body to Camel GenericFile, so unable to process ingested file");
        }

        if (ingestedFile == null) {
            LOGGER.debug("EXITING: process - ingestedFile is NULL");
            return;
        }

        String fileExtension = FilenameUtils.getExtension(ingestedFile.getAbsolutePath());

        String mimeType;
        MimeTypeMapper mimeTypeMapper = endpoint.getComponent()
                .getMimeTypeMapper();
        if (mimeTypeMapper != null) {
            try (InputStream inputStream = Files.asByteSource(ingestedFile)
                    .openStream()) {
                if (fileExtension.equals("xml")) {
                    mimeType = mimeTypeMapper.guessMimeType(inputStream, fileExtension);
                } else {
                    mimeType = mimeTypeMapper.getMimeTypeForFileExtension(fileExtension);
                }

            } catch (MimeTypeResolutionException | IOException e) {
                throw new ContentComponentException(e);
            }
        } else {
            LOGGER.error("Did not find a MimeTypeMapper service");
            throw new ContentComponentException(
                    "Unable to find a mime type for the ingested file " + ingestedFile.getName());
        }

        LOGGER.debug("Preparing content item for mimeType = {}", mimeType);

        if (StringUtils.isNotEmpty(mimeType)) {
            ContentItem newItem;
            newItem = new ContentItemImpl(Files.asByteSource(ingestedFile),
                    mimeType,
                    ingestedFile.getName(),
                    null);

            LOGGER.debug("Creating content item.");

            Map<String, Serializable> propertiesMap = new HashMap<>();

            headerMap.forEach((key, value) -> {
                if (!CAMEL_DEFAULT_HEADERS.contains(key)) {
                    propertiesMap.put(key, (String) value);
                }
            });

            CreateStorageRequest createRequest =
                    new CreateStorageRequestImpl(Collections.singletonList(newItem), propertiesMap);
            CreateResponse createResponse = endpoint.getComponent()
                    .getCatalogFramework()
                    .create(createRequest);
            if (createResponse != null) {
                List<Metacard> createdMetacards = createResponse.getCreatedMetacards();

                if (LOGGER.isDebugEnabled()) {
                    for (Metacard metacard : createdMetacards) {
                        LOGGER.debug("content item created with id = {}", metacard.getId());
                    }
                }
            }
        } else {
            LOGGER.debug("mimeType is NULL");
            throw new ContentComponentException(
                    "Unable to determine mime type for the file " + ingestedFile.getName());
        }

        LOGGER.debug("EXITING: process");
    }

}
