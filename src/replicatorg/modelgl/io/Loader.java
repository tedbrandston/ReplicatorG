package replicatorg.modelgl.io;

import java.io.FileNotFoundException;
import java.net.URL;

import replicatorg.modelgl.Scene;

import com.sun.j3d.loaders.IncorrectFormatException;
import com.sun.j3d.loaders.ParsingErrorException;

public interface Loader {

	public abstract Scene load(String fileName) throws FileNotFoundException,
			IncorrectFormatException, ParsingErrorException;

	public abstract Scene load(URL url) throws FileNotFoundException,
			IncorrectFormatException, ParsingErrorException;

}