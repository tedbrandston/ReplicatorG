package replicatorg.plugin.toolpath.skeinforge;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.PopupMenuEvent;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.app.util.PythonUtils;
import replicatorg.app.util.StreamLoggerThread;
import replicatorg.model.BuildCode;
import replicatorg.plugin.toolpath.ToolpathGenerator;

public abstract class SkeinforgeGenerator extends ToolpathGenerator {

	/**
	 * A ProfileWatcher instance describes an object that wishes to be notified when the selected profile changes.
	 * @author giseburt
	 */
	public interface ProfileWatcher {
		public void profileChanged(Profile newProfile);
	}
	
	abstract public class ProfileKeyWatcher {
		private String subKey = null;
		void ProfileKeyWatcher(String key) {
			subKey = key;
		}
		
		public void profileChanged(Profile newProfile) {
			String key = subKey;

			profileKeyChanged(newProfile.getValueForPlastic(key));
		}
		
		abstract void profileKeyChanged(String value);
	}
	
	boolean configSuccess = false;
	Profile profile = null;
	List <SkeinforgePreference> preferences;
	private TreeMap <String, Profile> profiles = new TreeMap<String, Profile>();
	List <ProfileWatcher> profileWatchers = new LinkedList<ProfileWatcher>();
	
	JComboBox comboBox = null;

	public SkeinforgeGenerator() {
		preferences = getPreferences();
		getSelectedProfile();
	}
	
	// public void setComboBox(JComboBox comboBox) {
	// 	this.comboBox = comboBox;
	// 	comboBox.addPopupMenuListener(new PopupMenuListener() {
	// 		public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
	// 			JComboBox menu = (JComboBox)(e.getSource());
	// 			int index = menu.getSelectedIndex();
	// 			
	// 			if (!profile.isChanged()) {
	// 				if (index > 0 && menu.getItemAt(index-1) instanceof String) {
	// 					menu.removeItemAt(index-1);
	// 				}
	// 			} else {
	// 				if (profile.isChanged()) {
	// 					menu.insertItemAt(profile.getName(), index);
	// 				}
	// 			}
	// 		}
	// 		public void popupMenuCanceled(PopupMenuEvent e) {}
	// 		public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
	// 	});
	// }
	
	public boolean runSanityChecks() {
		String errors = "";
		
		for (SkeinforgePreference preference : preferences) {
			String error = preference.valueSanityCheck();
			if( error != null) {
				errors += error;
			}
		}
		
		if (errors.equals("")) {
			return true;
		}
		
		int result = JOptionPane.showConfirmDialog(null,
				"The following non-optimal profile settings were detected:\n\n"
				+ errors + "\n\n"
				+ "Press OK to attempt to generate profile anyway, or Cancel to go back and correct the settings.",
				"Profile warnings", JOptionPane.OK_CANCEL_OPTION);
		
		return (result == JOptionPane.OK_OPTION);
	}
	
	static public String getSelectedProfileName() {
		String name = Base.preferences.get("replicatorg.skeinforge.profile", "");
		Base.logger.log(Level.FINEST, "Selected profile name: " + name);
		return name;
	}

	public Profile getSelectedProfile() {
		if (profile==null) {
			if (profiles.size() == 0)
				getProfiles();
			profile = profiles.get(getSelectedProfileName());
			notifyProfileWatchers();
		}
		Base.logger.log(Level.FINEST, "Found a profile: " + (profile != null ? profile.getFullPath() : "NULL"));
		return profile;
	}

