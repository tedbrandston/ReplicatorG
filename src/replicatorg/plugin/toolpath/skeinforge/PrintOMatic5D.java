package replicatorg.plugin.toolpath.skeinforge;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.ParseException;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.app.ui.ProfileSavingTextField;
import replicatorg.app.ui.ProfileSavingCheckBox;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.SkeinforgeOption;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.SkeinforgePreference;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.Profile;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.ProfileWatcher;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.ProfileKeyWatcher;

public class PrintOMatic5D implements SkeinforgePreference,ProfileWatcher {
	private JPanel component;
	private JCheckBox enabled;
	private String baseName;
	private List<ProfileWatcher> profileWatchers = new LinkedList<ProfileWatcher>();
		
	abstract private class ValueSaver implements ProfileWatcher {
		Profile profile = null;
		DefaultComboBoxModel model = null;
		abstract public void valueChanged(String value);
		public void profileChanged(Profile newProfile) {
			profile = newProfile;
		}
		public void setComboBoxModel(DefaultComboBoxModel model) {
			this.model = model;
		}
	}
	
	private class KeyedValueSaver extends ValueSaver {
		String key = null;

		public KeyedValueSaver() {
		}

		public KeyedValueSaver(String key) {
			this.key = key;
		}

		@Override
		public void valueChanged(String value) {
			if (profile != null) {
				profile.setValueForPlastic(key, value);
			}
		}

		@Override
		public void profileChanged(Profile newProfile) {
			super.profileChanged(newProfile);
			String value = profile.getValueForPlastic(key);
			if (model != null)
				model.setSelectedItem(value);
		}
		
	}

	private class ComboListener implements ActionListener {
		final ValueSaver saver;
		final DefaultComboBoxModel input;
		
		public ComboListener(DefaultComboBoxModel input, ValueSaver saver) {
			this.input = input;
			this.saver = saver;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (input.getSize() == 0)
				return;

			Object o = input.getSelectedItem();
			String value = (o == null ? "" : (String)o.toString());
			
			if (saver != null) {
				saver.valueChanged(value);
			}
		}
	}	
	
	private class TextFieldPercentModifier implements ProfileSavingTextField.TextValueModifier {
		String optionName = null;
		final DecimalFormat numberFormat = new DecimalFormat();
		
		TextFieldPercentModifier(String name) {
			optionName = name;
			numberFormat.setMinimumFractionDigits(0);
			numberFormat.setMaximumFractionDigits(2);
			numberFormat.setMultiplier(100);
		}
		
		public String textToShowFromValue(String value) {
			Double number = -0.99; // clear indication of an error
			try {
				if (value != null)
					number = Double.valueOf(value);
			}
			catch (java.lang.NumberFormatException e) {
				Base.logger.severe("Print-O-Matic setting " + optionName + " does not contain a valid number, please correct this!");
			}
			return numberFormat.format(number);
		}
		
		public String valueToSaveFromText(String text) {
			Double number = -0.99; // clear indication of an error
			try {
				if (text != null)
					number = (Double)numberFormat.parse(text);
			}
			catch (java.text.ParseException e) {
				Base.logger.severe("Print-O-Matic setting " + optionName + " does not contain a valid number, please correct this!");
			}
			return number.toString();
		}
	}
	
	private class FormattedDoubleModifier implements ProfileSavingTextField.CalculatedValueModifier {
		String optionName = null;
		final DecimalFormat numberFormat = new DecimalFormat();
		
		FormattedDoubleModifier(String name) {
			optionName = name;
			numberFormat.setMinimumFractionDigits(0);
			numberFormat.setMaximumFractionDigits(4);
		}
		
		FormattedDoubleModifier(String name, int minFractionDigits, int maxFractionDigits) {
			optionName = name;
			numberFormat.setMinimumFractionDigits(minFractionDigits);
			numberFormat.setMaximumFractionDigits(maxFractionDigits);
		}
		
		Double getValueAsDouble(Profile profile, String key) {
			String textValue = profile.getValueForPlastic(key);
			Double doubleValue = null;
			try {
				if (textValue != null)
					doubleValue = Double.valueOf(textValue);
			}
			catch (java.lang.NumberFormatException e) {
				Base.logger.severe("Invalid numeric value: " + textValue);
			}
			
			return doubleValue;
		}
		
