package replicatorg.app.ui.modeling.gl;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import replicatorg.app.ui.MainWindow;
import replicatorg.app.ui.modeling.AbstractEditingModel;
import replicatorg.model.gl.AABB;
import replicatorg.model.gl.BuildModel;
import replicatorg.model.gl.Scene;

/**
 * A wrapper for displaying and editing an underlying model object.
 * @author phooky
 *
 */
//GL: possible issues with 3d math:
/*
 * Matrix4d inits to 0, trasform3d inits to 1, I may have messed that up
 * I'm not sure if I want to use getTransformedBoundingBox in more places
 * All of the scaling/rotating/translating, I got no idea what I'm doing there (that's a lie, I have a reasonable idea)
 * Is shapeTransform supposed to do the same thing as my Shape.transform?
 */
public class EditingModel extends AbstractEditingModel {
	public class ReferenceFrame {
		public Point3d origin;
		public Vector3d zAxis;
		
		public ReferenceFrame() {
			origin = new Point3d();
			zAxis = new Vector3d(0d,0d,1d);
		}
	}
	
	/**
	 * The underlying model being edited.
	 */
	final protected BuildModel model;

	/**
	 * Material definition for the model, maintained so that we can update the color without reloading.
	 */
	Material objectMaterial = null;

	/** The group which represents the displayable subtree.
	 */
	Scene scene = null;
	
	/**
	 * Cache of the original shape from the model.
	 */
	private Scene originalScene;

	/**
	 * The transform group for the shape.  The enclosed transform should be applied to the shape before:
	 * * bounding box calculation
	 * * saving out the STL for skeining
	 */
	Matrix4d shapeTransform = new Matrix4d();
	
	/** We maintain a link to the main window to update the undo/redo buttons.  Kind of silly, but
	 * there it is.
	 */
	private final MainWindow mainWindow;
	
	public EditingModel(BuildModel model, final MainWindow mainWindow) {
		this.model = model;
		this.mainWindow = mainWindow;
		model.setEditListener(this);
	}

	/**
	 * Create the branchgroup that will display the object.
	 */
	private Scene makeShape(BuildModel model) {
//		originalScene = model.getShape().clone();
//
//		objectMaterial = new Material();
//		
//		return wrapper;
		throw new UnsupportedOperationException();
	}

	public void updateModelColor() {
//		if (objectMaterial != null) {
//			Color modelColor = new Color(Base.preferences.getInt("ui.modelColor",-19635));
//			
//			objectMaterial.setAmbientColor(new Color3f(modelColor));
//			objectMaterial.setDiffuseColor(new Color3f(modelColor));
//		}
		
		throw new UnsupportedOperationException();
	}
	
	public Scene getScene() {
		if (scene == null) {
			scene = makeShape(model);
		}
		
		return scene;
	}
		
	public ReferenceFrame getReferenceFrame() {
		ReferenceFrame rf = new ReferenceFrame();
		shapeTransform.transform(rf.origin);
		shapeTransform.transform(rf.zAxis);
		return rf;
	}

	/**
	 * Transform the given transform to one that operates on the centroid of the object.
	 * @param transform
	 * @param name
	 * @return
	 */
	public Matrix4d transformOnCentroid(Matrix4d transform) {
		Matrix4d old = new Matrix4d(shapeTransform);
		Matrix4d t1 = new Matrix4d();
		Matrix4d t2 = new Matrix4d();

		Vector3d t1v = new Vector3d(getCentroid());
		t1v.negate();
		t1.setIdentity();
		t1.setTranslation(t1v);
		Vector3d t2v = new Vector3d(getCentroid());
		t2.setIdentity();
		t2.setTranslation(t2v);
		
		Matrix4d composite = new Matrix4d();
		composite.setIdentity();
		composite.mul(t2);
		composite.mul(transform);
		composite.mul(t1);
		composite.mul(old);
		return composite;
	}

