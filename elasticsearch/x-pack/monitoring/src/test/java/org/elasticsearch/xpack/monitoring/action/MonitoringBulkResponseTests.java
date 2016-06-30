/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.monitoring.action;

import org.elasticsearch.Version;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.monitoring.agent.exporter.ExportException;

import java.io.IOException;

import static org.elasticsearch.test.VersionUtils.randomVersion;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class MonitoringBulkResponseTests extends ESTestCase {

    public void testResponseStatus() {
        final long took = Math.abs(randomLong());
        MonitoringBulkResponse response = new MonitoringBulkResponse(took);

        assertThat(response.getTookInMillis(), equalTo(took));
        assertThat(response.getError(), is(nullValue()));
        assertThat(response.status(), equalTo(RestStatus.OK));

        ExportException exception = new ExportException(randomAsciiOfLength(10));
        response = new MonitoringBulkResponse(took, new MonitoringBulkResponse.Error(exception));

        assertThat(response.getTookInMillis(), equalTo(took));
        assertThat(response.getError(), is(notNullValue()));
        assertThat(response.status(), equalTo(RestStatus.INTERNAL_SERVER_ERROR));
    }

    public void testSerialization() throws IOException {
        int iterations = randomIntBetween(5, 50);
        for (int i = 0; i < iterations; i++) {
            MonitoringBulkResponse response;
            if (randomBoolean()) {
                response = new MonitoringBulkResponse(Math.abs(randomLong()));
            } else {
                Exception exception = randomFrom(
                        new ExportException(randomAsciiOfLength(5), new IllegalStateException(randomAsciiOfLength(5))),
                        new IllegalStateException(randomAsciiOfLength(5)),
                        new IllegalArgumentException(randomAsciiOfLength(5)));
                response = new MonitoringBulkResponse(Math.abs(randomLong()), new MonitoringBulkResponse.Error(exception));
            }

            BytesStreamOutput output = new BytesStreamOutput();
            Version outputVersion = randomVersion(random());
            output.setVersion(outputVersion);
            response.writeTo(output);

            StreamInput streamInput = output.bytes().streamInput();
            streamInput.setVersion(randomVersion(random()));
            MonitoringBulkResponse response2 = new MonitoringBulkResponse();
            response2.readFrom(streamInput);

            assertThat(response2.getTookInMillis(), equalTo(response.getTookInMillis()));
            if (response.getError() == null) {
                assertThat(response2.getError(), is(nullValue()));
            } else {
                assertThat(response2.getError(), is(notNullValue()));
            }
        }
    }
}
