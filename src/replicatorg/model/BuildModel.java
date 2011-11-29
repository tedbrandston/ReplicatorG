package replicatorg.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import org.j3d.renderer.java3d.loaders.ColladaLoader;
import org.j3d.renderer.java3d.loaders.ObjLoader;
import org.j3d.renderer.java3d.loaders.STLLoader;

import replicatorg.app.Base;
import replicatorg.app.ui.modeling.EditingModel;
import replicatorg.model.j3d.StlAsciiWriter;

import com.sun.j3d.loaders.Loader;
import com.sun.j3d.loaders.Scene;

public class BuildModel extends BuildElement {

	private File file; /// 3d model File
	private Transform3D transform = new Transform3D();
	private Shape3D shape = null;
	private EditingModel editListener = null;
	
	public File metadataFile; 
	public String previousMD5;
	public Deque<String> xformStack = new ArrayDeque<String>();
	
	public void setEditListener(EditingModel eModel) {
		editListener = eModel;
	}
	
	BuildModel(Build build, File file) {
		this.file = file;
		this.metadataFile = fileForMetadata(file);
	}		

	public File fileForMetadata(File baseFile)
	{
		String basePath = baseFile.getParent();
		String baseFilename = baseFile.getName();
		long lastMod = baseFile.lastModified();
		String assumedMetadata = basePath + "/." + Long.toString(lastMod) + "." + baseFilename + ".json";
		System.out.println(assumedMetadata);
		Base.logger.severe("looking or metadata at " + assumedMetadata);

		File md = new File(assumedMetadata);
		if(md.exists() ) {
			Base.logger.severe("we have previous edits at" + assumedMetadata);
			return md;
		}
		Base.logger.severe("we DO NOT have previous edits at" + assumedMetadata);
		return null;
	}

	public BuildElement.Type getType() {
		return BuildElement.Type.MODEL;
	}
	
	
	public String md5FromFile(File file)
	{
		try { 
			MessageDigest digest = MessageDigest.getInstance("MD5");
			byte[] buffer = new byte[8192];
			int read = 0;
			InputStream is;
			try {
				is = new FileInputStream(file);
			} catch(java.io.FileNotFoundException e) {
					System.out.println(e);
					return "";
			}
			try {
				while( (read = is.read(buffer)) > 0) {
					digest.update(buffer, 0, read);
				}		
				byte[] md5sum = digest.digest();
				BigInteger bigInt = new BigInteger(1, md5sum);
				String output = bigInt.toString(16);
				System.out.println("MD5: " + output);
				return output;
			}
			catch(IOException e) {
				//throw new RuntimeException("Unable to process file for MD5", e);
				return "";
			}
			finally {
				try {
					is.close();
				}
				catch(IOException e) {
					//throw new RuntimeException("Unable to close input stream for MD5 calculation", e);
					return "";
				}
			}
		} catch(java.security.NoSuchAlgorithmException e) {
			//throw new RuntimeException("Unable to close input stream for MD5 calculation", e);
			System.out.println(e);
		}
		return "";
	}


	public String getPath() {
		try {
			return file.getCanonicalPath();
		} catch (IOException ioe) { return null; }
	}

	public Shape3D getShape() {
		if (shape == null) { 
			loadShape();
		}
		return shape;
	}

