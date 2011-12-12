package replicatorg.model.gl;

import javax.media.opengl.GL2;
import javax.media.opengl.GLContext;
import javax.vecmath.Point3d;

/**
 * A class for holding an opengl display list of triangles, along with any
 * necessary meta data.
 */
public class TriangleDisplayList extends Shape{

	private final TriangleList triangles;
	
	/**
	 * Constructs a new TriangleDisplayList in the given GLContext referring to
	 * a display list with the given number.
	 * 
	 * If context is null, TriangleDisplayList will use the current one. 
	 * If listNum is -1 TriangleDisplayList will generate its own list number. 
	 * 
	 * @param context The GLContext on which the display list this represents
	 * 				  resides, or null
	 * @param listNum The number of the aforementioned display list or -1
	 */
	public TriangleDisplayList(TriangleList tl, GLContext context, int listNum)
	{
		super(context, listNum);
		
		if(tl == null)
		{
			throw new IllegalArgumentException(
					"What's a TriangleDisplayList without triangles to display?");
		}
		triangles = tl;
		
		compile();
	}
	/**
	 * convenience constructor, equivalent to calling 
	 * TriangleDisplayList(TriangleList, null, -1);
	 * @param tl
	 */
	public TriangleDisplayList(TriangleList tl)
	{
		this(tl, null, -1);
	}
	
	/**
	 * reconstruct our list to call all our sublists
	 */
	/*
	 * While we're looping through stuff, we can also re-calculate our AABB
	 */
	@Override
	public void compile()
	{
		context.makeCurrent();
		GL2 gl = context.getGL().getGL2();
		
		Point3d[][] t = triangles.getTriangleArray();
		Point3d[] n = triangles.getNormalArray();
		
		bbox = new AABB();
		
		gl.glNewList(listNum, GL2.GL_COMPILE);
		gl.glBegin(GL2.GL_TRIANGLES);
		for(int i = 0; i < triangles.length(); i++)
		{
        	// draw triangle to our gl object
        	if(n != null)
        		gl.glNormal3d(n[i].x, n[i].y, n[i].z);
        	gl.glVertex3d(t[i][0].x, t[i][0].y, t[i][0].z);
        	gl.glVertex3d(t[i][1].x, t[i][1].y, t[i][1].z);
        	gl.glVertex3d(t[i][2].x, t[i][2].y, t[i][2].z);
        	
        	// add the points to our bounding box 
        	bbox.incorporate(t[i][0]);
        	bbox.incorporate(t[i][1]);
        	bbox.incorporate(t[i][2]);
		}
		gl.glEnd();
		gl.glEndList();
	}
	
	public TriangleList getTriangles()
	{
		return triangles;
	}
	
	@Override
	public void applyTransform()
	{
		triangles.transform(transform);
		transform.setIdentity();
		compile();
	}
	@Override
	public TriangleDisplayList clone() {
		return new TriangleDisplayList(triangles.clone());
	}
}
