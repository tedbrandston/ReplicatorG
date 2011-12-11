package replicatorg.app.ui.modeling;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import replicatorg.model.AbstractBuildModel;

public abstract class AbstractEditingModel {

	public class ReferenceFrame {
		public Point3d origin;
		public Vector3d zAxis;
		
		public ReferenceFrame() {
			origin = new Point3d();
			zAxis = new Vector3d(0d,0d,1d);
		}
	}
	
	private AbstractBuildModel model;
	protected Point3d centroid;
	protected Point3d bottom;
	private boolean inDrag;
	private boolean firstDrag;

	public AbstractEditingModel() {
		super();
	}

	public AbstractBuildModel getBuildModel() {
		return model; 
	}

	public abstract void updateModelColor();

	public abstract void rotateObject(double turntable, double elevation);
	public abstract void rotateObject(AxisAngle4d angle);

	public abstract void modelTransformChanged();

	public abstract void translateObject(double x, double y, double z);

	/**
	 * Flip the object tree around the Z axis.  This is particularly useful when
	 * breaking a print into two parts.
	 */
	public abstract void flipZ();

	public abstract void mirrorX();

	public abstract void mirrorY();

	public abstract void mirrorZ();

	public abstract boolean isOnPlatform();

	public abstract void scale(double scale, boolean isOnPlatform);

	public Point3d getCentroid() {
		validateBounds();
		return centroid;
	}

	public Point3d getBottom() {
		validateBounds();
		return bottom;
	}

	protected void invalidateBounds() {
		centroid = null;
		bottom = null;
	}
	
	protected abstract void validateBounds();

	/**
	 * Center the object and raise its lowest point to Z=0.
	 */
	public abstract void center();

	/**
	 * Raise the object's lowest point to Z=0.
	 */
	public abstract void putOnPlatform();

	/**
	 * Lay the object flat with the Z object.  It computes this by finding the bottommost
	 * point, and then rotating the object to make the surface with the lowest angle to
	 * the Z plane parallel to it.
	 * 
	 * In the future, we will want to add a convex hull pass to this.
	 * 
	 */
	public abstract void layFlat();

	protected boolean isNewOp() {
		if (!inDrag) { return true; }
		if (firstDrag) {
			firstDrag = false;
			return true;
		}
		return false;
	}

	public void startDrag() {
		inDrag = true;
		firstDrag = true;
	}

	public void endDrag() {
		inDrag = false;
	}

}