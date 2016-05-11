/*
 * Antennen DB
 *
 * Copyright (c) 2015 science+computing ag ALL RIGHTS RESERVED The content of
 * this work contains confidential and proprietary information of
 * science+computing ag. Any duplication, modification, distribution, or
 * disclosure in any form, in whole, or in part, is strictly prohibited without
 * the prior express written permission of science+computing ag
 * 
 * 11.05.2016
 */
package cfh.bzf;

import static java.awt.GridBagConstraints.*;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;


/**
 * @author Carlos Heuberger
 *
 */
public class Test {
    
    private static final Path FILE = Paths.get("words.csv");
    private static final Path BAK = Paths.get("words.bak");
    private static final String SEP = ";";
    

    public static void main(String[] args) {
        try {
            new Test();
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, ex);
        }
    }
    
    
    private final List<Word> words = new ArrayList<>();
    private int sequence = 0;
    private Word word = null;
    
    private JFrame frame;
    private JToggleButton random;
    private JTextField question;
    private JTextField answer;
    private JTextField correction;
    private JButton yesButton;
    private JButton noButton;
    
    
    private Test() throws IOException {
        readWords();
        initGUI();
        next();
    }

    private void readWords() throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(FILE)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(SEP, -1);
                if (tokens.length < 3)
                    throw new IOException("wrong format: " + line);
                try {
                    int id = Integer.parseInt(tokens[0]);
                    String english = tokens[1].trim();
                    String german = tokens[2].trim();
                    int correct = (tokens.length > 3) ? Integer.parseInt(tokens[3]) : 0;
                    int wrong = (tokens.length > 4) ? Integer.parseInt(tokens[4]) : 0;
                    words.add(new Word(id, english, german, correct, wrong));
                } catch (NumberFormatException ex) {
                    throw new IOException(ex);
                }
            }
        }
        if (words.size() == 0)
            throw new IOException("no words read");
    }
    
    private void saveWords() throws IOException {
        Files.move(FILE, BAK, StandardCopyOption.REPLACE_EXISTING);
        try (BufferedWriter writer = Files.newBufferedWriter(FILE)) {
            for (Word w : words) {
                writer.write(w.getId() + SEP + w.getEnglish() + SEP + w.getGerman() + SEP + w.getCorrect() + SEP + w.getWrong());
                writer.newLine();
            }
        }
    }
    
    private void initGUI() {
        random = new JToggleButton("Random");
        
        JButton quit = new JButton("Quit");
        quit.addActionListener(this::doQuit);
        
        question = new JTextField(30);
        question.setEditable(false);
        
        answer = new JTextField(30);
        answer.addActionListener(this::doAnswer);
        
        correction = new JTextField(30);
        correction.setEditable(false);
        
        yesButton = new JButton("Yes");
        yesButton.setForeground(Color.GREEN.darker());
        yesButton.addActionListener(this::doYes);
        
        noButton = new JButton("NO");
        noButton.setForeground(Color.RED.darker());
        noButton.addActionListener(this::doNo);
        
        Box buttons = Box.createHorizontalBox();
        buttons.add(Box.createHorizontalGlue());
        buttons.add(yesButton);
        buttons.add(Box.createHorizontalGlue());
        buttons.add(noButton);
        buttons.add(Box.createHorizontalGlue());
        
        frame = new JFrame("BZF");
        frame.setLayout(new GridBagLayout());
        frame.add(random, new GridBagConstraints(0, 0, 1, 1, 0, 0, LINE_START, NONE, new Insets(12, 2, 22, 2) , 0, 0));
        frame.add(quit, new GridBagConstraints(RELATIVE, 0, 1, 1, 0, 0, LINE_END, NONE, new Insets(12, 2, 22, 2) , 0, 0));
        frame.add(question, new GridBagConstraints(0, RELATIVE, 2, 1, 1.0, 0, LINE_START, HORIZONTAL, new Insets(2, 2, 2, 2) , 0, 0));
        frame.add(answer, new GridBagConstraints(0, RELATIVE, 2, 1, 1.0, 0, LINE_START, HORIZONTAL, new Insets(2, 2, 22, 2) , 0, 0));
        frame.add(correction, new GridBagConstraints(0, RELATIVE, 2, 1, 1.0, 0, LINE_START, HORIZONTAL, new Insets(2, 2, 22, 2) , 0, 0));
        frame.add(buttons, new GridBagConstraints(0, RELATIVE, 2, 1, 1.0, 0, LINE_START, HORIZONTAL, new Insets(2, 2, 12, 2) , 0, 0));
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    private void next() {
        if (random.isSelected()) {
            JOptionPane.showMessageDialog(frame, "random not implemented");
        } else {
            word = words.get(sequence);
            if (++sequence >= words.size()) {
                sequence = 0;
            }
            question.setText(word.getEnglish());
            answer.setText(null);
            yesButton.setEnabled(false);
            noButton.setEnabled(false);
        }
    }
    
    private void jumpNext() {
        try {
            Thread.sleep(1500);
            correction.setForeground(Color.GRAY);
            next();
        } catch (InterruptedException ex) {
            // ignored
        }
    }
    
    private void doAnswer(ActionEvent ev) {
        String text = answer.getText().toLowerCase().trim();
        if (text.isEmpty()) {
            yesButton.setEnabled(false);
            noButton.setEnabled(false);
        } else {
            correction.setText(word.getGerman());
            correction.setForeground(null);
            if (text.equalsIgnoreCase(word.getGerman())) {
                correction.setBackground(Color.GREEN);
                word.correct();
                SwingUtilities.invokeLater(this::jumpNext);
                return;
            }
            
            yesButton.setEnabled(true);
            noButton.setEnabled(true);
            String correct = word.getGerman().toLowerCase();
            if (correct.contains(text) || text.contains(correct)) {
                correction.setBackground(Color.YELLOW);
            } else {
                correction.setBackground(null);
            }
        }
    }
    
    private void doYes(ActionEvent ev) {
        word.correct();
        correction.setText(null);
        correction.setForeground(null);
        correction.setBackground(null);
        next();
    }
    
    private void doNo(ActionEvent ev) {
        word.wrong();
        correction.setText(null);
        correction.setForeground(null);
        correction.setBackground(null);
        next();
    }
    
    private void doQuit(ActionEvent ev) {
        try {
            saveWords();
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame, ex);
        }
        frame.dispose();
    }
}
