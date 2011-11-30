package replicatorg.plugin.toolpath.skeinforge;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.Profile;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.SkeinforgePreference;

class ConfigurationDialog extends JDialog implements Profile.ProfileChangedWatcher {
	final boolean postProcessToolheadIndex = true;
	final String profilePref = "replicatorg.skeinforge.profilePref";

	JButton generateButton = new JButton("Generate Gcode");
	JButton cancelButton = new JButton("Cancel");
	JButton saveButton = new JButton("Save...");

	/* these must be explicitly nulled at close because of a java bug:
	 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6497929
	 * 
	 * because JDialogs may never be garbage collected, anything they keep reference to 
	 * may never be gc'd. By explicitly nulling these in the setVisible() function
	 * we allow them to be removed.
	 */
	private SkeinforgeGenerator parentGenerator = null;
	private List<Profile> profiles = null;
	private DefaultComboBoxModel menuModel = new DefaultComboBoxModel();

	JPanel profilePanel = new JPanel();

	/**
	 * Fills a combo box with a list of skeinforge profiles
	 * @param comboBox to fill with list of skeinforge profiles
	 */
	private void loadList(JComboBox comboBox) {
		comboBox.removeAllItems();
		profiles = new ArrayList<Profile>(parentGenerator.getProfiles());
		for (Profile p : profiles) {
			menuModel.addElement(p);
		}
		comboBox.setModel(menuModel);
		Profile lastProfile = parentGenerator.getSelectedProfile();
		if (lastProfile != null) {
			lastProfile.addChangeWatcher(this);
			menuModel.setSelectedItem(lastProfile);
		}
	}

	// this means the profile is telling us that it is in a changed state (or not)
	public void profileIsChanged(Profile profile) {
		Base.logger.log(Level.FINEST, "profileIsChanged");
		int index = menuModel.getIndexOf(profile);

		if (!profile.isChanged()) {
			if (index > 0) {
				Object menuItem = menuModel.getElementAt(index-1);
				boolean isSelectedProfile = menuModel.getSelectedItem().equals(menuItem);
				if (menuItem instanceof String)
					menuModel.removeElement(menuItem);
				if (isSelectedProfile)
					menuModel.setSelectedItem(profile);
			}
			ConfigurationDialog.this.saveButton.setEnabled(false);
		} else {
			Base.logger.log(Level.FINEST, "profileIsChanged/insertElementAt");
			menuModel.insertElementAt(profile.getName(), index);
			ConfigurationDialog.this.saveButton.setEnabled(true);
		}
	}

	private void setSelectedProfile(Profile profile) {
		menuModel.setSelectedItem(profile);
	}


	/**
	 * Help reduce effects of miserable memory leak.
	 * see declarations above.
	 */
	@Override
	public void setVisible(boolean b) {
		super.setVisible(b);
		if(!b)
		{
			parentGenerator = null;
			profiles = null;
		}
	}

	final JComboBox prefPulldown = new JComboBox();

	public ConfigurationDialog(final Frame parent, final SkeinforgeGenerator parentGeneratorIn) {
		super(parent, true);
		parentGenerator = parentGeneratorIn;
		setTitle("GCode Generator");
		setLayout(new MigLayout("aligny, top, ins 5, fill"));

		//loadList(prefPulldown);

		generateButton.setEnabled(true);
		saveButton.setEnabled(false);

		add(new JLabel("Base Profile:"), "split 2");

		loadList(prefPulldown);
		prefPulldown.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String value = (String)prefPulldown.getSelectedItem().toString();
				Profile oldProfile = parentGenerator.getSelectedProfile();
				boolean changed = parentGenerator.setSelectedProfile(value);
				// There's a chance that the profile won't actually change, if the user cancelled
				if (changed) {
					oldProfile.removeChangeWatcher(ConfigurationDialog.this);
					parentGenerator.getSelectedProfile().addChangeWatcher(ConfigurationDialog.this);
				} else {
					ConfigurationDialog.this.setSelectedProfile(oldProfile);
				}
			}
		});
		add(prefPulldown, "wrap, growx");

		for (SkeinforgePreference preference: parentGenerator.preferences) {
			add(preference.getUI(), "wrap");
		}

		final JCheckBox autoGen = new JCheckBox("Automatically generate when building.");
		autoGen.setToolTipText("When building from the model view with this checked " +
				"GCode will automatically be generated, bypassing this dialog.");
		add(autoGen, "wrap");
		autoGen.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Base.preferences.putBoolean("build.autoGenerateGcode", autoGen.isSelected());
			}
		});
		autoGen.setSelected(Base.preferences.getBoolean("build.autoGenerateGcode", false));

		add(cancelButton, "tag cancel, split 3");
		add(saveButton, "tag finish");
		add(generateButton, "tag ok");
		generateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parentGenerator.configSuccess = configureGenerator();
				setVisible(!parentGenerator.configSuccess);
			}
		});
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parentGenerator.configSuccess = false;
				setVisible(false);
			}
		});
		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Profile p = parentGenerator.getSelectedProfile();
				String newName = JOptionPane.showInputDialog(parent,
						"Choose a name (don't change it to over):", p.getName());
				if (newName != null) {
					p.save(parentGenerator.getUserProfilesDir(), newName);
					parentGenerator.setSelectedProfile(newName);
					pack();
				}
			}
		});
	}

	/**
	 * Does pre-skeinforge generation tasks
	 */
	protected boolean configureGenerator()
	{
		if(!parentGenerator.runSanityChecks()) {
			return false;
		}

		int idx = prefPulldown.getSelectedIndex();

		if(idx == -1) {
			return false;
		}

		Profile p = parentGenerator.getSelectedProfile();
		Base.preferences.put("lastGeneratorProfileSelected",p.toString());
		//parentGenerator.profile = p.getFullPath();
		//SkeinforgeGenerator.setSelectedProfile(p.toString());
		return true;
	}
};
