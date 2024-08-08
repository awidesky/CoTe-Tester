package io.github.awidesky.coTe;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

public class MainFrame extends JFrame {

	private static final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
	private static File root = new File("probs");

	private final JComboBox<String> cb_week = new JComboBox<>(new String[] {"Week"});
	private final JComboBox<String> cb_prob = new JComboBox<>(new String[] {"Prob"});
	
	private final JButton show = new JButton("Show problem");
	private final JButton submit = new JButton("Submit");
	private final JFileChooser jfc = new JFileChooser();
	
	public MainFrame() {
		setTitle("CoTe-Tester");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(500, 500);
		setLayout(new BorderLayout(5, 5));
		
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
		submit.setEnabled(false);
		submit.addActionListener(this::submit);
		submitPanel.add(show);
		//submitPanel.add(Box.createHorizontalStrut(10));
		submitPanel.add(submit);
		add(submitPanel, BorderLayout.SOUTH);
		
		pack();
		setLocation(dim.width / 2 - getSize().width, dim.height / 2 - getSize().height / 2);
		setVisible(true);
	}

	public static void main(String[] args) {
		if(args.length != 0) root = new File(args[0]);
		SwingUtilities.invokeLater(MainFrame::new);
	}
	
	
	public static File getRoot() {
		return root;
	}

	private List<IntPair> cotes;
	private void setupCheckbox() {
		cotes = Arrays.stream(new File(root, "test_codes").listFiles())
				.filter(f -> f.getName().endsWith(".cpp"))
				.map(File::getName)
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
			boolean b = !((String)cb_prob.getSelectedItem()).equals("Prob");
			show.setEnabled(b);
			submit.setEnabled(b);
		});
	}
	
	public void submit(ActionEvent e) {
		jfc.setMultiSelectionEnabled(true);
		jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		jfc.setDialogTitle("Choose cpp files");
		jfc.setFileFilter(new FileNameExtensionFilter(".cpp file", "cpp"));
	}
}


class IntPair {
	public int week;
	public int prob;
	
	public IntPair(String str) {
		Matcher m = Pattern.compile("\\D*(\\d+)\\D+(\\d+)\\D*").matcher(str);
		if(!m.find()) System.out.println(str);;
		week = Integer.parseInt(m.group(1));
		prob = Integer.parseInt(m.group(2));
	}

	public int getWeek() {
		return week;
	}
	public int getProb() {
		return prob;
	}
	
	@Override
	public int hashCode() {
		return week * 10 + prob;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof IntPair other)
			return prob == other.prob && week == other.week;
		if (obj instanceof String other)
			return other.equals(week + "\\D+" + prob);
		if (obj instanceof Integer other)
			return other == hashCode();
		
		return false;
	}
	
}