	/**
	 * Transform the given transform to one that operates on the bottom of the object.
	 * @param transform
	 * @param name
	 * @return
	 */
	public Matrix4d transformOnBottom(Matrix4d transform) {
		Matrix4d old = new Matrix4d(shapeTransform);
		Matrix4d t1 = new Matrix4d();
		Matrix4d t2 = new Matrix4d();

		Vector3d t1v = new Vector3d(getBottom());
		t1v.negate();
		t1.setIdentity();
		t1.setTranslation(t1v);
		Vector3d t2v = new Vector3d(getBottom());
		t2.setIdentity();
		t2.setTranslation(t2v);
		
		Matrix4d composite = new Matrix4d();
		composite.setIdentity();
		composite.mul(t2);
		composite.mul(transform);
		composite.mul(t1);
		composite.mul(old);
		return composite;
	}

	public void rotateObject(double turntable, double elevation) {
		// Skip identity translations
		if (turntable == 0.0 && elevation == 0.0) { return; }
		Matrix4d r1 = new Matrix4d();
		r1.setIdentity();
		Matrix4d r2 = new Matrix4d();
		r2.setIdentity();
		r1.rotX(elevation);
		r2.rotZ(turntable);
		r2.mul(r1);
		r2 = transformOnCentroid(r2);
		model.setTransform(r2,"rotation",isNewOp());
	}
	
	public void rotateObject(AxisAngle4d angle) {
		Matrix4d t = new Matrix4d();
		t.setIdentity();
		t.setRotation(angle);
		t = transformOnCentroid(t);
		model.setTransform(t, "rotation",isNewOp());
	}
	
	public void translateObject(double x, double y, double z) {
		// Skip identity translations
		if (x == 0.0 && y == 0.0 && z == 0.0) { return; }
		invalidateBounds();
		Matrix4d translate = new Matrix4d();
		translate.setTranslation(new Vector3d(x,y,z));
		Matrix4d old = new Matrix4d(shapeTransform);
		old.add(translate);
		model.setTransform(old,"move",isNewOp());
	}
	
	/**
	 * Flip the object tree around the Z axis.  This is particularly useful when
	 * breaking a print into two parts.
	 */
	public void flipZ() {
		Matrix4d flipZ = new Matrix4d();
		flipZ.rotY(Math.PI);
		flipZ = transformOnCentroid(flipZ);
		model.setTransform(flipZ,"flip",isNewOp());
	}

	//GL: I believe this is how to set up a mirror about individual axes
	public void mirrorX() {
		Matrix4d t = new Matrix4d();
		t.setZero();
		t.m00 = -1d;
		t.m11 = 1d;
		t.m22 = 1d;
		t = transformOnCentroid(t);
		model.setTransform(t,"mirror X",isNewOp());
	}

	//GL: I believe this is how to set up a mirror about individual axes
	public void mirrorY() {
		Matrix4d t = new Matrix4d();
		t.setZero();
		t.m00 = 1d;
		t.m11 = -1d;
		t.m22 = 1d;
		t = transformOnCentroid(t);
		model.setTransform(t,"mirror Y",isNewOp());
	}

	//GL: I believe this is how to set up a mirror about individual axes
	public void mirrorZ() {
		Matrix4d t = new Matrix4d();
		t.setZero();
		t.m00 = 1d;
		t.m11 = 1d;
		t.m22 = -1d;
		t = transformOnCentroid(t);
		model.setTransform(t,"mirror Z",isNewOp());
	}
		
	public boolean isOnPlatform() {
		AABB bbox = scene.getBoundingBox();
		return bbox.lower.z < 0.001d && bbox.lower.z > -0.001d;
	}

