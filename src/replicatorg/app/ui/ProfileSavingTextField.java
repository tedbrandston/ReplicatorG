package replicatorg.app.ui;

import replicatorg.app.Base;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.Profile;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.ProfileWatcher;

public class ProfileSavingTextField extends ActionTextField implements ProfileWatcher {
	final String keyName;
	Profile profile = null;
	TextValueModifier modifier = null;
	CalculatedValueModifier calcModifier = null;
	
	public interface TextValueModifier {
		String textToShowFromValue(String value);
		String valueToSaveFromText(String text);
	}

	public interface CalculatedValueModifier {
		String profileChangedRecalc(Profile profile);
		void saveValue(String text, Profile profile);
	}
	
	public ProfileSavingTextField(String keyName, String text, int columns) {
		super(text, columns, Base.getLocalFormat());
		this.keyName = keyName;
	}

	public ProfileSavingTextField(CalculatedValueModifier calcModifier, int columns) {
		super("", columns, Base.getLocalFormat());
		this.calcModifier = calcModifier;
		this.keyName = null;
	}
	
	public void setModifier(TextValueModifier modifier) {
		this.modifier = modifier;
	}

	@Override
	public void doSaveEvent() {
		String value = getText();
		if (calcModifier != null) {
			calcModifier.saveValue(value, profile);
		} else {
			if (modifier != null)
				value = modifier.valueToSaveFromText(value);

			Base.logger.fine("text changed: " + keyName + "=" + value);
			if (profile != null) {
				profile.setValueForPlastic(keyName, value);
			}
		}
	}

	@Override
	public void profileChanged(Profile newProfile) {
		profile = newProfile;
		if (profile != null) {
			String value = null;
			if (calcModifier != null) {
				value = calcModifier.profileChangedRecalc(profile);
			} else {
				value = profile.getValueForPlastic(keyName);

				if (modifier != null)
					value = modifier.textToShowFromValue(value);
			}
			setText(value);
		}
	}
}
