package replicatorg.modelgl.io;

import java.io.IOException;
import java.io.OutputStream;

import javax.vecmath.Matrix4d;

public abstract class ModelWriter {
	protected OutputStream ostream;
	
	public ModelWriter(OutputStream ostream) {
		this.ostream = ostream;
	}
	
	public void close() throws IOException {
		ostream.close();
	}
	
//	protected TriangleArray getGeometry(Shape3D shape) {
//		Geometry g = shape.getGeometry();
//		if (g instanceof TriangleArray) { return (TriangleArray)g; }
//		return null;
//	}
	
	/**
	 * Write the given shape to the output stream, applying the given transform to all points.
	 * @param shape
	 * @param transform
	 */
	abstract public void writeShape(int[] shapes, Matrix4d transform);
}