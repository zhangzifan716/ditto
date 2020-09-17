/*
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.ditto.protocoladapter;

import java.util.Objects;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.eclipse.ditto.json.JsonArray;
import org.eclipse.ditto.json.JsonCollectors;
import org.eclipse.ditto.json.JsonParseException;
import org.eclipse.ditto.json.JsonValue;
import org.eclipse.ditto.model.base.acks.DittoAcknowledgementLabel;
import org.eclipse.ditto.model.base.headers.DittoHeaderDefinition;

/**
 * This {@link HeaderEntryFilter} checks if the given key references {@link DittoHeaderDefinition#REQUESTED_ACKS} and
 * removes {@link DittoAcknowledgementLabel Ditto-internal acknowledgement requests} from the returned value.
 */
@Immutable
final class DittoAckRequestsFilter extends AbstractHeaderEntryFilter {

    private static final JsonValue EMPTY_JSON_STRING = JsonValue.of("");

    private static final DittoAckRequestsFilter INSTANCE = new DittoAckRequestsFilter();

    private DittoAckRequestsFilter() {
        super();
    }

    /**
     * Returns an instance of {@code DittoAckRequestsFilter} which adjusts the value of the header entry identified by
     * {@link DittoHeaderDefinition#REQUESTED_ACKS} by removing
     * {@link DittoAcknowledgementLabel Ditto-internal acknowledgement requests} from it.
     *
     * @return the instance.
     */
    public static DittoAckRequestsFilter getInstance() {
        return INSTANCE;
    }

    @Override
    @Nullable
    public String filterValue(final String key, final String value) {
        @Nullable final String result;
        if (isRequestedAcks(key)) {
            if (value.isEmpty()) {
                result = null;
            } else {
                result = tryToParseAsJsonArrayAndFilter(value);
            }
        } else {
           result = value;
        }
        return result;
    }

    private static boolean isRequestedAcks(final String key) {
        return Objects.equals(DittoHeaderDefinition.REQUESTED_ACKS.getKey(), key);
    }

    @Nullable
    private static String tryToParseAsJsonArrayAndFilter(final String value) {
        try {
            return parseAsJsonArrayAndFilter(value);
        } catch (final JsonParseException e) {
            return null;
        }
    }

    @Nullable
    private static String parseAsJsonArrayAndFilter(final String value) {
        final JsonArray originalAckRequestsJsonArray = JsonArray.of(value);
        final JsonArray filteredAckRequestsJsonArray = originalAckRequestsJsonArray.stream()
                .filter(JsonValue::isString)
                .filter(jsonValue -> !jsonValue.equals(EMPTY_JSON_STRING))
                .filter(jsonValue -> !isDittoInternal(jsonValue))
                .collect(JsonCollectors.valuesToArray());

        final boolean allElementsFiltered =
                filteredAckRequestsJsonArray.isEmpty() && !originalAckRequestsJsonArray.isEmpty();
        return allElementsFiltered ? null : filteredAckRequestsJsonArray.toString();
    }

    private static boolean isDittoInternal(final JsonValue ackRequestLabelJsonValue) {
        return DittoAcknowledgementLabel.TWIN_PERSISTED.toString().equals(ackRequestLabelJsonValue.asString());
    }

}