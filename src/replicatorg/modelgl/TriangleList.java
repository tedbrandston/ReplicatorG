package replicatorg.modelgl;

import javax.vecmath.Matrix4d;

public final class TriangleList {

	private final double[][][] triangles;
	private final double[][] normals;
	private final int length;
	
	public TriangleList(double[][][] triangles, double[][] normals)
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
			
			if( triangles[i][0].length != 3 ||
				triangles[i][1].length != 3 ||
				triangles[i][2].length != 3 ||
				normals[i].length != 3) 
			{
				this.triangles = null;
				this.normals = null;
				throw new IllegalArgumentException(
						"Each point needs exactly three dimensions.");
			}
		}
		//done verifying!
		
		this.triangles = triangles;
		this.normals = normals;
	}
	
	public double[][][] getTriangleArray()
	{
		return triangles;
	}

	public double[][] getNormalArray()
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
		throw new UnsupportedOperationException();
	}
}
