package replicatorg.modelgl.io;

import java.io.FileNotFoundException;
import java.net.URL;

import org.j3d.loaders.stl.STLFileReader;

import com.sun.j3d.loaders.IncorrectFormatException;
import com.sun.j3d.loaders.ParsingErrorException;

public interface Loader {

	public abstract int[] load(String fileName) throws FileNotFoundException,
			IncorrectFormatException, ParsingErrorException;

	public abstract int[] load(URL url) throws FileNotFoundException,
			IncorrectFormatException, ParsingErrorException;

}