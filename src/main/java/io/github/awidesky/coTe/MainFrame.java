package io.github.awidesky.coTe;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class MainFrame extends JFrame {

	private static final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

	private final JComboBox<String> cb_week = new JComboBox<>(new String[] {"Week"});
	private final JComboBox<String> cb_prob = new JComboBox<>(new String[] {"Prob"});
	private final JFileChooser jfc = new JFileChooser();
	
	public MainFrame() {
		setTitle("CoTe-Tester");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(500, 500);
		setLayout(new BorderLayout(5, 5));
		
		JPanel problemSelection = new JPanel();
		cb_prob.setEnabled(false);
		problemSelection.add(cb_week);
		problemSelection.add(Box.createHorizontalStrut(10));
		problemSelection.add(new JLabel("-"));
		problemSelection.add(Box.createHorizontalStrut(10));
		problemSelection.add(cb_prob);
		problemSelection.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
		add(problemSelection, BorderLayout.CENTER);
		
		JPanel submitPanel = new JPanel();
		//choose and submitbutton
		add(submitPanel, BorderLayout.SOUTH);
		
		pack();
		setLocation(dim.width / 2 - getSize().width, dim.height / 2 - getSize().height / 2);
		setVisible(true);
		start();
		//dispose();
	}
	
	
	public void start() {
		jfc.setMultiSelectionEnabled(true);
		jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		jfc.setDialogTitle("Choose cpp files");
	}
}
