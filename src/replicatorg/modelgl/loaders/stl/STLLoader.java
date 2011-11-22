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

package replicatorg.modelgl.loaders.stl;

// Local imports
import java.io.IOException;
import java.io.InterruptedIOException;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLContext;
import javax.vecmath.Vector3d;

import org.j3d.loaders.stl.STLFileReader;

import com.sun.j3d.loaders.ParsingErrorException;

public class STLLoader
{
	public static int[] loadShapes(final STLFileReader reader, final GLContext destContext)
	{
		try
		{
			// The number of objects in the stl
			final int numObjects = reader.getNumOfObjects();
			// The number of facets for each object 
            final int[] numFacets = reader.getNumOfFacets();
            // The names of the objects
            final String[] objNames = reader.getObjectNames();
            
            // Our GL to cry on 
            final GL2 gl = destContext.getGL().getGL2();
            destContext.makeCurrent();

            final int listStart = gl.glGenLists(numObjects);
            final int[] displayLists = new int[numObjects];
            
            final double[] normal = new double[3];
            final double[][] vertices = new double[3][3];
            for(int i = 0; i < numObjects; i++)
            {
            	displayLists[i] = listStart + i;
                gl.glNewList(displayLists[i], GL2.GL_COMPILE);

        		gl.glBegin(GL.GL_TRIANGLES);
                for(int j = 0; j < numFacets[i]; j++)
                {
                	if(reader.getNextFacet(normal, vertices))
                    {
                        if (normal[0] == 0 && 
                        	normal[1] == 0 &&
                        	normal[2] == 0)
                        {
                        	// Calculate normal
                        	Vector3d v0 = new Vector3d(vertices[0]);
                        	v0.negate();
                        	Vector3d v1 = new Vector3d(vertices[1]);
                        	v1.add(v0);
                        	Vector3d v2 = new Vector3d(vertices[2]);
                        	v2.add(v0);
                        	Vector3d n = new Vector3d();
                        	n.cross(v1,v2);
                        	n.normalize();
                        	normal[0] = n.x;
                        	normal[1] = n.y;
                        	normal[2] = n.z;
                        }
                    }
                    else
                    {
                        throw new ParsingErrorException( );
                    }
                	
                	// draw triangle to our gl object
                	gl.glNormal3dv(normal, 0);
                	gl.glVertex3dv(vertices[0], 0);
                	gl.glVertex3dv(vertices[1], 0);
                	gl.glVertex3dv(vertices[2], 0);
                }
                gl.glEnd();
                gl.glEndList();
            }
		}
        catch( InterruptedIOException ie )
        {
            // user cancelled loading
            return null;
        }
        catch( IOException e )
        {
            throw new ParsingErrorException( e.toString( ) );
        }
		finally
		{
            try
            {
                reader.close( );
            }
            catch( IOException e )
            {
                e.printStackTrace( );
            }
		}
		return null;
	}
}