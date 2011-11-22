package replicatorg.app.ui;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.math.BigInteger;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import net.miginfocom.swing.MigLayout;
import replicatorg.drivers.gen3.Sanguino3GDriver;


public class DebugWindow extends JFrame
{
	private final Sanguino3GDriver driver;
	public boolean failure = false;
	
	public DebugWindow(Sanguino3GDriver driver)
	{
		super("Debug window");
		
		if(driver == null)
			failure = true;
		
		this.driver = driver;
		Container content = getContentPane();
		
		content.setLayout(new MigLayout("fill"));
		
		final JButton setDCodeButton;
		final JTextField setDCodeField;
		
		setDCodeField = new JTextField();
		setDCodeField.setColumns(5);
		
		setDCodeButton = new JButton("Set Debug Code (0x76)");
		setDCodeButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Integer value = null;
				try {
					value = Integer.valueOf(setDCodeField.getText());
				} catch(NumberFormatException e) {
					setDCodeField.setText("NaN");
				}
					
				if(value != null)
					DebugWindow.this.driver.setDebugCode(value);
			}
		});
		content.add(setDCodeButton, "split");
		content.add(setDCodeField, "growx, wrap");
		
		final JButton getDCodeButton;
		final JTextField getDCodeField;
		
		getDCodeField = new JTextField();
		getDCodeField.setEditable(false);
		getDCodeField.setEnabled(false);
		getDCodeField.setColumns(5);
		
		getDCodeButton = new JButton("Get Debug Code (0x77)");
		getDCodeButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Integer code = DebugWindow.this.driver.getDebugCode();
				getDCodeField.setText(code.toString());
			}
		});
		content.add(getDCodeButton, "split");
		content.add(getDCodeField, "growx, wrap");
		
		final JButton getDBufferButton;
		final JTextArea getDBufferField;
		
		getDBufferButton = new JButton("Get Debug Buffer");
		getDBufferField = new JTextArea();
		getDBufferField.setEditable(false);
		getDBufferField.setColumns(16);
		getDBufferField.setRows(8);
		getDBufferField.setLineWrap(true);
		
		getDBufferButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				byte[] buffer = DebugWindow.this.driver.getDebugBuffer();
				
				getDBufferField.setText("");
				for(int i = 0; i < buffer.length; i++)
				{
					BigInteger bi = new BigInteger(1, new byte[]{buffer[i]});
					//convert to hex and append
					getDBufferField.append(bi.toString(16) + " ");
				}
			}
		});

		content.add(getDBufferButton, "split");
		content.add(getDBufferField, "wrap");
		
		pack();
		
		content.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				KeyStroke wc = MainWindow.WINDOW_CLOSE_KEYSTROKE;
				if ((e.getKeyCode() == KeyEvent.VK_ESCAPE)
						|| (KeyStroke.getKeyStrokeForEvent(e).equals(wc))) {
					dispose();
				}
			}
		});
	}
	
}