	public boolean setSelectedProfile(String name) {
		Base.logger.log(Level.FINEST, "setSelectedProfile: " + name);
		Matcher m = Pattern.compile("^(.*) \\[Modified\\]$").matcher(name);
		boolean reset = false;
		if (m.matches()) {
			name = m.group(1);
			Base.logger.log(Level.FINEST, "setSelectedProfile fixed: " + name);
		} else if (profile.isChanged()) {
			int userResponse = JOptionPane.showConfirmDialog(null,
					"Are you sure you wish to discard your changes\nto the profile '" 
					+ profile.getName() + "'?\nThis cannot be undone.",
					"Yes", JOptionPane.YES_NO_OPTION);
			
			if (userResponse == JOptionPane.YES_OPTION) {
				profile.clearOverrides();

				// delete the old [Modified]
				File modifiedPath = new File(getUserProfilesDir(), profile.getNameModified());
				if (modifiedPath.exists())
					ProfileUtils.delete(modifiedPath);

				reset = true;
			} else {
				return false;
			}
		}
		Profile namedProfile = profiles.get(name);
		if (namedProfile != null) {
			Base.preferences.put("replicatorg.skeinforge.profile", name);
			profile = namedProfile;
			notifyProfileWatchers();
		}
		//TODO: Put dialog check in here somewhere
		return true;
	}
	
	public void addProfileWatcher(ProfileWatcher aWatcher) {
		profileWatchers.add(aWatcher);
		aWatcher.profileChanged(profile);
	}
	
	public void removeProfileWatcher(ProfileWatcher aWatcher) {
		profileWatchers.remove(aWatcher);
	}
	
	public void notifyProfileWatchers() {
		if (profile == null)
			return;
		for (ProfileWatcher p : profileWatchers) {
			p.profileChanged(profile);
		}
	}
	
	/**
	 * A Profile describes both a profile and all of it's settings.
	 * @author giseburt
	 */
	public static class Profile implements Comparable<Profile> {
		private File profileFile;
		private Map<String,String> settingMap = new HashMap<String,String>();
		private Map<String,String> overrideMap = new HashMap<String,String>();
		// subProfiles -- such as ABS, PLA, PVA, etc. These have arbitray names, BTW.
		private LinkedList<String> subProfiles = new LinkedList<String>();
		
		public interface ProfileChangedWatcher {
			public void profileIsChanged(Profile profile);
		}
		
		private ArrayList<ProfileChangedWatcher> changeWatchers = new ArrayList<ProfileChangedWatcher>();
		
		// In order to save the profile again, we need to store the headers and key order for each file
		private class ProfileSubfileStore {
			public File file = null;
			public long lastModified = 0L;
			// store the name of the subprofile to easily reconstruct the full key
			public String subprofileName = null;
			public String header = "";
			public LinkedList<String> keyOrder = new LinkedList<String>();
			
			ProfileSubfileStore(File file, String subprofileName) {
				this.file = file;
				this.subprofileName = subprofileName;
			}
			
			public void reset() {
				keyOrder.clear();
				header = "";
			}
			
			public void addToHeader(String lines) {
				header += lines + "\n";
			}
			
			public void addKey(String key) {
				keyOrder.add(key);
			}
			
			public void setLastModified(long lastModified) {
				this.lastModified = lastModified;
			}
		}
		private LinkedList<ProfileSubfileStore> subFileStores = new LinkedList<ProfileSubfileStore>();
		private Map<String,ProfileSubfileStore> moduleModificationMap = new HashMap<String,ProfileSubfileStore>();

		public Profile(String fullPath) {
			this.profileFile = new File(fullPath);
			this.scanProfileFolder(profileFile, (String)null, 0, false);
		}
		
		public void addModifiedProfile(String modifiedPath) {
			this.scanProfileFolder(new File(modifiedPath), (String)null, 0, true);
		}
		
		public void addChangeWatcher(ProfileChangedWatcher pcw) {
			Base.logger.log(Level.FINEST, "addChangeWatcher");
			changeWatchers.add(pcw);
			if (isChanged())
				pcw.profileIsChanged(this);
		}

		public void removeChangeWatcher(ProfileChangedWatcher pcw) {
			changeWatchers.remove(pcw);
		}
		
		public String getFullPath() {
			return profileFile.getPath();
		}

