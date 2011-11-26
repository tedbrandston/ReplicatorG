package replicatorg.modelgl.io.stl;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Locale;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3f;

import replicatorg.app.Base;
import replicatorg.modelgl.Scene;
import replicatorg.modelgl.TriangleList;
import replicatorg.modelgl.io.ModelWriter;

public class StlAsciiWriter extends ModelWriter {
	public StlAsciiWriter(OutputStream ostream) {
		super(ostream);
	}
	
	Locale l = Locale.US;
	
	@Override
	public void writeShape(Scene scene, Matrix4d transform) {
		PrintWriter w = new PrintWriter(ostream);
		TriangleList triangles = scene.getAllTheTriangles();
		if (triangles == null) {
			Base.logger.info("Couldn't find valid geometry during save.");
			return;
		}

		String name = scene.getName();
		
		w.printf(l,"solid %s\n", name);
		int facets = triangles.length();
		
		float[] norm = new float[3];
		double[] coord = new double[3];
		for (int faceIdx = 0; faceIdx < facets; faceIdx++) {
			triangles.getNormal(faceIdx*3, norm);
			Vector3f norm3f = new Vector3f(norm);
			transform.transform(norm3f);
			norm3f.normalize();
			w.printf(l,"  facet normal %e %e %e\n", norm3f.x,norm3f.y,norm3f.z);
			w.printf(l,"    outer loop\n");
			Point3d face3d;
			triangles.getCoordinate(faceIdx*3, coord);
			face3d = new Point3d(coord);
			transform.transform(face3d);
			w.printf(l,"      vertex %e %e %e\n", face3d.x,face3d.y,face3d.z);
			triangles.getCoordinate((faceIdx*3)+1, coord);
			face3d = new Point3d(coord);
			transform.transform(face3d);
			w.printf(l,"      vertex %e %e %e\n", face3d.x,face3d.y,face3d.z);
			triangles.getCoordinate((faceIdx*3)+2, coord);
			face3d = new Point3d(coord);
			transform.transform(face3d);
			w.printf(l,"      vertex %e %e %e\n", face3d.x,face3d.y,face3d.z);
			w.printf(l,"    endloop\n");
			w.printf(l,"  endfacet\n");
		}
		w.printf(l,"endsolid %s\n", name);
		w.close();
	}

}
