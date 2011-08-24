package replicatorg.app.ui;
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

$Id: MainWindow.java 370 2008-01-19 16:37:19Z mellis $
*/
/**
 * @author Noah Levy
 * 
 * <class>DualStrusionWindow</class> is a Swing class designed to integrate DualStrusion into the existing ReplicatorG GUI
 */


import java.awt.Color;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import replicatorg.app.Base;
import replicatorg.dualstrusion.DualStrusionConstruction;
import replicatorg.model.BuildCode;
import replicatorg.model.GCodeSource;
import replicatorg.model.StringListSource;

import net.miginfocom.swing.MigLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
public class DualStrusionWindow extends JFrame implements ActionListener, ItemListener{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2548421042732389328L; //Generated serial
	File result; // final combined gcode will be saved here
	JFrame frame = new JFrame("DualStrusion Window");
	boolean hasOneGcode; //this boolean is true if the constructor is passed one gcode file to begin with, it later effect the layout of the Swing Window
	String originalGcodePath;
	/**
	 * 
	 */

	/**
	 * 
	 * This is the default constructor, it is only invoked if the ReplicatorG window did not already have a piece of gcode open
	 */
	public DualStrusionWindow()
	{
		hasOneGcode = false;
	}
/**
 * This is a constructor that takes the filepath of the gcode open currently in ReplicatorG
 * @param s the path of the gcode currently open in RepG
 */
	public DualStrusionWindow(String s) {
		// TODO Auto-generated constructor stub
		hasOneGcode = true;
		originalGcodePath = s;
		
	}
/**
 * This method creates and shows the DualStrusionWindow GUI, this window is a MigLayout with 3 JFileChooser-TextBox Pairs, the first two being source gcodes and the last being the combined gcode destination.
 * It also links to online DualStrusion Documentation NOTE: This may be buggy, it uses getDesktop() which is JDK 1.6 and scary.
 * This method also invokes the thread in which the gcode combining operations run in, I would like to turn this into a SwingWorker soon.
 */
	public void go()
	{
		
		//JCheckBox useSD = new JCheckBox(false);
		frame.dispose();
		frame.setResizable(true);
		//frame.setContentPane(this);
		frame.setVisible(true);	
		frame.setLocation(400, 0);
		frame.setSize(800,500 );
		Container cont = this.getContentPane();
		cont.setLayout(new MigLayout("fill"));
		cont.setVisible(true);
		JTextArea explanation = new JTextArea();
		explanation.setText("This window is used to combine two Gcode files generated by SkeinForge. This allows for multiple objects in one print job or multiple materials or colors in one printed object. The resulting gcode assumes that Toolhead1 is on the left and Toolhead0 is on the right");
		explanation.setOpaque(false);
		explanation.setEditable(false);
		explanation.setWrapStyleWord(true);
		explanation.setSize(700, 200);
		explanation.setLineWrap(true);
		cont.add(explanation, "wrap");
		final JLabel linkage = new JLabel("<html><u>See documentation</u></html>");
		linkage.setForeground(Color.BLUE);

		linkage.addMouseListener(new MouseListener()
		{

			@Override
			public void mouseClicked(MouseEvent arg0) {

				try {
					Desktop.getDesktop().browse(new URI("http://www.makerbot.com/docs/dualstrusion"));
					linkage.setForeground(Color.MAGENTA);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}
		});

		cont.add(linkage, "wrap");
		cont.add(new JLabel("Toolhead 1 (Primary)"), "split");
		
		final JTextField Toolhead1 = new JTextField(60);
		Toolhead1.setText("");
		if(hasOneGcode)
		{
			Toolhead1.setText(originalGcodePath);
		}
		JButton Toolhead1ChooserButton = new JButton("Browse...");
		Toolhead1ChooserButton.addActionListener(new ActionListener()
		{

			
			public void actionPerformed(ActionEvent arg0) {
				String s = null;
				if(!Toolhead1.getText().equals(""))
				{
				s = GcodeSelectWindow.goString(new File(Toolhead1.getText()));	
				}
				else
				{
				s = GcodeSelectWindow.goString();
				}
				if(s != null)
				{
				Toolhead1.setText(s);
				}
				
			}
			
		});
		cont.add(Toolhead1,"split");
		cont.add(Toolhead1ChooserButton, "wrap");
		
		//
		
		final JTextField Toolhead0 = new JTextField(60);
		Toolhead0.setText("");
		
		JButton Toolhead0ChooserButton = new JButton("Browse...");
		Toolhead0ChooserButton.addActionListener(new ActionListener()
		{
			
			public void actionPerformed(ActionEvent arg0) {
				String s = null;
				if(!Toolhead0.getText().equals(""))
				{
				s = GcodeSelectWindow.goString(new File(Toolhead0.getText()));	
				}
				else
				{
				s = GcodeSelectWindow.goString();
				}
				if(s != null)
				{
				Toolhead0.setText(s);
				}
				
			}
			
		});
		JButton switchItem = new JButton("Switch Toolheads"); //This button switches the contents of the two text fields in order to easily swap Primary and Secondary Toolheads
		switchItem.addActionListener(new ActionListener()
		{
		

			
			public void actionPerformed(ActionEvent arg0) {
				String temp = Toolhead1.getText();
				Toolhead1.setText(Toolhead0.getText());
				Toolhead0.setText(temp);
				
			}
		});
		cont.add(switchItem, "wrap");
		cont.add(new JLabel("Toolhead 2 (Secondary)"), "split");
		
		cont.add(Toolhead0,"split");
		cont.add(Toolhead0ChooserButton, "wrap");
	
		final JTextField DestinationTextField = new JTextField(60);
		DestinationTextField.setText("");
		
		JButton DestinationChooserButton = new JButton("Browse...");
		DestinationChooserButton.addActionListener(new ActionListener()
		{

			
			public void actionPerformed(ActionEvent arg0) {
				String s = null;
				if(!DestinationTextField.getText().equals(""))
				{
				s = GcodeSelectWindow.goString(new File(DestinationTextField.getText()));	
				}
				else
				{
				s = GcodeSelectWindow.goString();
				}
				if(s != null)
				{
				DestinationTextField.setText(s);
				}
				
			}
			
		});
		cont.add(new JLabel("Combined Gcode: "), "split");
		cont.add(DestinationTextField, "split");
		cont.add(DestinationChooserButton, "wrap");
		final JCheckBox replaceStart = new JCheckBox();
		replaceStart.setSelected(true);
		cont.add(new JLabel("Replace start.gcode: "), "split");
		cont.add(replaceStart,"wrap");
		final JCheckBox replaceEnd = new JCheckBox();
		replaceEnd.setSelected(true);
		cont.add(new JLabel("Replace end.gcode: "), "split");
		cont.add(replaceEnd,"wrap");
		JButton merge = new JButton("Merge");
		
		merge.addActionListener(new ActionListener()
		
		{

			public void actionPerformed(ActionEvent arg0) {
				
				File primary = new File(Toolhead1.getText());
				File secondary = new File(Toolhead0.getText());
				File dest = new File(DestinationTextField.getText());
				DualStrusionConstruction dcs = new DualStrusionConstruction(primary, secondary, dest, replaceStart.isSelected(), replaceEnd.isSelected());
				Thread th = new Thread(dcs);
				th.run();
				result = dcs.getCombinedFile();
				Base.getEditor().handleOpenFile(result);
				frame.removeAll();
				frame.dispose();
				
			}
			
		});
		cont.add(merge);
		frame.add(cont);

	}
	/**
	 * This method returns the result of the gcode combining operation.
	 * @return the combined gcode.
	 */
	public File getCombined()
	{
		return result;
	}
	/**
	 * This method is unused and a prime canidate for deletion.
	 * @param savethis
	 */
	private static void saveGCodeSource(GCodeSource savethis)
	{StringListSource slss = (StringListSource) savethis;
	Iterator<String> slit = slss.iterator();
	try {
		FileWriter fwr = new FileWriter(new File("/home/makerbot/Desktop/target.gcode"));
		while(slit.hasNext())
		{
			fwr.write(slit.next());
		}
		fwr.close();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}	
	
	}
	public void actionPerformed(ActionEvent arg0) 
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void itemStateChanged(ItemEvent arg0) 
	{
		Object source = arg0.getItemSelectable();

	}
}