		// Get the name as if it were modified
		public String getNameModified() {
			return profileFile.getName() + " [Modified]";
		}

		public String getName() {
			return profileFile.getName();
		}
		
		public String toString() {
			return profileFile.getName() + (isChanged() ? " [Modified]" : "");
		}
		
		public String getValue(String module, String key) {
			return getValue(module+":"+key);
		}

		public String getValue(String key) {
			// Base.logger.log(Level.FINEST, "Value for key: " + key + " = " + (overrideMap.containsKey(key) ? overrideMap.get(key) : settingMap.get(key)));
			return overrideMap.containsKey(key) ? overrideMap.get(key) : settingMap.get(key);
		}

		public String getValueForPlastic(String key) {
			String value = getValue(key);
			if (value == null) {
				String profilePlastic = getValue("extrusion.csv:Profile Selection:");
				// Base.logger.log(Level.FINEST, "Profile Selection: " + profilePlastic);
				if (profilePlastic != null)
					value = getValue(profilePlastic + "/" + key);
			}
			return value;
		}
		
		public void setValue(String module, String key, String value) {
			setValue(module+":"+key, value);
		}

		// We want to be smart here, so if we go to set it to the current value, don't add a key to overrideMap
		// Likewise, is we are setting it back to what's in the profiles, we remove the key from overrideMap
		public void setValue(String key, String value) {
			boolean wasChanged = isChanged();
			if (settingMap.containsKey(key) && (value == null && settingMap.get(key) == null) || (settingMap.get(key) != null && settingMap.get(key).equals(value))) {
				overrideMap.remove(key);
			} else {
				overrideMap.put(key, value);
			}
			if (isChanged() != wasChanged)
				notifyProfileChangedChanged();
		}
		
		private void notifyProfileChangedChanged() {
			for (ProfileChangedWatcher p : changeWatchers)
				p.profileIsChanged(this);
		}
		
		public void setValueForPlastic(String key, String value) {
			String profilePlastic = getValue("extrusion.csv:Profile Selection:");
			// Base.logger.log(Level.FINEST, "Profile Selection: " + profilePlastic);
			// Base.logger.log(Level.FINEST, "Setting value for key: " + (profilePlastic!=null ? profilePlastic + "/" : "") + key + " == " + value);
			setValue((profilePlastic!=null ? profilePlastic + "/" : "") + key, value);
		}
		
		public boolean isChanged() {
			return (overrideMap.size() > 0);
		}
		
		public void clearOverrides() {
			boolean wasChanged = isChanged();
			overrideMap.clear();
			if (isChanged() != wasChanged)
				notifyProfileChangedChanged();
		}
		
		public boolean equals(Profile o) {
			return profileFile.getName().equals(o.profileFile.getName());
		}

		public int compareTo(Profile o) {
			return profileFile.getName().compareTo(o.profileFile.getName());
		}
		
		public boolean checkForUpdate() {
			subProfiles.clear();
			return scanProfileFolder(profileFile, (String)null, 0, true);
		}
		
