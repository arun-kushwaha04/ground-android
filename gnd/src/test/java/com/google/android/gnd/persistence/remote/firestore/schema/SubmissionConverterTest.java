/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gnd.persistence.remote.firestore.schema;

import static com.google.android.gnd.model.TestModelBuilders.newAuditInfo;
import static com.google.android.gnd.model.TestModelBuilders.newField;
import static com.google.android.gnd.model.TestModelBuilders.newForm;
import static com.google.android.gnd.model.TestModelBuilders.newLayer;
import static com.google.android.gnd.model.TestModelBuilders.newPointFeature;
import static com.google.android.gnd.model.TestModelBuilders.newProject;
import static com.google.android.gnd.model.TestModelBuilders.newUser;
import static com.google.android.gnd.util.ImmutableListCollector.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static java8.util.J8Arrays.stream;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import com.google.android.gnd.model.AuditInfo;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.model.submission.MultipleChoiceResponse;
import com.google.android.gnd.model.submission.ResponseMap;
import com.google.android.gnd.model.submission.Submission;
import com.google.android.gnd.model.submission.TextResponse;
import com.google.android.gnd.model.task.Element;
import com.google.android.gnd.model.task.Field;
import com.google.android.gnd.model.task.MultipleChoice;
import com.google.android.gnd.model.task.MultipleChoice.Cardinality;
import com.google.android.gnd.model.task.Task;
import com.google.android.gnd.persistence.remote.DataStoreException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.Date;
import java8.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SubmissionConverterTest {

  @Mock
  private DocumentSnapshot observationDocumentSnapshot;

  private Task task;
  private Layer layer;
  private Project project;
  private Feature feature;

  private static final AuditInfo AUDIT_INFO_1 =
      newAuditInfo()
          .setUser(newUser().setId("user1").build())
          .setClientTimestamp(new Date(100))
          .setServerTimestamp(Optional.of(new Date(101)))
          .build();

  private static final AuditInfo AUDIT_INFO_2 =
      newAuditInfo()
          .setUser(newUser().setId("user2").build())
          .setClientTimestamp(new Date(200))
          .setServerTimestamp(Optional.of(new Date(201)))
          .build();

  private static final AuditInfoNestedObject AUDIT_INFO_1_NESTED_OBJECT =
      new AuditInfoNestedObject(
          new UserNestedObject("user1", null, null),
          new Timestamp(new Date(100)),
          new Timestamp(new Date(101)));

  private static final AuditInfoNestedObject AUDIT_INFO_2_NESTED_OBJECT =
      new AuditInfoNestedObject(
          new UserNestedObject("user2", null, null),
          new Timestamp(new Date(200)),
          new Timestamp(new Date(201)));

  @Test
  public void testToSubmission() {
    setUpTestProject(
        "layer001",
        "form001",
        newField().setId("field1").setType(Field.Type.TEXT_FIELD).build(),
        newField()
            .setId("field2")
            .setType(Field.Type.MULTIPLE_CHOICE)
            .setMultipleChoice(
                MultipleChoice.newBuilder().setCardinality(Cardinality.SELECT_ONE).build())
            .build(),
        newField().setId("field3").setType(Field.Type.MULTIPLE_CHOICE).build(),
        newField().setId("field4").setType(Field.Type.PHOTO).build());
    setUpTestFeature("feature001");
    mockObservationDocumentSnapshot(
        "observation123",
        new ObservationDocument(
            /* featureId */
            "feature001",
            /* taskId */
            "form001",
            /* created */
            AUDIT_INFO_1_NESTED_OBJECT,
            /* lastModified */
            AUDIT_INFO_2_NESTED_OBJECT,
            /* responses */
            ImmutableMap.of(
                "field1",
                "Text response",
                "field2",
                ImmutableList.of("option2"),
                "field3",
                ImmutableList.of("optionA", "optionB"),
                "field4",
                "Photo URL")));

    assertThat(toSubmission())
        .isEqualTo(
            Submission.newBuilder()
                .setId("observation123")
                .setProject(project)
                .setFeature(feature)
                .setTask(task)
                .setResponses(
                    ResponseMap.builder()
                        .putResponse("field1", new TextResponse("Text response"))
                        .putResponse(
                            "field2", new MultipleChoiceResponse(null, ImmutableList.of("option2")))
                        .putResponse(
                            "field3",
                            new MultipleChoiceResponse(
                                null, ImmutableList.of("optionA", "optionB")))
                        .putResponse("field4", new TextResponse("Photo URL"))
                        .build())
                .setCreated(AUDIT_INFO_1)
                .setLastModified(AUDIT_INFO_2)
                .build());
  }

  @Test
  public void testToSubmission_mismatchedFeatureId() {
    setUpTestProject(
        "layer001", "form001", newField().setId("field1").setType(Field.Type.TEXT_FIELD).build());
    setUpTestFeature("feature001");
    mockObservationDocumentSnapshot(
        "observation123",
        new ObservationDocument(
            /* featureId */
            "feature999",
            /* taskId */
            "form001",
            /* created */
            AUDIT_INFO_1_NESTED_OBJECT,
            /* lastModified */
            AUDIT_INFO_2_NESTED_OBJECT,
            /* responses */
            ImmutableMap.of("field1", "")));

    assertThrows(DataStoreException.class, this::toSubmission);
  }

  @Test
  public void testToSubmission_nullResponses() {
    setUpTestProject(
        "layer001", "form001", newField().setId("field1").setType(Field.Type.TEXT_FIELD).build());
    setUpTestFeature("feature001");
    mockObservationDocumentSnapshot(
        "observation123",
        new ObservationDocument(
            /* featureId */
            "feature001",
            /* taskId */
            "form001",
            /* created */
            AUDIT_INFO_1_NESTED_OBJECT,
            /* lastModified */
            AUDIT_INFO_2_NESTED_OBJECT,
            /* responses */
            null));

    assertThat(toSubmission())
        .isEqualTo(
            Submission.newBuilder()
                .setId("observation123")
                .setProject(project)
                .setFeature(feature)
                .setTask(task)
                .setCreated(AUDIT_INFO_1)
                .setLastModified(AUDIT_INFO_2)
                .build());
  }

  @Test
  public void testToSubmission_emptyTextResponse() {
    setUpTestProject(
        "layer001", "form001", newField().setId("field1").setType(Field.Type.TEXT_FIELD).build());
    setUpTestFeature("feature001");
    mockObservationDocumentSnapshot(
        "observation123",
        new ObservationDocument(
            /* featureId */
            "feature001",
            /* taskId */
            "form001",
            /* created */
            AUDIT_INFO_1_NESTED_OBJECT,
            /* lastModified */
            AUDIT_INFO_2_NESTED_OBJECT,
            /* responses */
            ImmutableMap.of("field1", "")));

    assertThat(toSubmission())
        .isEqualTo(
            Submission.newBuilder()
                .setId("observation123")
                .setProject(project)
                .setFeature(feature)
                .setTask(task)
                .setCreated(AUDIT_INFO_1)
                .setLastModified(AUDIT_INFO_2)
                .build());
  }

  @Test
  public void testToSubmission_emptyMultipleChoiceResponse() {
    setUpTestProject(
        "layer001",
        "form001",
        newField().setId("field1").setType(Field.Type.MULTIPLE_CHOICE).build());
    setUpTestFeature("feature001");
    mockObservationDocumentSnapshot(
        "observation123",
        new ObservationDocument(
            /* featureId */
            "feature001",
            /* taskId */
            "form001",
            /* created */
            AUDIT_INFO_1_NESTED_OBJECT,
            /* lastModified */
            AUDIT_INFO_2_NESTED_OBJECT,
            /* responses */
            ImmutableMap.of("field1", ImmutableList.of())));

    assertThat(toSubmission())
        .isEqualTo(
            Submission.newBuilder()
                .setId("observation123")
                .setProject(project)
                .setFeature(feature)
                .setTask(task)
                .setCreated(AUDIT_INFO_1)
                .setLastModified(AUDIT_INFO_2)
                .build());
  }

  private void setUpTestProject(String layerId, String taskId, Field... fields) {
    task =
        newForm()
            .setId(taskId)
            .setElements(stream(fields).map(Element::ofField).collect(toImmutableList()))
            .build();
    layer = newLayer().setId(layerId).setTask(task).build();
    project = newProject().putLayer(layer).build();
  }

  @Test
  public void testToSubmission_unknownFieldType() {
    setUpTestProject(
        "layer001",
        "form001",
        newField().setId("field1").setType(Field.Type.UNKNOWN).build(),
        newField().setId("field2").setType(Field.Type.TEXT_FIELD).build());
    setUpTestFeature("feature001");
    mockObservationDocumentSnapshot(
        "observation123",
        new ObservationDocument(
            /* featureId */
            "feature001",
            /* taskId */
            "form001",
            /* created */
            AUDIT_INFO_1_NESTED_OBJECT,
            /* lastModified */
            AUDIT_INFO_2_NESTED_OBJECT,
            /* responses */
            ImmutableMap.of("field1", "Unknown", "field2", "Text response")));

    assertThat(toSubmission())
        .isEqualTo(
            Submission.newBuilder()
                .setId("observation123")
                .setProject(project)
                .setFeature(feature)
                .setTask(task)
                .setResponses(
                    // Field "field1" with unknown field type ignored.
                    ResponseMap.builder()
                        .putResponse("field2", new TextResponse("Text response"))
                        .build())
                .setCreated(AUDIT_INFO_1)
                .setLastModified(AUDIT_INFO_2)
                .build());
  }

  private void setUpTestFeature(String featureId) {
    feature = newPointFeature().setId(featureId).setProject(project).setLayer(layer).build();
  }

  /**
   * Mock submission document snapshot to return the specified id and object representation.
   */
  private void mockObservationDocumentSnapshot(String id, ObservationDocument doc) {
    when(observationDocumentSnapshot.getId()).thenReturn(id);
    when(observationDocumentSnapshot.toObject(ObservationDocument.class)).thenReturn(doc);
  }

  private Submission toSubmission() {
    return ObservationConverter.toSubmission(feature, observationDocumentSnapshot);
  }
}
