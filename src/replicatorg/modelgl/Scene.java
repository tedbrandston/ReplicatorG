package replicatorg.modelgl;

import java.util.ArrayList;
import java.util.List;

/**
 * @author thbrandston
 *
 */
public class Scene /*implements Shape*/{

	private List<TriangleDisplayList> shapes;
	
	public Scene(TriangleDisplayList[] shapes)
	{
		this.shapes = new ArrayList<TriangleDisplayList>();
		for(TriangleDisplayList s : shapes)
			this.shapes.add(s);
	}
	
	public void add(TriangleDisplayList s)
	{
		
	}
	
	public void setName(String s)
	{
		
	}
	
	public String getName()
	{
		return "";
	}
	
	public TriangleList getAllTheTriangles()
	{
		int length = 0;
		for(TriangleDisplayList s : shapes)
		{
			length += s.getTriangles().length();
		}
		
		double[][][] triangles = new double[length][3][3];
		double[][] normals = new double[length][3];
		int i = 0;

		for(TriangleDisplayList s : shapes)
		{
			double[][][] loadTriangles = s.getTriangles().getTriangleArray();
			double[][] loadNormals = s.getTriangles().getNormalArray();
			
			for(int j = 0; j < loadTriangles.length; j++)
			{
				triangles[i] = loadTriangles[j];
				normals[i] = loadNormals[j];
				i++;
			}
		}
		
		return new TriangleList(triangles, normals);
	}
	
	public void compile()
	{
		
		return;
	}
}