		private boolean scanProfileFolder(File basePath, String subprofile, int depth, boolean modified) {
			boolean updated = false;
			for (String subpath : basePath.list()) {
				File subFile = new File(basePath, subpath);
				if (subFile.isDirectory()) {
					if ((depth == 0 && subFile.getName().matches("^(profiles)$")) || (depth == 1 && subFile.getName().matches("^(extrusion)$"))) {
						scanProfileFolder(subFile, (String)null, depth+1, modified);
					} else if (depth == 2) {
						subProfiles.add(subFile.getName());
						scanProfileFolder(subFile, subFile.getName(), depth+1, modified);
					}
				}
				else if (subFile.getName().matches(".*\\.csv$")) {
					ProfileSubfileStore store = null;
					if (moduleModificationMap.containsKey(subprofile+"/"+subFile.getName())) {
						store = moduleModificationMap.get(subprofile+"/"+subFile.getName());
						if (store.lastModified<subFile.lastModified()) {
							store.setLastModified(subFile.lastModified());
							store.reset();
							updated = true;
						} else {
							continue;
						}
					} else {
						store = new ProfileSubfileStore(subFile, subprofile);
						store.setLastModified(subFile.lastModified());
						subFileStores.add(store);
						moduleModificationMap.put(subprofile+"/"+subFile.getName(), store);
						updated = true;
					}
					
					BufferedReader in = null;
					try {
						in = new BufferedReader(new FileReader(subFile));
					} catch (java.io.FileNotFoundException ioe) {
						Base.logger.log(Level.SEVERE, "Couldn't read directory: " + subFile, ioe);
					}
					String line = null;
					int skip = 2;
					while (true) {
						try {
							line = in.readLine();
						} catch (IOException ioe) {
							Base.logger.log(Level.SEVERE, "Couldn't read line from: " + subFile, ioe);
							line = null;
						}
						if (line == null)
							break;
						if (skip-- > 0) {// store the comment and the header to the ProfileSubfileStore
							store.addToHeader(line);
							continue;
						} 
						String[] tokens = line.split("\t");
						String key = (subprofile==null?"":subprofile+"/")+subFile.getName()+":"+tokens[0];
						// Base.logger.log(Level.FINEST, "Loaded value for key: " + key + " = " + (tokens.length>1?tokens[1]:null));
						if (modified)
							setValue(key, (tokens.length>1?tokens[1]:null)); // use the normal override logic
						else
							settingMap.put(key, (tokens.length>1?tokens[1]:null));
						store.addKey(tokens[0]);
					}
				}
			}
			return updated;
		}
		
		public List<String> getSubProfiles() {
			return subProfiles;
		}
		
		public void save(File basePath, String newName) {
			boolean makinCopy = false;
			File newBasePath = null;
			
			if (newName == null)
				newName = toString(); // we use toString's logic to grab the name, possibly including " [Modified]"
				
			if (!newName.equals(profileFile.getName())) {
				/* the copymeister is*/ makinCopy = true;
				
				// copy to new path
				File newProfDir = new File(basePath, newName);
				try {
					Base.copyDir(profileFile, newProfDir);
				} catch (IOException ioe) {
					Base.logger.log(Level.SEVERE,
							"Couldn't copy profile", ioe);
				}
				
				// prepare our newBasePath
				newBasePath = new File(basePath, newName);
				Base.logger.log(Level.FINEST, "newBasePath: " + newBasePath.getPath());
			}
				
			for (ProfileSubfileStore store : subFileStores) {
				File storeFile = store.file;
				if (makinCopy) {
					String newFilePath = newBasePath + storeFile.getPath().substring(profileFile.getPath().length());
					storeFile = new File(newFilePath);
					Base.logger.log(Level.FINEST, "new path: " + storeFile.getPath());
				}
				
				boolean isDirty = false;
				// Since only a few files will likely be effected, we search
				// the overrideMap to see if we need to save this file
				String keyPrefix = (store.subprofileName != null ? store.subprofileName + "/" : "") + storeFile.getName() + ":";
				for (String key : overrideMap.keySet()) {
					if (key.startsWith(keyPrefix)) {
						isDirty = true;
						break;
					}
				}
				
				if (isDirty) {
					try {
						// Base.logger.log(Level.SEVERE, "Saving file: " + storeFile);
						BufferedWriter writer = new BufferedWriter(new FileWriter(storeFile));
						// Base.logger.log(Level.FINEST, "\t" + store.header);
						writer.write(store.header);
						for (String key : store.keyOrder) {
							writer.write(key+"\t");
							String value = getValue(keyPrefix+key);
							writer.write((value == null ? "" : value) + "\n");
							// Base.logger.log(Level.FINEST, keyPrefix+"\t" + key + "==" + (value == null ? "<null>" : value));
						}
						writer.close();
					} catch (IOException ioe) {
						Base.logger.log(Level.SEVERE, "Couldn't write to: " + storeFile, ioe);
					}
					
				}
			}
		}
	}

