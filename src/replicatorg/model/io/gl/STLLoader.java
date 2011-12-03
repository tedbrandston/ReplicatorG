/*****************************************************************************
 * STLLoader.java
 * Java Source
 *
 * This source is licensed under the GNU LGPL v2.1.
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information.
 *
 * Copyright (c) 2001, 2002 Dipl. Ing. P. Szawlowski
 * University of Vienna, Dept. of Medical Computer Sciences
 * 
 * Modified by Ted Brandston (ted at makerbot dot com)
 ****************************************************************************/

package replicatorg.model.io.gl;

// Local imports
import java.awt.Component;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.media.opengl.GL2;
import javax.media.opengl.GLContext;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.j3d.loaders.stl.STLFileReader;

import replicatorg.model.gl.Scene;
import replicatorg.model.gl.TriangleDisplayList;
import replicatorg.model.gl.TriangleList;

import com.sun.j3d.loaders.IncorrectFormatException;
import com.sun.j3d.loaders.ParsingErrorException;

public class STLLoader implements Loader
{
    private final Component parentComponent;
    private boolean         showProgress = false;

    /**
     * Creates a STLLoader object.
     */
    public STLLoader( )
    {
        parentComponent = null;
    }
    
    public STLLoader( final Component parentComponent )
    {
        this.parentComponent = parentComponent;
        showProgress = true;
    }
    
    @Override
	public Scene load(String fileName) throws FileNotFoundException,
    IncorrectFormatException, ParsingErrorException
    {
        try
        {
            return load(new File(fileName).toURI().toURL());
        }
        catch(MalformedURLException e)
        {
            throw new FileNotFoundException();
        }
    }

    @Override
	public Scene load(URL url) throws FileNotFoundException,
    IncorrectFormatException, ParsingErrorException
    {
        STLFileReader reader = null;
        try
        {
            if(showProgress)
            {
                reader = new STLFileReader(url, parentComponent);
            }
            else
            {
                reader = new STLFileReader(url);
            }
            Scene s = loadShapes(reader);
            s.setName(url.getFile());
            return s;
        }
        catch( InterruptedIOException ie)
        {
            // user cancelled loading
            return null;
        }
        catch(IOException e)
        {
            throw new IncorrectFormatException(e.toString());
        }
    }
    
	public static Scene loadShapes(final STLFileReader reader/*, final GLContext destContext*/)
	{
		try
		{
			// The number of objects in the stl
			final int numObjects = reader.getNumOfObjects();
			// The number of facets for each object 
            final int[] numFacets = reader.getNumOfFacets();
            // The names of the objects
            final String[] objNames = reader.getObjectNames();
            
            TriangleDisplayList[] objects = new TriangleDisplayList[numObjects];
            
            for(int i = 0; i < numObjects; i++)
            {
	            final Point3d[] normals = new Point3d[numFacets[i]];
	            final Point3d[][] triangles = new Point3d[numFacets[i]][3];
                for(int j = 0; j < numFacets[i]; j++)
                {
                	double[] norm = new double[3];
                	double[][] facet = new double[3][3];
                	if(reader.getNextFacet(norm, facet))
                    {
                		triangles[j][0] = new Point3d(facet[0]);
                		triangles[j][1] = new Point3d(facet[1]);
                		triangles[j][2] = new Point3d(facet[2]);
                		
                        if (norm[0] == 0 && 
                        	norm[1] == 0 &&
                        	norm[2] == 0)
                        {
                        	// Calculate normal
                        	Vector3d v0 = new Vector3d(triangles[j][0]);
                        	v0.negate();
                        	Vector3d v1 = new Vector3d(triangles[j][1]);
                        	v1.add(v0);
                        	Vector3d v2 = new Vector3d(triangles[j][2]);
                        	v2.add(v0);
                        	Vector3d n = new Vector3d();
                        	n.cross(v1,v2);
                        	n.normalize();
                        	normals[j] = new Point3d(n.x, n.y, n.z);
                        }
                        else
                    	{
                        	//normal has been provided
                        	normals[j] = new Point3d(norm);
                    	}
                    }
                    else // failure when reading
                    {
                        throw new ParsingErrorException("STLLoader.loadShapes:"
                        		+ " reader failed to getNextFacet");
                    }
                }
                TriangleList tl = new TriangleList(triangles, normals);
                objects[i] = new TriangleDisplayList(tl); 
                objects[i].setName(objNames[i]);
            }
            return new Scene(objects);
		}
        catch(InterruptedIOException ie)
        {
            // user cancelled loading
            return null;
        }
        catch(IOException e)
        {
            throw new ParsingErrorException(e.toString());
        }
		finally
		{
            try
            {
                reader.close();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
		}
	}
}