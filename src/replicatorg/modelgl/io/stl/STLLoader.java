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

package replicatorg.modelgl.io.stl;

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
import javax.vecmath.Vector3d;

import org.j3d.loaders.stl.STLFileReader;

import replicatorg.modelgl.Scene;
import replicatorg.modelgl.TriangleDisplayList;
import replicatorg.modelgl.TriangleList;
import replicatorg.modelgl.io.Loader;

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
            
            // Our GL to cry on 
            final GL2 gl = GLContext.getCurrentGL().getGL2();
            
            for(int i = 0; i < numObjects; i++)
            {
	            final double[][] normals = new double[numFacets[i]][3];
	            final double[][][] triangles = new double[numFacets[i]][3][3];
                for(int j = 0; j < numFacets[i]; j++)
                {
                	if(reader.getNextFacet(normals[j], triangles[j]))
                    {
                        if (normals[j][0] == 0 && 
                        	normals[j][1] == 0 &&
                        	normals[j][2] == 0)
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
                        	normals[j][0] = n.x;
                        	normals[j][1] = n.y;
                        	normals[j][2] = n.z;
                        }
                        // else normal has been provided
                    }
                    else // failure when reading
                    {
                        throw new ParsingErrorException("STLLoader.loadShapes:"
                        		+ " reader failed to getNextFacet");
                    }
                }
                TriangleList tl = new TriangleList(triangles, normals);
                objects[i] = new TriangleDisplayList(tl); 
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