	void getProfilesIn(File dir) {
		File modifiedPath = null;
		if (dir.exists() && dir.isDirectory()) {
			for (String subpath : dir.list()) {
				File subDir = new File(dir, subpath);
				if (subDir.isDirectory()) {
					String subDirName = subDir.getName();
					Base.logger.log(Level.FINEST, "Profile search: " + subDirName);
					if (subDirName.matches("^(.*) \\[Modified\\]$")) {
						modifiedPath = subDir; // there can be only one
						continue;
					}
					if (profiles.containsKey(subDirName))
						profiles.get(subDirName).checkForUpdate();
					else {
						Base.logger.log(Level.FINEST, "Adding profile: " + subDir.getAbsolutePath());
						profiles.put(subDirName, new Profile(subDir.getAbsolutePath()));
					}
				}
			}
		}
		
		if (modifiedPath != null) {
			Matcher m = Pattern.compile("^(.*) \\[Modified\\]$").matcher(modifiedPath.getName());
			boolean reset = false;
			if (m.matches()) {
				String name = m.group(1);
				if (profiles.containsKey(name)) {
					Base.logger.log(Level.FINEST, "Modified profile: " + modifiedPath.getPath());
					profiles.get(name).addModifiedProfile(modifiedPath.getPath());
				}
			}
		}
	}

	abstract public File getUserProfilesDir();

	Collection<Profile> getProfiles() {
		// Get default installed profiles
		File dir = new File(getSkeinforgeDir(), "prefs");
		getProfilesIn(dir);
		dir = getUserProfilesDir();
		getProfilesIn(dir);
		return profiles.values();
	}
	
	
	
	
	/**
	 * A SkeinforgeOption instance describes a single preference override to pass to skeinforge.
	 * @author phooky
	 */
	protected static class SkeinforgeOption {
		final String parameter;
		final String module;
		final String preference;
		final String value;
		public SkeinforgeOption(String module, String preference, String value) {
			this.parameter = "--option";
			this.module = module; 
			this.preference = preference; 
			this.value = value;
		}
/* *I don't think we want this to be used. We'll catch it if we try now...*
		public SkeinforgeOption(String parameter) {
			this.parameter = parameter;
			this.module = null;
			this.subprofile = null;
			this.preference = null;
			this.value = "";
		}
*/
		public String getParameter() {
			return this.parameter;
		}
		public String getModule() {
			return this.module;
		}
		public String getPreference() {
			return this.preference;
		}
		public String getValue() {
			return this.value;
		}
		public String getArgument() {
			return (this.module != null ? this.module + ":" : "") + (this.preference != null ? this.preference + "=" : "") + this.value;
		}
	}
		
	/**
	 * A SkeinforgePreference describes a user-visible preference that appears in the 
	 * configuration dialog.  SkeinforgePreferences should give a list of options
	 * that will be set at runtime.
	 * @param name The human-readable name of the preference.
	 * @param pereferenceName If you wish to cache the last selected value of this option in
	 * the java application preferences, specify it here.
	 * @param defaultState the default state of this preference, to be used if the
	 * preferenceState is not supplied or not set.
	 * @author phooky
	 *
	 */
	protected interface SkeinforgePreference {
		public JComponent getUI();
		public List<SkeinforgeOption> getOptions();
		public String valueSanityCheck();
	}
	
	public static class SkeinforgeChoicePreference implements SkeinforgePreference,ProfileWatcher {
		private Map<String,List<SkeinforgeOption>> optionsMap = new HashMap<String,List<SkeinforgeOption>>();
		private JPanel component;
		private DefaultComboBoxModel model;
		private String chosen;
		
