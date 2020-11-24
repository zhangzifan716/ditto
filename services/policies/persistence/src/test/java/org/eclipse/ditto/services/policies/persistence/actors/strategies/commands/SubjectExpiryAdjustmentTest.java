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
package org.eclipse.ditto.services.policies.persistence.actors.strategies.commands;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.eclipse.ditto.model.base.headers.DittoHeaders;
import org.eclipse.ditto.model.policies.PolicyId;
import org.eclipse.ditto.model.policies.Subject;
import org.eclipse.ditto.model.policies.SubjectExpiry;
import org.eclipse.ditto.model.policies.SubjectId;
import org.eclipse.ditto.model.policies.SubjectIssuer;
import org.eclipse.ditto.services.policies.common.config.DefaultPolicyConfig;
import org.eclipse.ditto.services.policies.persistence.TestConstants;
import org.eclipse.ditto.services.utils.persistentactors.commands.CommandStrategy;
import org.eclipse.ditto.signals.commands.policies.modify.ModifySubject;
import org.eclipse.ditto.signals.commands.policies.modify.ModifySubjectResponse;
import org.eclipse.ditto.signals.events.policies.SubjectCreated;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

/**
 * Unit test for testing the adjustment (rounding up) of a Policy Subject
 * {@link org.eclipse.ditto.model.policies.SubjectExpiry}.
 */
public final class SubjectExpiryAdjustmentTest extends AbstractPolicyCommandStrategyTest {

    @Test
    public void roundingUpToOneSecond() {
        final ModifySubjectStrategy underTest = createStrategy("1s");

        final LocalDateTime givenExpiry = LocalDateTime.now()
                .plusHours(1)
                .plusMinutes(7)
                .plusSeconds(52);
        final LocalDateTime expectedAdjustedExpiry = givenExpiry
                .truncatedTo(ChronoUnit.SECONDS)
                .plusSeconds(1);

        doTestSubjectExpiryAdjustment(underTest, givenExpiry, expectedAdjustedExpiry);
    }

    @Test
    public void roundingUpToTenSeconds() {
        final ModifySubjectStrategy underTest = createStrategy("10s");

        final LocalDateTime givenExpiry = LocalDateTime.now()
                .truncatedTo(ChronoUnit.HOURS)
                .plusHours(1)
                .plusMinutes(7)
                .plusSeconds(44);
        final LocalDateTime expectedAdjustedExpiry = givenExpiry
                .truncatedTo(ChronoUnit.MINUTES)
                .plusSeconds(50);

        doTestSubjectExpiryAdjustment(underTest, givenExpiry, expectedAdjustedExpiry);
    }

    @Test
    public void roundingUpToOneMinute() {
        final ModifySubjectStrategy underTest = createStrategy("1m");

        final LocalDateTime givenExpiry = LocalDateTime.now()
                .plusHours(1)
                .plusMinutes(7)
                .plusSeconds(3);
        final LocalDateTime expectedAdjustedExpiry = givenExpiry
                .truncatedTo(ChronoUnit.MINUTES)
                .plusMinutes(1);

        doTestSubjectExpiryAdjustment(underTest, givenExpiry, expectedAdjustedExpiry);
    }

    @Test
    public void roundingUpToFifteenMinutes() {
        final ModifySubjectStrategy underTest = createStrategy("15m");

        final LocalDateTime givenExpiry = LocalDateTime.now()
                .truncatedTo(ChronoUnit.HOURS)
                .plusHours(1)
                .plusMinutes(7)
                .plusSeconds(3);
        final LocalDateTime expectedAdjustedExpiry = givenExpiry
                .truncatedTo(ChronoUnit.HOURS)
                .plusMinutes(15);

        doTestSubjectExpiryAdjustment(underTest, givenExpiry, expectedAdjustedExpiry);
    }

    @Test
    public void roundingUpToOneHour() {
        final ModifySubjectStrategy underTest = createStrategy("1h");

        final LocalDateTime givenExpiry = LocalDateTime.now()
                .plusHours(1)
                .plusMinutes(7)
                .plusSeconds(3);
        final LocalDateTime expectedAdjustedExpiry = givenExpiry
                .truncatedTo(ChronoUnit.HOURS)
                .plusHours(1);

        doTestSubjectExpiryAdjustment(underTest, givenExpiry, expectedAdjustedExpiry);
    }

    @Test
    public void roundingUpToTwelveHours() {
        final ModifySubjectStrategy underTest = createStrategy("12h");

        final LocalDateTime givenExpiry = LocalDateTime.now()
                .plusDays(1)
                .withHour(9);
        final LocalDateTime expectedAdjustedExpiry = givenExpiry
                .truncatedTo(ChronoUnit.DAYS)
                .plusHours(12);

        doTestSubjectExpiryAdjustment(underTest, givenExpiry, expectedAdjustedExpiry);
    }

    @Test
    public void roundingUpToOneDay() {
        final ModifySubjectStrategy underTest = createStrategy("1d");

        final LocalDateTime givenExpiry = LocalDateTime.now()
                .plusHours(1)
                .plusMinutes(7)
                .plusSeconds(3);
        final LocalDateTime expectedAdjustedExpiry = givenExpiry
                .truncatedTo(ChronoUnit.DAYS)
                .plusDays(1);

        doTestSubjectExpiryAdjustment(underTest, givenExpiry, expectedAdjustedExpiry);
    }

    private static ModifySubjectStrategy createStrategy(final String configuredGranularityString) {
        return new ModifySubjectStrategy(DefaultPolicyConfig.of(ConfigFactory.load("policy-test")
                .withValue("policy.subject-expiry-granularity",
                        ConfigValueFactory.fromAnyRef(configuredGranularityString))));
    }

    private static void doTestSubjectExpiryAdjustment(final ModifySubjectStrategy underTest,
            final LocalDateTime expiry,
            final LocalDateTime expectedExpiry) {

        final Subject subject = Subject.newInstance(SubjectId.newInstance(SubjectIssuer.INTEGRATION, "this-is-me"),
                TestConstants.Policy.SUBJECT_TYPE, SubjectExpiry.newInstance(expiry.toInstant(ZoneOffset.UTC)));
        final Subject expectedSubject =
                Subject.newInstance(SubjectId.newInstance(SubjectIssuer.INTEGRATION, "this-is-me"),
                        TestConstants.Policy.SUBJECT_TYPE,
                        SubjectExpiry.newInstance(expectedExpiry.toInstant(ZoneOffset.UTC)));

        final CommandStrategy.Context<PolicyId> context = getDefaultContext();
        final DittoHeaders dittoHeaders = DittoHeaders.empty();
        final ModifySubject command =
                ModifySubject.of(context.getState(), TestConstants.Policy.LABEL, subject, dittoHeaders);

        assertModificationResult(underTest, TestConstants.Policy.POLICY, command,
                SubjectCreated.class,
                ModifySubjectResponse.created(context.getState(), TestConstants.Policy.LABEL, expectedSubject,
                        appendETagToDittoHeaders(subject, dittoHeaders)));
    }

}
