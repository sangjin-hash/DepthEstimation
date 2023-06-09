package com.shubham0204.ml.depthestimation.model;

import android.media.Image;

import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.Session;

import java.nio.FloatBuffer;

/**
 * Stores depth data from ARCore as a 3D pointcloud. Points are added by calling the Raw Depth API,
 * and reprojected into 3D space. The points are stored relative to an anchor created with each
 * instance. The color of the points are matched with the latest color image from the same frame.
 */
public final class DepthData {
    /** Buffer of point coordinates and confidence values. */
    private FloatBuffer points;

    /** Buffer of point RGB color values. */
    private FloatBuffer colors;

    /** The anchor to the 3D position of the camera at the point of depth acquisition. */
    private final Anchor anchor;

    /** The timestamp in nanoseconds when the raw depth image was observed. */
    private long timestamp;

    private DepthData(
            FloatBuffer points, FloatBuffer colors, long timestamp, Anchor cameraPoseAnchor) {
        this.points = points;
        this.colors = colors;
        this.timestamp = timestamp;
        this.anchor = cameraPoseAnchor;
    }

    public static DepthData create(Session session, Frame frame) {
        try (Image cameraImage = frame.acquireCameraImage()) {
            // Depth images vary in size depending on device, and can be large on devices with a depth
            // camera. To ensure smooth framerate, we cap the number of points each frame.
            final int maxNumberOfPointsToRender = 15000;
            PointCloudHelper.convertImageToDepthAndColors(cameraImage, depthImage, confidenceImage,
                    frame, maxNumberOfPointsToRender);

            Anchor cameraPoseAnchor = session.createAnchor(frame.getCamera().getPose());
            return new DepthData(frameData.get(frameData.size()-1).getPoints(),
                    frameData.get(frameData.size()-1).getColors(),
                    depthImage.getTimestamp(),
                    cameraPoseAnchor);
        } catch (NotYetAvailableException e) {
            // This normally means that depth data is not available yet. This is normal so we will not
            // spam the logcat with this.
        }

        return null;
    }

    /**
     * Buffer of point coordinates and confidence values.
     *
     * <p>Each point is represented by four consecutive values in the buffer; first the X, Y, Z
     * position coordinates, followed by a confidence value. This is the same format as described in
     * {@link android.graphics.ImageFormat#DEPTH_POINT_CLOUD}.
     *
     * <p>Point locations are in the world coordinate space, consistent with the camera position for
     * the frame that provided the point cloud.
     */
    public FloatBuffer getPoints() {
        return points;
    }

    /**
     * Buffer of point RGB values from the color camera.
     *
     * <p>Each point is represented by three consecutive values in the buffer for the red, green and
     * blue image channels. The values for each color are in 0-1 range (inclusive).
     */
    public FloatBuffer getColors() {
        return colors;
    }

    /** Returns the anchor corresponding to the camera pose where the depth data was acquired. */
    public Anchor getAnchor() {
        return anchor;
    }

    /**
     * Retrieves the linearized column-major 4x4 matrix representing the transform from pointcloud to
     * the session coordinates.
     */
    public void getModelMatrix(float[] modelMatrix) {
        anchor.getPose().toMatrix(modelMatrix, 0);
    }

}