		public SkeinforgeChoicePreference(String name, final String preferenceName, String defaultState, String toolTip) {
			component = new JPanel(new MigLayout("ins 5"));
			chosen = defaultState;
			if (preferenceName != null) {
				chosen = Base.preferences.get(preferenceName, defaultState);
			}
			model = new DefaultComboBoxModel();
			model.setSelectedItem(chosen);
			component.add(new JLabel(name));
			JComboBox cb = new JComboBox(model);
			component.add(cb);
			cb.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					chosen = (String)model.getSelectedItem();
					if (preferenceName != null) {
						Base.preferences.put(preferenceName,chosen);
					}
				}
			});
			if (toolTip != null) {
				component.setToolTipText(toolTip);
			}
		}
		public JComponent getUI() { return component; }
		
		public void addOption(String name, SkeinforgeOption o) {
			if (!optionsMap.containsKey(name)) {
				model.addElement(name);
				optionsMap.put(name, new LinkedList<SkeinforgeOption>());
				if (name.equals(chosen)) {
					model.setSelectedItem(name);
				}
			}
			List<SkeinforgeOption> list = optionsMap.get(name);
			list.add(o);
		}

		public List<SkeinforgeOption> getOptions() {
			if (optionsMap.containsKey(chosen)) {
				List<SkeinforgeOption> l = optionsMap.get(chosen);
				for (SkeinforgeOption o : l) {
					Base.logger.fine(o.getArgument());
				}
				return optionsMap.get(chosen);
			}
			return new LinkedList<SkeinforgeOption>();
		}
		
		public void profileChanged(Profile p) {
			// TODO: write this
		}
		
		@Override
		public String valueSanityCheck() {
			return null;
		}
	}
	
	protected static class SkeinforgeBooleanPreference implements SkeinforgePreference,ProfileWatcher {
		private boolean isSet;
		private JCheckBox component;
		private List<SkeinforgeOption> trueOptions = new LinkedList<SkeinforgeOption>();
		private List<SkeinforgeOption> falseOptions = new LinkedList<SkeinforgeOption>();
		
		public SkeinforgeBooleanPreference(String name, final String preferenceName, boolean defaultState, String toolTip) {
			isSet = defaultState;
			if (preferenceName != null) {
				isSet = Base.preferences.getBoolean(preferenceName, defaultState);
			}
			component = new JCheckBox(name, isSet);
			component.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					isSet = component.isSelected();
					if (preferenceName != null) {
						Base.preferences.putBoolean(preferenceName,isSet);
					}
				}
			});
			if (toolTip != null) {
				component.setToolTipText(toolTip);
			}
		}
		
		public JComponent getUI() { return component; }
		
		public void addTrueOption(SkeinforgeOption o) { trueOptions.add(o); }
		public void addFalseOption(SkeinforgeOption o) { falseOptions.add(o); }
		public void addNegateableOption(SkeinforgeOption o) {
			trueOptions.add(o);
			String negated = o.value.equalsIgnoreCase("true")?"false":"true";
			falseOptions.add(new SkeinforgeOption(o.module,o.preference,negated));
		}

		public List<SkeinforgeOption> getOptions() {
			return isSet?trueOptions:falseOptions;
		}
		
		public void profileChanged(Profile p) {
			// TODO: write this
		}

		@Override
		public String valueSanityCheck() {
			return null;
		}
	}

	public ConfigurationDialog visualConfiguregetCD(Frame parent, int x, int y, String name) {
		// First check for Python.
		parent.setName(name);
		ConfigurationDialog cd = new ConfigurationDialog(parent, this);
		cd.setName(name);
		cd.setTitle(name);
		//cd.setSize(500, 760);
		cd.pack();
		cd.setLocation(x, y);
		cd.setVisible(true);
		return cd;
	}
	public boolean visualConfigure(Frame parent, int x, int y, String name) {
		if (name == null)
			name = "Generating gcode";
		
		// First check for Python.
		boolean hasPython = PythonUtils.interactiveCheckVersion(parent,
				name, new PythonUtils.Version(2, 5, 0),
				new PythonUtils.Version(3, 0, 0));
		if (!hasPython) {
			return false;
		}
		boolean hasTkInter = PythonUtils.interactiveCheckTkInter(parent,
				name);
		if (!hasTkInter) {
			return false;
		}
		parent.setName(name);
		ConfigurationDialog cd = new ConfigurationDialog(parent, this);
		cd.setName(name);
		cd.setTitle(name);
		//cd.setSize(500, 760);
		
		if (x == -1 || y == -1) {
			double x2 = parent.getBounds().getCenterX();
			double y2 = parent.getBounds().getCenterY();
			cd.pack();
			x2 -= cd.getWidth() / 2.0;
			y2 -= cd.getHeight() / 2.0;
			x = (int)x2;
			y = (int)y2;
		} else {
			cd.pack();
		}
		
		cd.setLocation(x, y);
		cd.setVisible(true);
		emitUpdate("Config Done");
		return configSuccess;
	}
	
	public boolean visualConfigure(Frame parent) {
		return visualConfigure(parent, -1, -1, null);
	}

	public void editProfiles(Frame parent) {
		// First check for Python.
		boolean hasPython = PythonUtils.interactiveCheckVersion(parent,
				"Editing Profiles", new PythonUtils.Version(2, 5, 0),
				new PythonUtils.Version(3, 0, 0));
		if (!hasPython) {
			return;
		}
		boolean hasTkInter = PythonUtils.interactiveCheckTkInter(parent,
				"Editing Profiles");
		if (!hasTkInter) {
			return;
		}
		EditProfileDialog ep = new EditProfileDialog(parent, this);

		double x = parent.getBounds().getCenterX();
		double y = parent.getBounds().getCenterY();
		ep.pack();
		x -= ep.getWidth() / 2.0;
		y -= ep.getHeight() / 2.0;
		
		ep.setLocation((int)x, (int)y);
		ep.setVisible(true);
	}

	abstract public File getDefaultSkeinforgeDir();

	public File getSkeinforgeDir() {
		String skeinforgePath = System
				.getProperty("replicatorg.skeinforge.path");
		if (skeinforgePath == null || (skeinforgePath.length() == 0)) {
			return getDefaultSkeinforgeDir();
		}
		return new File(skeinforgePath);
	}

	public Profile duplicateProfile(Profile originalProfile, String newName) {
		File newProfDir = new File(getUserProfilesDir(), newName);
		File oldProfDir = new File(originalProfile.getFullPath());
		try {
			Base.copyDir(oldProfDir, newProfDir);
			Profile newProf = new Profile(newProfDir.getAbsolutePath());
			editProfile(newProf);
			return newProf;
		} catch (IOException ioe) {
			Base.logger.log(Level.SEVERE,
					"Couldn't copy directory", ioe);
		}
		return null;
	}
	
	public void editProfile(Profile profile) {
		String[] arguments = { PythonUtils.getPythonPath(), "skeinforge.py",
				"-p", profile.getFullPath() };
		ProcessBuilder pb = new ProcessBuilder(arguments);
		File skeinforgeDir = getSkeinforgeDir();
		pb.directory(skeinforgeDir);
		Process process = null;
		Base.logger.log(Level.FINEST, "Starting Skeinforge process...");
		
		/**
		 * Run the process and wait for it to return. Because of an issue with process.waitfor() failing to
		 * return, we now also do a busy wait with a timeout. The timeout value is loaded from timeout.txt
		 */
		try {
			// force failure if something goes wrong
			int value = 1;
			
			long timeoutValue = Base.preferences.getInt("replicatorg.skeinforge.timeout", -1);
			
			process = pb.start();
			
			//if no timeout set
			if(timeoutValue == -1)
			{
				Base.logger.log(Level.FINEST, "\tRunning SF without a timeout");
				value = process.waitFor();
			}
			else // run for timeoutValue cycles trying to get an exit value from the process
			{
				Base.logger.log(Level.FINEST, "\tRunning SF with a timeout");
				while(timeoutValue > 0)
				{
					Thread.sleep(1000);
					try
					{
						value = process.exitValue(); 
						break;
					}
					catch (IllegalThreadStateException itse)
					{
						timeoutValue--;
					}
				}
				if(timeoutValue == 0)
				{
					JOptionPane.showConfirmDialog(null, 
							"\tSkeinforge has not returned, This may be due to a communication error\n" +
							"between Skeinforge and ReplicatorG. If you are still editing a Skeinforge\n" +
							"profile, ignore this message; any changes you make in the skeinforge window\n" +
							"and save will be used when generating the gcode file.\n\n" +
							"\tAdjusting the \"Skeinforge timeout\" in the preferences window will affect how\n" +
							"long ReplicatorG waits before assuming that Skeinforge has failed, if you\n" +
							"frequently encounter this message you may want to increase the timeout.",
							"SF Timeout", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
				}
			}
			Base.logger.log(Level.FINEST, "Skeinforge process returned");
			if (value != 0) {
				Base.logger.severe("Unrecognized error code returned by Skeinforge.");
			}
			else
			{
				Base.logger.log(Level.FINEST, "Normal Exit on Skeinforge close");
			}
		} catch (IOException ioe) {
			Base.logger.log(Level.SEVERE, "Could not run skeinforge.", ioe);
		} catch (InterruptedException e) {
			// We are most likely shutting down, or the process has been
			// manually aborted.
			// Kill the background process and bail out.
			System.out.println("SkeinforgeGenerator.editProfile() interrupted: " + e);
			if (process != null) {
				process.destroy();
			}
		}
	}

	abstract public List<SkeinforgePreference> getPreferences();
	
	public BuildCode generateToolpath() {
		String path = model.getPath();
		
		profile.save(getUserProfilesDir(), null);

		List<String> arguments = new LinkedList<String>();
		// The -u makes python output unbuffered. Oh joyous day.
		String[] baseArguments = { PythonUtils.getPythonPath(), "-u",
				"skeinforge.py", "-p", profile.getFullPath() };
		for (String arg : baseArguments) {
			arguments.add(arg);
		}
		for (SkeinforgePreference preference : preferences) {
			List<SkeinforgeOption> options = preference.getOptions();
			if (options != null) {
				for (SkeinforgeOption option : options) {
					arguments.add(option.getParameter());
					String arg = option.getArgument();
					if (arg.length() > 0) arguments.add(arg);
				}
			}
		}
		arguments.add(path);

		ProcessBuilder pb = new ProcessBuilder(arguments);
		pb.directory(getSkeinforgeDir());
		Process process = null;
		try {
			process = pb.start();
			StreamLoggerThread ist = new StreamLoggerThread(
					process.getInputStream()) {
				@Override
				protected void logMessage(String line) {
					emitUpdate(line);
					super.logMessage(line);
				}
			};
			StreamLoggerThread est = new StreamLoggerThread(
					process.getErrorStream());
			est.setDefaultLevel(Level.SEVERE);
			ist.setDefaultLevel(Level.FINE);
			ist.start();
			est.start();
			int value = process.waitFor();
			if (value != 0) {
				Base.logger.severe("Unrecognized error code returned by Skeinforge.");
				// Throw ToolpathGeneratorException
				return null;
			}
		} catch (IOException ioe) {
			Base.logger.log(Level.SEVERE, "Could not run skeinforge.", ioe);
			// Throw ToolpathGeneratorException
			return null;
		} catch (InterruptedException e) {
			// We are most likely shutting down, or the process has been
			// manually aborted.
			// Kill the background process and bail out.
			if (process != null) {
				process.destroy();
			}
			return null;
		}
		int lastIdx = path.lastIndexOf('.');
		String root = (lastIdx >= 0) ? path.substring(0, lastIdx) : path;
		return new BuildCode(root, new File(root + ".gcode"));
	}
}