	public void scale(double scale, boolean isOnPlatform) {
//		Matrix4d t = new Matrix4d();
//		t.setScale(scale);
//		if (isOnPlatform) {
//			t = transformOnBottom(t);
//		} else {
//			t = transformOnCentroid(t);			
//		}
//		shapeTransform.setTransform(t);
//		model.setTransform(t,"resize",isNewOp());		
		throw new UnsupportedOperationException("Scale not implemented");
	}
	
	
	protected void validateBounds() {
		if (centroid == null) {
			AABB bbox = new AABB(scene.getBoundingBox());
			bbox.upper.interpolate(bbox.lower,0.5d);
			centroid = bbox.upper;
			bottom = new Point3d(centroid.x, centroid.y, bbox.lower.z);
		}
	}
	
	/**
	 * Center the object tree and raise its lowest point to Z=0.
	 */
	public void center() {
		AABB bbox = new AABB(scene.getBoundingBox());
		shapeTransform.transform(bbox.lower);
		shapeTransform.transform(bbox.upper);
		double zoff = -bbox.lower.z;
		double xoff = -(bbox.upper.x + bbox.lower.x)/2.0d;
		double yoff = -(bbox.upper.y + bbox.lower.y)/2.0d;
		translateObject(xoff, yoff, zoff);
	}

	/**
	 * Raise the object's lowest point to Z=0.
	 */
	public void putOnPlatform() {
		AABB bbox = new AABB(scene.getBoundingBox());
		shapeTransform.transform(bbox.lower);
		double zoff = -bbox.lower.z;
		translateObject(0d, 0d, zoff);
	}

	/**
	 * Lay the object flat with the Z object.  It computes this by finding the bottommost
	 * point, and then rotating the object to make the surface with the lowest angle to
	 * the Z plane parallel to it.
	 * 
	 * In the future, we will want to add a convex hull pass to this.
	 * 
	 */
	public void layFlat() {
//		// Compute transformation
//		Matrix4d t = new Matrix4d(shapeTransform);
//		Enumeration<?> geometries = originalScene.getAllGeometries();
//		while (geometries.hasMoreElements()) {
//			Geometry g = (Geometry)geometries.nextElement();
//			double lowest = Double.MAX_VALUE;
//			Vector3d flattest = new Vector3d(1d,0d,0d);
//			if (g instanceof GeometryArray) {
//				GeometryArray ga = (GeometryArray)g;
//				Point3d p1 = new Point3d();
//				Point3d p2 = new Point3d();
//				Point3d p3 = new Point3d();
//				for (int i = 0; i < ga.getVertexCount();) {
//					ga.getCoordinate(i++,p1);
//					ga.getCoordinate(i++,p2);
//					ga.getCoordinate(i++,p3);
//					t.transform(p1);
//					t.transform(p2);
//					t.transform(p3);
//					double triLowest = Math.min(p1.z, Math.min(p2.z, p3.z));
//					if (triLowest < lowest) {
//						// Clear any prior triangles
//						flattest = new Vector3d(1d,0d,0d);
//						lowest = triLowest;
//					}
//					if (triLowest == lowest) {
//						// This triangle is a candidate!
//						Vector3d v1 = new Vector3d(p2);
//						v1.sub(p1);
//						Vector3d v2 = new Vector3d(p3);
//						v2.sub(p2);
//						Vector3d v = new Vector3d();
//						v.cross(v1,v2);
//						v.normalize();
//						if (v.z < flattest.z) { flattest = v; }
//					}
//				}
//			}
//			Matrix4d flattenTransform = new Matrix4d();
//			Vector3d downZ = new Vector3d(0d,0d,-1d);
//			double angle = Math.acos(flattest.dot(downZ));
//			Vector3d cross = new Vector3d();
//			cross.cross(flattest, downZ);
//			flattenTransform.setRotation(new AxisAngle4d(cross,angle));
//			flattenTransform = transformOnCentroid(flattenTransform);
//			shapeTransform.setTransform(flattenTransform);
//			model.setTransform(flattenTransform,"Lay flat", isNewOp());
//			invalidateBounds(); 
//		}
		throw new UnsupportedOperationException("Lay Flat not implemented");
	}

	@Override
	public void modelTransformChanged() {
		shapeTransform = new Matrix4d(model.getTransform());
		mainWindow.updateUndo();
	}
}
