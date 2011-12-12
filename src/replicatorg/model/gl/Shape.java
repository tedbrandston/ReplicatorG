package replicatorg.model.gl;

import javax.media.opengl.GL2;
import javax.media.opengl.GLContext;
import javax.vecmath.Matrix4d;

public abstract class Shape implements Cloneable{

	protected final GLContext context;
	protected final int listNum;
	protected String name;
	protected Matrix4d transform;
	protected AABB bbox;
	
	public Shape(GLContext context, int listNum)
	{
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
		
		transform = new Matrix4d();
		transform.setIdentity();
	}

	/**
	 * Draws this list to its GLContext, wraps glCallList
	 */
	public void callList() {
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
	 * reconstruct our display list
	 */
	public abstract void compile();

	/**
	 * The name of this ShapeList
	 * @return The name of this ShapeList
	 */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getNumber() {
		return listNum;
	}

	public Matrix4d getTransform()
	{
		return transform;
	}
	
	public void setTransform(Matrix4d transform) {
		this.transform = transform;
	}
	
	public AABB getBoundingBox()
	{
		return bbox;
	}
	
	public AABB getTransformedBoundingBox()
	{
		AABB tmp = new AABB(getBoundingBox());
		transform.transform(tmp.lower);
		transform.transform(tmp.upper);
		return tmp;
	}

	/**
	 * Multiplies the current transformation by the supplied matrix
	 * @param transform
	 */
	public void transform(Matrix4d transform) {
		this.transform.mul(transform);
	}

	//TODO: come up with a clearer way to explain this function
	/**
	 * 'Applies' the transformation to the shape. 
	 * 
	 * After this call the transformation matrix will be the identity matrix, 
	 * and the shape will be transformed. 
	 */
	public abstract void applyTransform();

	/**
	 * Remove this display list.  Wraps glDeleteLists().
	 */
	public void delete() {
		context.makeCurrent();
		GL2 gl = context.getGL().getGL2();
		
		gl.glDeleteLists(listNum, 1);
	}
	
	@Override
	public abstract Shape clone();
}