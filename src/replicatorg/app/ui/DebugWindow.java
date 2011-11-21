package replicatorg.app.ui;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;
import replicatorg.drivers.gen3.Sanguino3GDriver;


public class DebugWindow extends JFrame
{
	private final Sanguino3GDriver driver;
	
	public DebugWindow(Sanguino3GDriver driver)
	{
		super("Debug window");
		
		this.driver = driver;
		Container content = getContentPane();
		
		content.setLayout(new MigLayout(""));
		
		final JButton setDCodeButton;
		final JFormattedTextField setDCodeField;
		
		setDCodeField = new JFormattedTextField();
		
		setDCodeButton = new JButton("Set Debug Code (0x76)");
		setDCodeButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				DebugWindow.this.driver.setDebugCode((Integer)setDCodeField.getValue());
			}
		});
		content.add(setDCodeButton);
		content.add(setDCodeField, "wrap");
		
		final JButton getDCodeButton;
		final JFormattedTextField getDCodeField;
		
		getDCodeField = new JFormattedTextField();
		getDCodeField.setEditable(false);
		getDCodeField.setEnabled(false);
		
		getDCodeButton = new JButton("Get Debug Code (0x77)");
		getDCodeButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Integer code = DebugWindow.this.driver.getDebugCode();
				getDCodeField.setText(code.toString());
			}
		});
		content.add(getDCodeButton);
		content.add(getDCodeField, "wrap");
	}
	
}
