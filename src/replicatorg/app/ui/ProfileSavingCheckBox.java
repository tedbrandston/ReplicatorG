package replicatorg.app.ui;

import java.util.logging.Level;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JCheckBox;

import replicatorg.app.Base;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.Profile;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.ProfileWatcher;

// Text field that keeps track of whether it's data has been modified, and calls a function
// when it loses focus or gets an ENTER key to allow the subclasser to handle the event.
public class ProfileSavingCheckBox extends JCheckBox implements ActionListener,ProfileWatcher {
	private Profile profile = null;
	private String key = null;
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (profile != null) {
			profile.setValueForPlastic(key, isSelected() ? "True" : "False");
		}
	}

	@Override
	public void profileChanged(Profile newProfile) {
		profile = newProfile;
		if (newProfile != null) {
			Base.logger.log(Level.FINEST, "ProfileSavingCheckBox, value for key " + key + "=" + newProfile.getValueForPlastic(key));
			String value = newProfile.getValueForPlastic(key);
			setSelected(value == null ? false : value.equalsIgnoreCase("True"));
		}
	}
	
	public ProfileSavingCheckBox(String key) {
		super();
		
		this.key = key;
		addActionListener(this);
	}
	
}
