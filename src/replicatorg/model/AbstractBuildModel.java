package replicatorg.model;

import java.io.File;
import java.io.IOException;

import replicatorg.app.Base;

public abstract class AbstractBuildModel extends BuildElement {

	protected File file;

	public AbstractBuildModel() {
		super();
	}

	public BuildElement.Type getType() {
		return BuildElement.Type.MODEL;
	}

	public String getPath() {
		try {
			return file.getCanonicalPath();
		} catch (IOException ioe) { return null; }
	}

	private String getFileExtension(File file) {
		int dotExtension = (file.getName()).lastIndexOf('.');
		
		if (dotExtension == -1) {
			return "";
		}
		
		return (file.getName()).substring(dotExtension+1).toLowerCase();
	}

	private String getFileBase(File file) {
		int dotExtension = (file.getName()).lastIndexOf('.');
		
		
		if (dotExtension == -1) {
			return file.getName();
		}
		
		return (file.getName()).substring(0,dotExtension);
	}

	public void save() {
	
		// If we already have a gcode or stl file, just save it.
		if (getFileExtension(file).equals("gcode")
				|| getFileExtension(file).equals("stl")) {
			saveInternal(file);
		}
		else {
			// Otherwise, assume we have a non-stl model file, and save it out to an stl instead.
			String newFileName = file.getParent() + File.separatorChar + getFileBase(file) + ".stl";
			
			Base.logger.info("Exporting modified model as .stl file: " + newFileName);
			
			File newFile = new File(newFileName);
			
			if (saveInternal(newFile)) {
				file = newFile;
			}
		}
	}

	public void saveAs(File f) {
		if (saveInternal(f)) {
			file = f;
		}
	}
	
	protected abstract boolean saveInternal(File f);

}