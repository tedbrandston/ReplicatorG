package replicatorg.modelgl;

import java.util.LinkedList;

import javax.media.opengl.GL2;
import javax.media.opengl.GLContext;

/**
 * A class for holding an opengl display list and sublists, along with any
 * necessary meta data.
 */
public class ShapeList {

	private int listNum;
	private final LinkedList<ShapeList> sublists = new LinkedList<ShapeList>();
	
	private String name;
	
	// We use this context to make sure all our display lists are on the same 
	// GL pipeline
	private GLContext context;
	
	private boolean recording;
	
	/**
	 * Constructs a new ShapeList in the given GLContext referring to a display
	 * list with the given number
	 * 
	 * If listNum is -1 ShapeList will generate its own list number 
	 * @param context The GLContext on which the display list 
	 * 		  this represents resides
	 * @param listNum The number of the aforementioned display list or -1
	 */
	public ShapeList(GLContext context, int listNum)
	{
		this.context = context;
		if(listNum == -1)
		{
			context.makeCurrent();
			GL2 gl = context.getGL().getGL2();
			this.listNum = gl.glGenLists(1);
		}
		else
		{
			this.listNum = listNum;
		}
	}
	/**
	 * Starts recording a new sublist of this list. This is, basically, a 
	 * wrapper for glGenLists(1) and glNewList();
	 * 
	 * Note that storing sublists replaces any other shape data that may have
	 * been in this ShapeList (at least until I come up with a better system). 
	 * @return The new ShapeList currently being recorded
	 */
	public ShapeList beginSublist(String name)
	{
		if(recording)	//TODO: do we want something stronger to happen here?
			return null;// e.g. throwing an exception?
		
		context.makeCurrent();
		GL2 gl = context.getGL().getGL2();
		
		int sublistNum = gl.glGenLists(1);
		ShapeList newSublist = new ShapeList(context, sublistNum);
		
		sublists.add(newSublist);
		
		gl.glNewList(sublistNum, GL2.GL_COMPILE);
		
		return newSublist;
	}
	
	/**
	 * Finish recording a sublist and add it to this list 
	 * @return The new ShapeList
	 */
	public ShapeList endSublist()
	{
		if(!recording)	//TODO: do we want something stronger to happen here?
			return null;// e.g. throwing an exception?
		
		context.makeCurrent();
		GL2 gl = context.getGL().getGL2();
		
		gl.glEndList();
		
		rebuild();
		
		recording = false;
		
		return sublists.getLast();
	}
	
	/**
	 * 
	 * @return true if we had been recording a new ShapeList, false otherwise
	 */
	public boolean cancelSublist()
	{
		if(!recording)
			return false;
		
		context.makeCurrent();
		GL2 gl = context.getGL().getGL2();
		
		gl.glEndList();
		
		sublists.removeLast();
		
		recording = false;
		
		return true;
	}
	
	/**
	 * Draws this list to its GLContext, wraps glCallList
	 */
	public void callList()
	{
		context.makeCurrent();
		GL2 gl = context.getGL().getGL2();
		gl.glCallList(listNum);
	}
	
	/**
	 * reconstruct our list to call all our sublists
	 */
	private void rebuild()
	{
		context.makeCurrent();
		GL2 gl = context.getGL().getGL2();
		
		gl.glNewList(listNum, GL2.GL_COMPILE);
		for(ShapeList sl : sublists)
			sl.callList();
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
	
	/**
	 * Remove this display list and, if recurse is true, all sublists
	 * Wraps glDeleteLists()
	 * @param recurse
	 */
	public void delete(boolean recurse)
	{
		context.makeCurrent();
		GL2 gl = context.getGL().getGL2();
		
		gl.glDeleteLists(listNum, 1);
		
		if(recurse)
		{
			for(ShapeList sl : sublists)
				sl.delete(true);
		}
	}
}
