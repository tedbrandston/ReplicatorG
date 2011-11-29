/*
 Part of the ReplicatorG project - http://www.replicat.org
 Copyright (c) 2008 Zach Smith

 Forked from Arduino: http://www.arduino.cc

 Based on Processing http://www.processing.org
 Copyright (c) 2004-05 Ben Fry and Casey Reas
 Copyright (c) 2001-04 Massachusetts Institute of Technology

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package replicatorg.model;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;

import replicatorg.app.Base;
import replicatorg.app.syntax.SyntaxDocument;

public class SmartBuildCode extends BuildCode {
	
	/** json metadata file of transforms, and origional import filename */	
	public File metadataFile; 
	public String previousMD5;
	
	public SmartBuildCode(String name, File file)  {
		super(name, file);
		this.metadataFile = fileForMetadata(file);
		try {
			load();
		} catch (IOException e) {
			Base.logger.severe("error while loading code " + name);
		}
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
	
//	/**
//	 * Load this piece of code from a file.
//	 */
//	public void load() throws IOException {
//		if (file == null) {
//			program = "";
//			setModified(true);
//		} else {
//			program = Base.loadFile(file);
//			setModified(false);
//		}
//	}

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
		} 	catch(java.security.NoSuchAlgorithmException e) {
				//throw new RuntimeException("Unable to close input stream for MD5 calculation", e);
				System.out.println(e);
		}
		return "";
	}
	
	/**
	 * Save this piece of code, regardless of whether the modified flag is set
	 * or not.
	 */
	public void save() throws IOException {
		// TODO re-enable history
		// history.record(s, SketchHistory.SAVE);
		this.previousMD5 = md5FromFile(file);
		Base.saveFile(program, file);
		setModified(false);
		String newMd5 = md5FromFile(file);

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

//		String newMd5 = md5(file);		
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
//
//	/**
//	 * Save this file to another location, used by Sketch.saveAs()
//	 */
//	public void saveAs(File newFile) throws IOException {
//		Base.saveFile(program, newFile);
//		file = newFile;
//		name = file.getName();
//		// we're still truncating the suffix, for now.
//		int lastIdx = name.lastIndexOf('.');
//		if (lastIdx > 0) {
//			name = name.substring(0, lastIdx);
//		}
//		setModified(false);
//	}
//
//	public int compareTo(SmartBuildCode other) {
//		if (name == null) { return (other.name == null)?0:-1; }
//		return name.compareTo(other.name);
//	}
//
//	public Type getType() {
//		return BuildElement.Type.GCODE;
//	}
//
//	@Override
//	void writeToStream(OutputStream ostream) {
//		// TODO Auto-generated method stub
//		
//	}
}
