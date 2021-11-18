package com.google.android.gnd.ui.home.mapcontainer;

import static com.google.android.gnd.TestObservers.observeUntilFirstChange;
import static com.google.common.truth.Truth.assertThat;

import android.view.View;
import com.google.android.gnd.BaseHiltTest;
import com.google.android.gnd.FakeData;
import com.google.android.gnd.TestObservers;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.rx.Nil;
import dagger.hilt.android.testing.HiltAndroidTest;
import io.reactivex.observers.TestObserver;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@HiltAndroidTest
@RunWith(RobolectricTestRunner.class)
public class PolygonDrawingViewModelTest extends BaseHiltTest {

  @Inject PolygonDrawingViewModel viewModel;

  @Override
  public void setUp() {
    super.setUp();

    // Initialize polygon drawing
    viewModel.startDrawingFlow(FakeData.PROJECT, FakeData.LAYER);
  }

  @Test
  public void testSelectCurrentVertex_whenCameraTargetIsNotAvailable() {
    viewModel.selectCurrentVertex();

    assertThat(viewModel.getVertexCount()).isEqualTo(0);
  }

  @Test
  public void testSelectCurrentVertex_whenCameraTargetIsAvailable() {
    viewModel.onCameraMoved(newPoint(0.0, 0.0));
    viewModel.selectCurrentVertex();

    assertPolygonFeatureMutated(1);
  }

  @Test
  public void testSelectMultipleVertices() {
    viewModel.onCameraMoved(newPoint(0.0, 0.0));
    viewModel.selectCurrentVertex();
    viewModel.onCameraMoved(newPoint(10.0, 10.0));
    viewModel.selectCurrentVertex();
    viewModel.onCameraMoved(newPoint(20.0, 20.0));
    viewModel.selectCurrentVertex();

    assertPolygonFeatureMutated(3);
    assertCompleteButtonVisible(View.INVISIBLE);
  }

  @Test
  public void testUpdateLastVertex_whenVertexCountLessThan3() {
    viewModel.updateLastVertex(newPoint(0.0, 0.0), 100);
    viewModel.updateLastVertex(newPoint(10.0, 10.0), 100);
    viewModel.updateLastVertex(newPoint(20.0, 20.0), 100);

    assertPolygonFeatureMutated(1);
    assertCompleteButtonVisible(View.INVISIBLE);
  }

  @Test
  public void testUpdateLastVertex_whenVertexCountEqualTo3AndLastVertexIsNotNearFirstPoint() {
    // Select 3 vertices
    viewModel.onCameraMoved(newPoint(0.0, 0.0));
    viewModel.selectCurrentVertex();
    viewModel.onCameraMoved(newPoint(10.0, 10.0));
    viewModel.selectCurrentVertex();
    viewModel.onCameraMoved(newPoint(20.0, 20.0));
    viewModel.selectCurrentVertex();

    // Move camera such that distance from last vertex is more than threshold
    viewModel.updateLastVertex(newPoint(30.0, 30.0), 25);

    assertPolygonFeatureMutated(4);
    assertCompleteButtonVisible(View.INVISIBLE);
    assertThat(viewModel.getFirstVertex()).isNotEqualTo(viewModel.getLastVertex());
  }

  @Test
  public void testUpdateLastVertex_whenVertexCountEqualTo3AndLastVertexIsNearFirstPoint() {
    // Select 3 vertices
    viewModel.onCameraMoved(newPoint(0.0, 0.0));
    viewModel.selectCurrentVertex();
    viewModel.onCameraMoved(newPoint(10.0, 10.0));
    viewModel.selectCurrentVertex();
    viewModel.onCameraMoved(newPoint(20.0, 20.0));
    viewModel.selectCurrentVertex();

    // Move camera such that distance from last vertex is equal to threshold
    viewModel.updateLastVertex(newPoint(30.0, 30.0), 24);

    assertPolygonFeatureMutated(4);
    assertCompleteButtonVisible(View.VISIBLE);
    assertThat(viewModel.getFirstVertex()).isEqualTo(viewModel.getLastVertex());
  }

  @Test
  public void testRemoveLastVertex() {
    viewModel.onCameraMoved(newPoint(0.0, 0.0));
    viewModel.selectCurrentVertex();

    viewModel.removeLastVertex();

    assertPolygonFeatureMutated(0);
    assertCompleteButtonVisible(View.INVISIBLE);
  }

  @Test
  public void testRemoveLastVertex_whenNothingIsSelected() {
    TestObserver<Nil> testObserver = viewModel.getDefaultMapMode().test();

    viewModel.removeLastVertex();

    testObserver.assertValue(Nil.NIL);
  }

  @Test
  public void testRemoveLastVertex_whenPolygonIsComplete() {
    viewModel.onCameraMoved(newPoint(0.0, 0.0));
    viewModel.selectCurrentVertex();
    viewModel.onCameraMoved(newPoint(10.0, 10.0));
    viewModel.selectCurrentVertex();
    viewModel.onCameraMoved(newPoint(20.0, 20.0));
    viewModel.selectCurrentVertex();
    viewModel.updateLastVertex(newPoint(30.0, 30.0), 24);

    viewModel.removeLastVertex();

    assertPolygonFeatureMutated(3);
    assertCompleteButtonVisible(View.INVISIBLE);
  }

  private void assertCompleteButtonVisible(int visibility) {
    TestObservers.observeUntilFirstChange(viewModel.getPolygonDrawingCompletedVisibility());
    assertThat(viewModel.getPolygonDrawingCompletedVisibility().getValue()).isEqualTo(visibility);
  }

  private void assertPolygonFeatureMutated(int vertexCount) {
    observeUntilFirstChange(viewModel.getPolygonFeature());
    assertThat(viewModel.getPolygonFeature().getValue().getVertices()).hasSize(vertexCount);
  }

  private Point newPoint(double latitude, double longitude) {
    return Point.newBuilder().setLatitude(latitude).setLongitude(longitude).build();
  }
}
