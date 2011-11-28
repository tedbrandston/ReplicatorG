package replicatorg.modelgl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.media.opengl.GL2;
import javax.media.opengl.GLContext;
import javax.vecmath.Point3d;

/**
 * @author thbrandston
 *
 */
public class Scene extends Shape{

	private List<Shape> shapes;
	
	private AABB bboxCache;

	public Scene(List<Shape> shapes, GLContext context, int listNum)
	{
		super(context, listNum);
		
		this.shapes = shapes;
		
		compile();
	}
	public Scene(List<Shape> shapes)
	{
		this(shapes, null, -1);
	}
	public Scene(Shape[] shapes, GLContext context, int listNum)
	{
		this(Arrays.asList(shapes), context, listNum);
	}
	public Scene(Shape[] shapes)
	{
		this(shapes, null, -1);
	}
	
	public void add(Shape s)
	{
		shapes.add(s);
		compile();
	}
	
	public void remove(Shape s)
	{
		shapes.remove(s);
		compile();
	}
	
	public TriangleList getAllTheTriangles()
	{
		int length = 0;
		for(Shape s : shapes)
		{
			if(s instanceof TriangleDisplayList)
				length += ((TriangleDisplayList)s).getTriangles().length();
		}
		
		Point3d[][] triangles = new Point3d[length][3];
		Point3d[] normals = new Point3d[length];
		int i = 0;

		for(Shape s : shapes)
		{
			if(s instanceof TriangleDisplayList)
			{
				TriangleDisplayList tdl = (TriangleDisplayList)s;
				TriangleList tl = tdl.getTriangles().clone();
				tl.transform(tdl.getTransform());
				Point3d[][] loadTriangles = tl.getTriangleArray();
				Point3d[] loadNormals = tl.getNormalArray();
				
				for(int j = 0; j < loadTriangles.length; j++)
				{
					triangles[i] = loadTriangles[j];
					normals[i] = loadNormals[j];
					i++;
				}
			}
		}
		
		return new TriangleList(triangles, normals);
	}
	
	@Override
	public void compile()
	{
		context.makeCurrent();
		GL2 gl = context.getGL().getGL2();
		
		gl.glNewList(listNum, GL2.GL_COMPILE);
		
		bboxCache = new AABB();
		
		for(Shape s : shapes)
		{
			s.callList();
			
			// build a bounding box for the scene
			bboxCache.incorporate(s.getBoundingBox());
		}

		gl.glEndList();
	}

	@Override
	public void applyTransform()
	{
		for(Shape s : shapes)
			s.transform(transform);
		transform.setIdentity();
	}
	@Override
	public Scene clone() {
		
		List<Shape> tmp = new ArrayList<Shape>(shapes);
		
		// clone everything in the list, to get a deep copy
		for(Shape s : tmp)
			s = s.clone();
				
		return new Scene(tmp);
	}
	@Override
	public AABB getBoundingBox() {
		// TODO Auto-generated method stub
		return null;
	}
}
