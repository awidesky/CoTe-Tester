package io.github.awidesky.coTe;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import io.github.awidesky.coTe.exception.CompileErrorException;
import io.github.awidesky.guiUtil.SwingDialogs;

public class MainFrame extends JFrame {

	private static final long serialVersionUID = 252547593768742341L;
	public static final String version = "1.0";

	private static final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
	private static File root = new File("probs");

	private final JComboBox<String> cb_week = new JComboBox<>(new String[] {"Week"});
	private final JComboBox<String> cb_prob = new JComboBox<>(new String[] {"Prob"});
	
	private final JButton show = new JButton("Show problem");
	private final JButton submit = new JButton("Submit");
	private final JFileChooser jfc = new JFileChooser();
	
	private IntPair selected = null;
	
	public MainFrame() {
		setTitle("CoTe-Tester " + version);
		System.out.println("CoTe-Tester v" + version);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(500, 500);
		setLayout(new BorderLayout(5, 5));

		jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		jfc.setFileFilter(new FileNameExtensionFilter(".cpp file", "cpp"));
		
		JPanel problemSelection = new JPanel();
		cb_prob.setEnabled(false);
		setupCheckbox();
		problemSelection.add(cb_week);
		problemSelection.add(Box.createHorizontalStrut(10));
		problemSelection.add(new JLabel("-"));
		problemSelection.add(Box.createHorizontalStrut(10));
		problemSelection.add(cb_prob);
		problemSelection.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
		add(problemSelection, BorderLayout.CENTER);
		
		JPanel submitPanel = new JPanel();
		show.setEnabled(false);
		show.addActionListener(e -> {
			File f= new File(root, "pdfs" + File.separator
					+ selected.toString() + ".pdf");
			try {
				Desktop.getDesktop().open(f);
			} catch (IOException e1) {
				SwingDialogs.error("Cannot open " + f.getName(), "%e%", e1, false);
			}
		});
		submit.setEnabled(false);
		submit.addActionListener(this::submit);
		submitPanel.add(show);
		submitPanel.add(submit);
		add(submitPanel, BorderLayout.SOUTH);
		
		pack();
		setLocation((dim.width - getSize().width) / 2, (dim.height - getSize().height) / 2);
		setVisible(true);
	}

	private String getSelectedProb() {
		return cb_week.getSelectedItem() + "_" + cb_prob.getSelectedItem();
	}

	
	public static File getRoot() {
		return root;
	}

	private List<IntPair> cotes;
	private void setupCheckbox() {
		cotes = Arrays.stream(new File(root, "IO").listFiles())
				.filter(f -> f.getName().endsWith(".in"))
				.map(File::getName)
				.map(s -> s.replaceAll("\\.\\d\\.in", ""))
				.map(IntPair::new)
				.toList();
		cotes.stream().mapToInt(IntPair::getWeek).distinct().sorted().mapToObj(String::valueOf).forEach(cb_week::addItem);
		cb_week.addActionListener(e -> {
			String selected = (String)cb_week.getSelectedItem();
			int cnt = cb_prob.getItemCount();
			for (int j = 1; j < cnt; j++) cb_prob.removeItemAt(1);
			if(selected.equals("Week")) {
				cb_prob.setSelectedIndex(0);
				cb_prob.setEnabled(false);
				return;
			}
            int i = Integer.parseInt(selected);
            
            cotes.stream()
            	.filter(p -> p.week == i)
            	.mapToInt(IntPair::getProb).distinct().sorted()
            	.mapToObj(String::valueOf)
            	.forEach(cb_prob::addItem);
            cb_prob.setEnabled(true);
		});
		cb_prob.addActionListener(e -> {
			boolean selectSomething = !((String)cb_prob.getSelectedItem()).equals("Prob");
			show.setEnabled(selectSomething);
			submit.setEnabled(selectSomething);
			
			if(selectSomething) {
				selected = new IntPair(getSelectedProb());
			}
		});
	}
	
	public void submit(ActionEvent e) {
		cb_prob.setEnabled(false);
		cb_week.setEnabled(false);
		show.setEnabled(false);
		submit.setEnabled(false);
		
		jfc.setDialogTitle("Choose cpp file for : " + getSelectedProb());
		if(jfc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;
		CoTe c = new CoTe(selected);
		boolean res = false;
		try {
			//TODO : run on separated thread to avoid deadlock
			res = c.test(jfc.getSelectedFile());
		} catch (CompileErrorException e1) {
			SwingDialogs.error("Compile Error!", "%e%", e1, true);
		}
		SwingDialogs.information(selected.toString(), res ? "Correct!" : "Wrong Answer - check the log!", true);
		
		cb_prob.setEnabled(true);
		cb_week.setEnabled(true);
		show.setEnabled(true);
		submit.setEnabled(true);
	}
	
	
	public static void main(String[] args) {
		if(args.length != 0) root = new File(args[0]);
		SwingUtilities.invokeLater(MainFrame::new);
	}
}
