package replicatorg.model.gl;

import javax.vecmath.Point3d;

public class AABB {
	public Point3d lower, upper;
	
	/** default constructor */
	public AABB(){}
	
	/** copy constructor */
	public AABB(AABB copy)
	{
		this.lower = copy.lower;
		this.upper = copy.upper;
	}
	
	/** after incorporating a point the bounding box will enclose both the
	 * original bounding box and the new point
	 * @param p The Point3d to add to the bounding box
	 */
	public void incorporate(Point3d p)
	{
		if(lower == null && upper == null)
		{
			lower = p;
			upper = p;
			return;
		}
		if(p.x < lower.x) lower.x = p.x;
		if(p.y < lower.y) lower.y = p.y;
		if(p.z < lower.z) lower.z = p.z;
		if(p.x > upper.x) upper.x = p.x;
		if(p.y > upper.y) upper.y = p.y;
		if(p.z > upper.z) upper.z = p.z;
	}
	
	public void incorporate(AABB box)
	{
		incorporate(box.upper);
		incorporate(box.lower);
	}
	
}