		public String profileChangedRecalc(Profile profile) {
			Double value = getValueAsDouble(profile, optionName);
			return numberFormat.format(value == null ? 0 : value);
		}
		
		public void saveValue(String text, Profile profile) {
			profile.setValueForPlastic(optionName, text);
		}
	}
	
	private void addTextParameter(JComponent target, String name, String description, String defaultValue, String toolTip) {
		ProfileSavingTextField input = new ProfileSavingTextField(name, "", 10);
		profileWatchers.add(input);
		
		target.add(new JLabel(description));
		target.add(input, "wrap");
		
		if (description.contains("(%)")) {
			input.setModifier(new TextFieldPercentModifier(description));
		}
		
		if (toolTip != null) {
			// TODO: This is wrong.
			input.setToolTipText(toolTip);
		}
	}
	
	private void addTextParameter(JComponent target, ProfileSavingTextField.CalculatedValueModifier modifier, String description, String defaultValue, String toolTip) {
		ProfileSavingTextField input = new ProfileSavingTextField(modifier, 10);
		profileWatchers.add(input);
		
		target.add(new JLabel(description));
		target.add(input, "wrap");
		
		if (toolTip != null) {
			// TODO: This is wrong.
			input.setToolTipText(toolTip);
		}
	}
	
	private void addDropDownParameter(JComponent target, ValueSaver saver, String description, Vector<String> options, String toolTip) {
		ValueSaver mySaver = saver;
		target.add(new JLabel(description));
		
		DefaultComboBoxModel model;
		model = new DefaultComboBoxModel(options);
		ComboListener listener = new ComboListener(model, mySaver);
		mySaver.setComboBoxModel(model);
		
		JComboBox input = new JComboBox(model);
		target.add(input, "wrap");
		input.addActionListener(listener);
		profileWatchers.add(mySaver);
		
		if (toolTip != null) {
			// TODO: This is wrong.
			input.setToolTipText(toolTip);
		}
		
	}

	
	private void addBooleanParameter(JComponent target, String name, String description, boolean defaultValue, String toolTip) {
		target.add(new JLabel(description));
		
		final ProfileSavingCheckBox input = new ProfileSavingCheckBox(name);
		target.add(input, "wrap");
		profileWatchers.add(input);
		
		if (toolTip != null) {
			// TODO: This is wrong.
			input.setToolTipText(toolTip);
		}
		
	}

	JTabbedPane printOMatic5D;

	boolean processingProfileChange;
	
