package replicatorg.modelgl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import javax.vecmath.Matrix4d;

import replicatorg.app.Base;
import replicatorg.app.ui.modeling.EditingModel;
import replicatorg.model.Build;
import replicatorg.model.j3d.StlAsciiWriter;
import replicatorg.modelgl.io.Loader;
import replicatorg.modelgl.io.stl.STLLoader;


public class BuildModel extends BuildElement {

	private File file;
	private Matrix4d transform = new Matrix4d();
	private int[] shapes = null;						//GL: this can become a display list (int)
	private EditingModel editListener = null;
	
	public void setEditListener(EditingModel eModel) {
		editListener = eModel;
	}
	
	BuildModel(Build build, File file) {
		this.file = file;
	}		

	public BuildElement.Type getType() {
		return BuildElement.Type.MODEL;
	}

	public String getPath() {
		try {
			return file.getCanonicalPath();
		} catch (IOException ioe) { return null; }
	}

	public int[] getShapes() {
		if (shapes == null) { 
			loadShapes();
		}

		return shapes;
	}

	// Attempt to load the file with the given loader.  Should return
	// null if the given loader can't identify the file as being of
	// the correct type.
	private int[] loadShapes(Loader loader) {
		int[] loaded = null;
		try {
			loaded = loader.load(file.getCanonicalPath());
		} catch (Exception e) {
			Base.logger.log(Level.INFO,
					"Could not load "+file.getPath()+
					" with "+ loader.getClass().getSimpleName(),e);
			return null;
		}
		
		return loaded;
	}

	Map<String,Loader> loaderExtensionMap = new HashMap<String,Loader>();
	{
		loaderExtensionMap.put("stl",new STLLoader());
//		loaderExtensionMap.put("obj",new ObjLoader());
//		loaderExtensionMap.put("dae",new ColladaLoader());
	}
	
	private void loadShapes() {
		String suffix = null;
		String name = file.getName();
		int idx = name.lastIndexOf('.');
		if (idx > 0) {
			suffix = name.substring(idx+1);
		}
		// Attempt to find loader based on suffix
		int[] candidate = null; 
		if (suffix != null) {
			Loader loadCandidate = loaderExtensionMap.get(suffix.toLowerCase());
			if (loadCandidate != null) {
				candidate = loadShapes(loadCandidate);
			}
		}
		// Couldn't find loader for suffix or file is corrupt or of wrong type
		if (candidate == null) {
			for (Loader loadCandidate : loaderExtensionMap.values()) {
				candidate = loadShapes(loadCandidate);
				
				if (candidate != null)
					break;
			}
		}
		if (candidate != null) {
			shapes = candidate;
		}
	}

	public Matrix4d getTransform() {
		return transform;
	}
	
	class UndoEntry implements UndoableEdit {
		Matrix4d before;
		Matrix4d after;
		String description;
		boolean newOp;
		
		// The newOp flag is set at the start of every drag or button operation.
		// NewOps will never be merged into the undo op at the top of the stack.
		public UndoEntry(final Matrix4d before, final Matrix4d after,
				String description, boolean newOp) {
			this.before = new Matrix4d(before);
			this.after= new Matrix4d(after);
			this.description = description;
			this.newOp = newOp;
		}
		
		public boolean addEdit(UndoableEdit edit) {
			if (edit instanceof UndoEntry) {
				UndoEntry ue = (UndoEntry)edit;
				if (!ue.newOp && description == ue.description) {
					after = ue.after;
					return true;
				}
			}
			return false;
		}
		
		public boolean canRedo() {
			return true;
		}
		public boolean canUndo() {
			return true;
		}
		public void die() {
			
		}
		public String getPresentationName() {
			return description;
		}
		public String getRedoPresentationName() {
			return "Redo "+getPresentationName();
		}
		public String getUndoPresentationName() {
			return "Undo "+getPresentationName();
		}
		public boolean isSignificant() {
			return true;
		}
		public void redo() throws CannotRedoException {
			doEdit(after);
		}
		public boolean replaceEdit(UndoableEdit edit) {
			return false;
		}
		public void undo() throws CannotUndoException {
			doEdit(before);
		}
	}
		
	public void setTransform(Matrix4d t, String description, boolean newOp) {
		if (transform.equals(t)) return;
		undo.addEdit(new UndoEntry(transform,t,description, newOp));
		transform.set(t);
		setModified(true);
		if (editListener != null) {
			editListener.modelTransformChanged();
		}
	}

	public void doEdit(Matrix4d edit) {
		transform.set(edit);
		setModified(undo.canUndo());
		editListener.modelTransformChanged();
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

	private boolean saveInternal(File f) {
		try {
			FileOutputStream ostream = new FileOutputStream(f);
			Base.logger.info("Writing to "+f.getCanonicalPath()+".");
			StlAsciiWriter saw = new StlAsciiWriter(ostream);
			saw.writeShape(getShapes(), getTransform());
			ostream.close();
			undo = new UndoManager();
			setModified(false);
			return true;
		} catch (FileNotFoundException fnfe) {
			Base.logger.log(Level.SEVERE,"Error during save",fnfe);
		} catch (IOException ioe) {
			Base.logger.log(Level.SEVERE,"Error during save",ioe);
		}
		return false;
	}

	@Override
	void writeToStream(OutputStream ostream) {
		// TODO Auto-generated method stub
		
	}
}