	/** Attempt to load the file with the given loader.  Should return null if the given 
	 * loader can't identify the file as being of the correct type.
	 * @param loader
	 * @return
	 */
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
		if (candidate != null) { shape = candidate; }
	}

	public Transform3D getTransform() { return transform; }
	
	/** 
	 * This class is used for Undo/Redo operations for 3d manipulaton
	 * @author 
	 */
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
			redoEdit(after);
		}
		public boolean replaceEdit(UndoableEdit edit) {
			return false;
		}
		public void undo() throws CannotUndoException {
			undoEdit(before);
		}
	}
		
	public void setTransform(Transform3D t, String description, boolean newOp) {
		//xform.push("foo");
		this.xformStack.push(description);
		System.out.println("XXX spush etTransform:" + description);
		System.out.println("XXX setTransform 2: " + t.getType());		
		if (transform.equals(t)) return;
		undo.addEdit(new UndoEntry(transform,t,description, newOp));
		this.doEdit(t, true );
//		transform.set(t);
//		setModified(true);
//		if (editListener != null) {
//			editListener.modelTransformChanged();
//		}
	}

	public void redoEdit(Transform3D edit)
	{
		String x = "redo of ???";
		xformStack.push(x);
		System.out.println("XXX push? doEdit:" + undo.getUndoOrRedoPresentationName() );
		doEdit(edit, undo.canUndo());
	}
	
	public void undoEdit(Transform3D edit) {
		System.out.println("XXX pop? doEdit:" + undo.getUndoOrRedoPresentationName() );
		String x = xformStack.pop();
		doEdit(edit, undo.canUndo());
	}
	
	public void doEdit(Transform3D t, boolean isModified) {
		transform.set(t);
		setModified( isModified); //undo.canUndo());
		if (editListener != null) {
			editListener.modelTransformChanged();
		}
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

		/// cache xform infom. 		undo.addEdit(new UndoEntry(transform,t,description, newOp));
		String xforms = "";
		System.out.println("**** undo testing");
//		for( UndoableEdit e : undo )
//		{
//			System.out.println(e.getPresentationName());
//			System.out.println(e );
//		}
		System.out.println("**** undo testing");

		try {
			previousMD5 = md5FromFile(f);
			FileOutputStream ostream = new FileOutputStream(f);
			Base.logger.info("Writing to "+f.getCanonicalPath()+".");
			StlAsciiWriter saw = new StlAsciiWriter(ostream);
			saw.writeShape(getShape(), getTransform());
			ostream.close();
			undo = new UndoManager();
			setModified(false);
			
			String newMd5 = md5FromFile(f);
			if(metadataFile == null)
			{
				Base.logger.severe("we DO NOT have previous edits");
				String editsJson = "{ \"first_seen_md5\":\"" + previousMD5 + "\", \"first_seen_name\"=\"" + file.getName() + "\" }";
				String basePath = file.getParent();
				String baseFilename = file.getName();
				long newMod= file.lastModified();
				String assumedMetadata = basePath + "/." + Long.toString(newMod) + "." + baseFilename + ".json";
				Base.logger.severe("saving new fake previous edits to " + assumedMetadata);
				File mdFile = new File(assumedMetadata);
				Base.saveFile(editsJson, mdFile);
				this.metadataFile = mdFile;
			}
			else { //metadataFile != null
				this.updateMetadata(previousMD5, newMd5);
				//appendLatestTransforms.
			}


			return true;
		} catch (FileNotFoundException fnfe) {
			Base.logger.log(Level.SEVERE,"Error during save",fnfe);
		} catch (IOException ioe) {
			Base.logger.log(Level.SEVERE,"Error during save",ioe);
		}
		return false;
	}

	public void updateMetadata(String prevMD5, String newMd5)
	{
		String basePath = file.getParent();
		String baseFilename = file.getName();
		long newMod = file.lastModified();
		File mdFile = null;
		String assumedMetadata = basePath + "/." + Long.toString(newMod) + "." + baseFilename + ".json";
		try { 
			String previousData = Base.loadFile(metadataFile);
			String newMetadata = previousData + 
					"{ \"old_md5\":\"" + prevMD5 + "\", \"new_md5\":\"" + newMd5 +"\" \"xforms\"=[]}";
			mdFile = new File(assumedMetadata);
			Base.saveFile(newMetadata, mdFile);
		} catch ( java.io.IOException e)
		{
			mdFile = null;
			System.out.println(e);
			return;
		}
		if (mdFile != null) {
			metadataFile.delete();
			metadataFile = mdFile;
		}
	}

	
	@Override
	void writeToStream(OutputStream ostream) {
		// TODO Auto-generated method stub
		
	}
}