	public PrintOMatic5D() {
		component = new JPanel(new MigLayout("ins 0, fillx, hidemode 1"));
		processingProfileChange = false;
		
		baseName = "replicatorg.skeinforge.printOMatic5D.";

		// Add a checkbox to switch print-o-matic on and off
		final String enabledName = baseName + "enabled";
		enabled = new JCheckBox("Use Print-O-Matic (stepper extruders only)", Base.preferences.getBoolean(enabledName,false));
		enabled.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (enabledName != null) {
					Base.preferences.putBoolean(enabledName,enabled.isSelected());
					
					printOMatic5D.setVisible(enabled.isSelected());
					printOMatic5D.invalidate();
					Window w = SwingUtilities.getWindowAncestor(printOMatic5D);
					w.pack();
				
				}
			}
		});
		
		component.add(enabled, "wrap, spanx");
		
		// Make a tabbed pane to sort basic and advanced components 
		printOMatic5D = new JTabbedPane();
		
		JComponent printPanel = new JPanel(new MigLayout("fillx"));
		JComponent materialPanel = new JPanel(new MigLayout("fillx"));
                JComponent supportPanel = new JPanel(new MigLayout("fillx"));
		
		addTextParameter(printPanel, "fill.csv:Infill Solidity (ratio):",
				"Object infill (%)", "30",
				"0= hollow object, 100=solid object");
		
		addTextParameter(printPanel, new FormattedDoubleModifier("carve.csv:Layer Thickness (mm):", 2, 4),
			"Layer Height (mm)", "0.35",
			"Set the desired layer height");

		ProfileSavingTextField.CalculatedValueModifier pathWidthCalculator = new FormattedDoubleModifier(null, 0, 4) {
				@Override
				public String profileChangedRecalc(Profile profile) {
					Double thickness = getValueAsDouble(profile, "carve.csv:Layer Thickness (mm):");
					Double widthOverThickness = getValueAsDouble(profile, "carve.csv:Perimeter Width over Thickness (ratio):");
			
					if (thickness == null || widthOverThickness == null) {
						return "";
					}
			
					Double width = widthOverThickness * thickness;
			
					numberFormat.setMinimumFractionDigits(0);
					numberFormat.setMaximumFractionDigits(2);
					return numberFormat.format(width);
				}
		
				@Override
				public void saveValue(String text, Profile profile) {
					Double thickness = getValueAsDouble(profile, "carve.csv:Layer Thickness (mm):");

					String widthText = text;
					Double width = 0.0;
					try {
						if (widthText != null)
							width = Double.valueOf(widthText);
				
						Double widthOverThickness = width / thickness;

						profile.setValueForPlastic("carve.csv:Perimeter Width over Thickness (ratio):", widthOverThickness.toString());
					}
					catch (java.lang.NumberFormatException e) {
						Base.logger.severe("Invalid Width setting: " + widthText);
					}
				}
			};
			
		addTextParameter(printPanel, pathWidthCalculator,
					"Path Width (mm)", "0.5",
					"Set the desired layer width");
		
		// We save the extra shells in various values, so we need a calculator
		ProfileSavingTextField.CalculatedValueModifier extraShellsCalculator = new FormattedDoubleModifier("fill.csv:Extra Shells on Base (layers):", 0, 4) {
				@Override
				public void saveValue(String text, Profile profile) {
					profile.setValueForPlastic("fill.csv:Extra Shells on Base (layers):", text);
					profile.setValueForPlastic("fill.csv:Extra Shells on Alternating Solid Layer (layers):", text);
					profile.setValueForPlastic("fill.csv:Extra Shells on Sparse Layer (layers):", text);
				}
			};
		
		addTextParameter(printPanel, extraShellsCalculator,
				"Number of shells:", "1",
				"Number of shells to add to the perimeter of an object. Set this to 0 if you are printing a model with thin features.");
		
		addTextParameter(printPanel, new FormattedDoubleModifier("speed.csv:Feed Rate (mm/s):", 0, 1),
				"Feedrate (mm/s)", "30",
				"slow: 0-20, default: 30, Fast: 40+");
		
		
		Vector<String> materialTypes = new Vector<String>();
		// These come from the Profile now...
		// materialTypes.add("ABS");
		// materialTypes.add("PLA");
		
		final ProfileWatcher parentWatcher = this;
		
		addDropDownParameter(materialPanel, new KeyedValueSaver("extrusion.csv:Profile Selection:") {
					boolean isInChanged = false;
					@Override
					public void valueChanged(String value) {
						if (profile != null) {
							Base.logger.log(Level.FINEST, " ### New Profile: " + value);
							profile.setValue(key, value); // <- Don't save for plastic!
							isInChanged = true;
							parentWatcher.profileChanged(profile);
							isInChanged = false;
						}
					}
					@Override
					public void profileChanged(Profile newProfile) {
						if (isInChanged == true)
							return;
						super.profileChanged(newProfile);
						String value = profile.getValue(key); // <- Don't get for plastic!
						if (model != null) {
							// Now we wipe out the list, repopulate from the selected profile, and then select the correct one.
							model.setSelectedItem(null);
							model.removeAllElements();
							for (String subProfile : profile.getSubProfiles()) {
								model.addElement(subProfile);
							}
							model.setSelectedItem(value);
						}
					}
				},
				"Material type:", materialTypes,
				"Select the type of plastic to use during print");
		
		addTextParameter(materialPanel, "dimension.csv:Filament Diameter (mm):",
				"Filament Diameter (mm)", "2.94",
				"measure feedstock");
                
                // TODO: Tie the materialType to this text box, so that switching the puldown changes this default
		addTextParameter(materialPanel, "dimension.csv:Filament Packing Density (ratio):",
				"Final Volume (%)", "85",
				"Between 85 and 100.");
		
		addBooleanParameter(supportPanel, "raft.csv:Add Raft, Elevate Nozzle, Orbit:",
				"Use raft", true,
				"If this option is checked, skeinforge will lay down a rectangular 'raft' of plastic before starting the build.  "
				+ "Rafts increase the build size slightly, so you should avoid using a raft if your build goes to the edge of the platform.");
				
		Vector<String> supportTypes = new Vector<String>();
		supportTypes.add("None");
		supportTypes.add("Exterior support");
		supportTypes.add("Full support");

		addDropDownParameter(supportPanel, new KeyedValueSaver() {
					@Override
					public void valueChanged(String value) {
						if (profile != null) {
							if (value.equals("None")) {
								profile.setValueForPlastic("raft.csv:None", "True");
								profile.setValueForPlastic("raft.csv:Empty Layers Only", "False");
								profile.setValueForPlastic("raft.csv:Everywhere", "False");
								profile.setValueForPlastic("raft.csv:Exterior Only", "False");
							}
							else
							if (value.equals("Exterior support")) {
								profile.setValueForPlastic("raft.csv:None", "False");
								profile.setValueForPlastic("raft.csv:Empty Layers Only", "False");
								profile.setValueForPlastic("raft.csv:Everywhere", "False");
								profile.setValueForPlastic("raft.csv:Exterior Only", "True");
							}
							else
							if (value.equals("Full support")) {
								profile.setValueForPlastic("raft.csv:None", "False");
								profile.setValueForPlastic("raft.csv:Empty Layers Only", "False");
								profile.setValueForPlastic("raft.csv:Everywhere", "True");
								profile.setValueForPlastic("raft.csv:Exterior Only", "False");
							}
						}
					}

					@Override
					public void profileChanged(Profile newProfile) {
						super.profileChanged(newProfile);
						String value = "None"; // Default to None
						String raftNone = profile.getValueForPlastic("raft.csv:None");
						if (raftNone!= null && raftNone.equals("False")) {
							// Ok, so we need to either select Exterior or Full
							String raftExteriorOnly = profile.getValueForPlastic("raft.csv:Exterior Only");
							if (raftExteriorOnly!= null && raftExteriorOnly.equalsIgnoreCase("True")) {
								value = "Exterior support";
							} else {
								value = "Full support";
							}
							
						}
						
						if (model != null)
							model.setSelectedItem(value);
					}

				},
				"Use support material:", supportTypes,
				"If this option is selected, skeinforge will attempt to support large overhangs by laying down a support "+
				"structure that you can later remove.");
	
		
		printOMatic5D.addTab("Settings", printPanel);
		printOMatic5D.addTab("Plastic", materialPanel);
                printOMatic5D.addTab("Support", supportPanel);
		component.add(printOMatic5D, "spanx");
		printOMatic5D.setVisible(enabled.isSelected());
	}

	public JComponent getUI() { return component; }
	
		
	public void profileChanged(Profile profile) {
		if (profile == null || processingProfileChange)
			return;
		processingProfileChange = true;
		for (ProfileWatcher p : profileWatchers) {
			p.profileChanged(profile);
		}
		processingProfileChange = false;
	}

	
	// Check the options to determine if they are in an acceptable range. Return null if
	// everything is ok, or a string describing the error if they are not ok.
	public String valueSanityCheck() {
		
		if (enabled.isSelected()) {
			// Check that width/thickness is ok
/*
                        double perimeterWidthOverThickness = getValue("desiredPathWidth")/getValue("desiredLayerHeight");
                        if (perimeterWidthOverThickness > 1.8) {
                                return "Layer height is smaller than recommended for the specified nozzle. Try increasing the layer height, or changing to a smaller nozzle.";
                        }
                        if (perimeterWidthOverThickness < 1.2) {
                                return "Layer height is larger than recommended for the specified nozzle. Try decreasing the layer height, or changing to a larger nozzle.";
                        }
*/
			
		}
		
		return null;
	}
	
	public List<SkeinforgeOption> getOptions() {
		
		List<SkeinforgeOption> options = new LinkedList<SkeinforgeOption>();
/*

		if (enabled.isSelected()) {
		
			double  infillRatio                        = getValue("infillPercent")/100;
			double  filamentDiameter                   = getValue("filamentDiameter");
			double  packingDensity                     = getValue("packingDensity")/100;
			double  perimeterWidthOverThickness        = getValue("desiredPathWidth")/getValue("desiredLayerHeight");
			double  infillWidthOverThickness           = perimeterWidthOverThickness;
			double  feedRate                           = getValue("desiredFeedrate");
			double  layerHeight                        = getValue("desiredLayerHeight");
			double  extraShellsOnAlternatingSolidLayer = getValue("numberOfShells");
			double  extraShellsOnBase                  = getValue("numberOfShells");
			double  extraShellsOnSparseLayer           = getValue("numberOfShells");
			boolean useRaft                            = getBooleanValue("useRaft");
			String  supportType                        = getStringValue("choiceSupport");

			Base.logger.fine("Print-O-Matic settings:"
					+ "\n infillRatio=" + infillRatio
					+ "\n filamentDiameter=" + filamentDiameter
					+ "\n packingDensity=" + packingDensity
					+ "\n perimeterWidthOverThickness=" + perimeterWidthOverThickness
					+ "\n infillWidthOverThickness=" + infillWidthOverThickness
					+ "\n feedRate=" + feedRate
					+ "\n layerHeight=" + layerHeight
					+ "\n extraShellsOnAlternatingSolidLayer=" + extraShellsOnAlternatingSolidLayer
					+ "\n extraShellsOnBase=" + extraShellsOnBase
					+ "\n extraShellsOnSparseLayer=" + extraShellsOnSparseLayer
					+ "\n useRaft=" + useRaft
					+ "\n supportType=" + supportType
					);
			
			options.add(new SkeinforgeOption("fill.csv", "Infill Solidity (ratio):", Double.toString(infillRatio)));
			options.add(new SkeinforgeOption("speed.csv", "Feed Rate (mm/s):", Double.toString(feedRate)));
			options.add(new SkeinforgeOption("speed.csv", "Flow Rate Setting (float):", Double.toString(feedRate)));
			options.add(new SkeinforgeOption("dimension.csv", "Filament Diameter (mm):", Double.toString(filamentDiameter)));
			options.add(new SkeinforgeOption("dimension.csv", "Filament Packing Density (ratio):", Double.toString(packingDensity)));
			options.add(new SkeinforgeOption("carve.csv", "Perimeter Width over Thickness (ratio):", Double.toString(perimeterWidthOverThickness)));
			options.add(new SkeinforgeOption("fill.csv", "Infill Width over Thickness (ratio):", Double.toString(infillWidthOverThickness)));
			options.add(new SkeinforgeOption("carve.csv", "Layer Thickness (mm):", Double.toString(layerHeight)));
			options.add(new SkeinforgeOption("fill.csv", "Extra Shells on Alternating Solid Layer (layers):", Double.toString(extraShellsOnAlternatingSolidLayer)));
			options.add(new SkeinforgeOption("fill.csv", "Extra Shells on Base (layers):", Double.toString(extraShellsOnBase)));
			options.add(new SkeinforgeOption("fill.csv", "Extra Shells on Sparse Layer (layers):", Double.toString(extraShellsOnSparseLayer)));
			options.add(new SkeinforgeOption("raft.csv", "Add Raft, Elevate Nozzle, Orbit:", useRaft ? "true" : "false"));
			
			if (supportType.equals("None")) {
				options.add(new SkeinforgeOption("raft.csv","None", "true"));
				options.add(new SkeinforgeOption("raft.csv","Empty Layers Only", "false"));
				options.add(new SkeinforgeOption("raft.csv","Everywhere", "false"));
				options.add(new SkeinforgeOption("raft.csv","Exterior Only", "false"));
			}
			else
			if (supportType.equals("Exterior support")) {
				options.add(new SkeinforgeOption("raft.csv","None", "false"));
				options.add(new SkeinforgeOption("raft.csv","Empty Layers Only", "false"));
				options.add(new SkeinforgeOption("raft.csv","Everywhere", "false"));
				options.add(new SkeinforgeOption("raft.csv","Exterior Only", "true"));
			}
			else
			if (supportType.equals("Full support")) {
				options.add(new SkeinforgeOption("raft.csv","None", "false"));
				options.add(new SkeinforgeOption("raft.csv","Empty Layers Only", "false"));
				options.add(new SkeinforgeOption("raft.csv","Everywhere", "true"));
				options.add(new SkeinforgeOption("raft.csv","Exterior Only", "false"));
			}
		}
		
*/
		return options;
	}
}