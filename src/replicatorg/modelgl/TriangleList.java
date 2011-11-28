package replicatorg.modelgl;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;

public final class TriangleList implements Cloneable{

	private final Point3d[][] triangles;
	private final Point3d[] normals;
	private final int length;
	
	public TriangleList(Point3d[][] triangles, Point3d[] normals)
	{
		// verify input
		if(triangles.length != normals.length)
		{
			this.triangles = null;
			this.normals = null;
			throw new IllegalArgumentException(
					"TriangleList needs exactly one normal for each triangle");
		}
		length = triangles.length;
		
		for(int i = 0; i < length; i++)
		{
			if(triangles[i].length != 3)
			{
				this.triangles = null;
				this.normals = null;
				throw new IllegalArgumentException(
						"Each triangle needs exactly three points.");
			}
		}
		//done verifying!
		
		this.triangles = triangles;
		this.normals = normals;
	}
	
	public Point3d[][] getTriangleArray()
	{
		return triangles;
	}

	public Point3d[] getNormalArray()
	{
		return normals;
	}
	
	public int length()
	{
		return length;
	}
	
	public void transform(Matrix4d trans)
	{
		// Yes, this method can throw NullPointerExceptions. we don't deal with
		// them here, because you shouldn't try to use a TriangleList if it 
		// hasn't been initialized properly

		for(int i = 0; i < length; i++)
		{
			trans.transform(triangles[i][0]);
			trans.transform(triangles[i][1]);
			trans.transform(triangles[i][2]);
		}
	}
	
	@Override
	public TriangleList clone()
	{
		return new TriangleList(triangles.clone(), normals.clone());
	}
}
