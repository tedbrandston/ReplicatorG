package replicatorg.model.j3d;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import javax.vecmath.Matrix4d;

import org.j3d.renderer.java3d.loaders.ColladaLoader;
import org.j3d.renderer.java3d.loaders.ObjLoader;
import org.j3d.renderer.java3d.loaders.STLLoader;

import replicatorg.app.Base;
import replicatorg.app.ui.modeling.j3d.EditingModel;
import replicatorg.model.AbstractBuildModel;
import replicatorg.model.Build;
import replicatorg.model.io.j3d.StlAsciiWriter;

import com.sun.j3d.loaders.Loader;
import com.sun.j3d.loaders.Scene;

public class BuildModel extends AbstractBuildModel {

	private Transform3D transform = new Transform3D(); 
	private Shape3D shape = null;
	private EditingModel editListener = null;
	
	public void setEditListener(EditingModel eModel) {
		editListener = eModel;
	}
	
	public BuildModel(Build build, File file) {
		this.file = file;
	}		

	public Shape3D getShape() {
		if (shape == null) { 
			loadShape();
		}
//		loadShape();
		return shape;
	}

	// Attempt to load the file with the given loader.  Should return
	// null if the given loader can't identify the file as being of
	// the correct type.
	private Shape3D loadShape(Loader loader) {
		Scene scene = null;
		try {
			scene = loader.load(file.getCanonicalPath());
		} catch (Exception e) {
			Base.logger.log(Level.INFO,
					"Could not load "+file.getPath()+
					" with "+ loader.getClass().getSimpleName(),e);
			return null;
		}
		if (scene == null) { return null; }
		return (Shape3D)scene.getSceneGroup().getChild(0);
	}

	Map<String,Loader> loaderExtensionMap = new HashMap<String,Loader>();
	{
		loaderExtensionMap.put("stl",new STLLoader());
		loaderExtensionMap.put("obj",new ObjLoader());
		loaderExtensionMap.put("dae",new ColladaLoader());
	}
	
	private void loadShape() {
		String suffix = null;
		String name = file.getName();
		int idx = name.lastIndexOf('.');
		if (idx > 0) {
			suffix = name.substring(idx+1);
		}
		// Attempt to find loader based on suffix
		Shape3D candidate = null; 
		if (suffix != null) {
			Loader loadCandidate = loaderExtensionMap.get(suffix.toLowerCase());
			if (loadCandidate != null) {
				candidate = loadShape(loadCandidate);
			}
		}
		// Couldn't find loader for suffix or file is corrupt or of wrong type
		if (candidate == null) {
			for (Loader loadCandidate : loaderExtensionMap.values()) {
				candidate = loadShape(loadCandidate);
				if (candidate != null) { break; }
			}
		}
		if (candidate != null) {
			shape = candidate;
		}
	}

	@Override
	public Matrix4d getTransform() {
		Matrix4d result = new Matrix4d();
		transform.get(result);
		return result;
	}
	
	class UndoEntry implements UndoableEdit {
		Transform3D before;
		Transform3D after;
		String description;
		boolean newOp;
		
		// The newOp flag is set at the start of every drag or every button operation.  NewOps will never
		// be merged into the undo op at the top of the stack.
		public UndoEntry(Transform3D before, Transform3D after, String description, boolean newOp) {
			this.before = new Transform3D(before);
			this.after= new Transform3D(after);
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
		
	public void setTransform(Transform3D t, String description, boolean newOp) {
		if (transform.equals(t)) return;
		undo.addEdit(new UndoEntry(transform,t,description, newOp));
		transform.set(t);
		setModified(true);
		if (editListener != null) {
			editListener.modelTransformChanged();
		}
	}

	public void doEdit(Transform3D edit) {
		transform.set(edit);
		setModified(undo.canUndo());
		editListener.modelTransformChanged();
	}
	
	@Override
	protected boolean saveInternal(File f) {
		try {
			FileOutputStream ostream = new FileOutputStream(f);
			Base.logger.info("Writing to "+f.getCanonicalPath()+".");
			StlAsciiWriter saw = new StlAsciiWriter(ostream);
			saw.writeShape(getShape(), transform);
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
	protected void writeToStream(OutputStream ostream) {
		// TODO Auto-generated method stub
		
	}
}