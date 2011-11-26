package replicatorg.modelgl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLContext;
import javax.vecmath.Matrix4d;

/**
 * A class for holding an opengl display list of triangles, along with any
 * necessary meta data.
 * 
 * might want to pull an interface out of this at some point, for anything that
 * uses something besides triangles? like quads?
 */
public class TriangleDisplayList /*implements Shape*/{

	// We use this context to make sure all our display lists are on the same 
	// GL pipeline
	private final GLContext context;
	private final int listNum;
	
	private String name;
	
	private final TriangleList triangles;
	private Matrix4d transform;
	
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
		if(tl == null)
		{
			throw new IllegalArgumentException(
					"What's a TriangleDisplayList without triangles to display?");
		}
		triangles = tl;
		
		if(context == null)
			this.context = GLContext.getCurrent();
		else
			this.context = context;
		
		if(listNum == -1)
		{
			this.context.makeCurrent();
			GL2 gl = this.context.getGL().getGL2();
			this.listNum = gl.glGenLists(1);
		}
		else
		{
			this.listNum = listNum;
		}
		
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
	 * Draws this list to its GLContext, wraps glCallList
	 */
	public void callList()
	{
		context.makeCurrent();
		GL2 gl = context.getGL().getGL2();
		
		double[] matrix = new double[]{
				transform.m00, transform.m01, transform.m02, transform.m03,
				transform.m10, transform.m11, transform.m12, transform.m13,
				transform.m20, transform.m21, transform.m22, transform.m23,
				transform.m30, transform.m31, transform.m32, transform.m33
		};
		
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPushMatrix();
		
		gl.glMultMatrixd(matrix, 0);
		gl.glCallList(listNum);
		
		gl.glPopMatrix();
	}
	
	/**
	 * reconstruct our list to call all our sublists
	 */
	private void compile()
	{
		context.makeCurrent();
		GL2 gl = context.getGL().getGL2();
		
		double[][][] t = triangles.getTriangleArray();
		double[][] n = triangles.getNormalArray();
		
		gl.glNewList(listNum, GL2.GL_COMPILE);
		for(int i = 0; i < triangles.length(); i++)
		{
        	// draw triangle to our gl object
        	gl.glNormal3dv(n[i], 0);
        	gl.glVertex3dv(t[i][0], 0);
        	gl.glVertex3dv(t[i][1], 0);
        	gl.glVertex3dv(t[i][2], 0);
		}
		gl.glEndList();
		
	}
	
	/**
	 * The name of this ShapeList
	 * @return The name of this ShapeList
	 */
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public int getNumber()
	{
		return listNum;
	}
	
	public TriangleList getTriangles()
	{
		return triangles;
	}
	
	public void setTransform(Matrix4d transform)
	{
		this.transform = transform;
	}
	
	public void transform(Matrix4d transform)
	{
		this.transform.mul(transform);
	}
	
	public void applyTransform()
	{
		triangles.transform(transform);
	}
	
	/**
	 * Remove this display list.  Wraps glDeleteLists().
	 */
	public void delete()
	{
		context.makeCurrent();
		GL2 gl = context.getGL().getGL2();
		
		gl.glDeleteLists(listNum, 1);
	}
}
