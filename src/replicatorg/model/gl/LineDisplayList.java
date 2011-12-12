package replicatorg.model.gl;

import javax.media.opengl.GLContext;
import javax.vecmath.Point3d;

public class LineDisplayList extends Shape {

	private Point3d[] lines;
	
	public LineDisplayList(Point3d[] lines, GLContext context, int listNum) {
		super(context, listNum);
		this.lines = lines;
	}

	public LineDisplayList(Point3d[] lines) {
		this(lines, null, -1);
	}

	@Override
	public void compile() {
		// TODO Auto-generated method stub

	}

	@Override
	public void applyTransform() {
		// TODO Auto-generated method stub

	}

	@Override
	public Shape clone() {
		// TODO Auto-generated method stub
		return null;
	}

}
