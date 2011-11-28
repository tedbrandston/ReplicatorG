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
		
		TriangleList triangles = scene.getAllTheTriangles().clone();
		if (triangles == null) {
			Base.logger.info("Couldn't find valid geometry during save.");
			return;
		}

		String name = scene.getName();
		
		w.printf(l,"solid %s\n", name);
		int numFacets = triangles.length();
		
		Point3d[] normals = triangles.getNormalArray();
		Point3d[][] facets = triangles.getTriangleArray();
		for (int i = 0; i < numFacets; i++) {
			Vector3f norm3f = new Vector3f(normals[i]);
			transform.transform(norm3f);
			norm3f.normalize();
			
			transform.transform(facets[i][0]);
			transform.transform(facets[i][1]);
			transform.transform(facets[i][2]);
			
			w.printf(l,"  facet normal %e %e %e\n", norm3f.x,norm3f.y,norm3f.z);
			w.printf(l,"    outer loop\n");
			w.printf(l,"      vertex %e %e %e\n", facets[i][0].x, facets[i][0].y, facets[i][0].z);
			w.printf(l,"      vertex %e %e %e\n", facets[i][1].x, facets[i][1].y, facets[i][1].z);
			w.printf(l,"      vertex %e %e %e\n", facets[i][2].x, facets[i][2].y, facets[i][2].z);
			w.printf(l,"    endloop\n");
			w.printf(l,"  endfacet\n");
		}
		w.printf(l,"endsolid %s\n", name);
		w.close();
	}

}
