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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


/**
 * @author Carlos Heuberger
 *
 */
public class Test {
    
    private static final String SEP = ";";
    

    public static void main(String[] args) {
        String file = "words.csv";
        if (args.length > 0) {
            file = args[0];
        }
        try {
            new Test(file);
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, ex);
        }
    }
    
    
    private final Path file;
    private final Path bak;
    
    private final List<Word> dictionary = new ArrayList<>();
    private final LinkedList<Word> words = new LinkedList<>();

    private JFrame frame;
    private JButton seqButton;
    private JButton randomButton;
    private JTextField question;
    private JTextField answer;
    private JTextField correction;
    private JButton yesButton;
    private JButton noButton;
    private JLabel status;
    
    private enum Mode { SEQ, RAND, SEQ_C, RAND_C, SEQ_T, RAND_T }
    
    private Mode mode;
    
    private int countCorrect = 0;
    private int countWrong = 0;
    private int totalCorrect = 0;
    private int totalWrong = 0;
    
    
    private Test(String filename) throws IOException {
        file = Paths.get(filename);
        bak = Paths.get(filename + ".bak");
        readWords();
        initGUI();
    }

    private void readWords() throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(SEP);
                if (tokens.length < 3)
                    throw new IOException("wrong format: " + line);
                try {
                    int id = parseInt(tokens[0]);
                    String english = tokens[1].trim();
                    String german = tokens[2].trim();
                    int correct = (tokens.length > 3) ? parseInt(tokens[3]) : 0;
                    int wrong = (tokens.length > 4) ? parseInt(tokens[4]) : 0;
                    dictionary.add(new Word(id, english, german, correct, wrong));
                } catch (NumberFormatException ex) {
                    throw new IOException(ex);
                }
            }
        }
        System.out.printf("Dictionary: %d words%n", dictionary.size());
        if (dictionary.size() == 0)
            throw new IOException("no words read");
        
        totalCorrect = dictionary.stream().mapToInt(Word::getCorrect).sum();
        totalWrong = dictionary.stream().mapToInt(Word::getWrong).sum();
    }
    
    private void saveWords() throws IOException {
        Files.move(file, bak, StandardCopyOption.REPLACE_EXISTING);
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            for (Word w : dictionary) {
                writer.write(w.getId() + SEP + w.getEnglish() + SEP + w.getGerman() + SEP + w.getCorrect() + SEP + w.getWrong());
                writer.newLine();
            }
        }
    }
    
    private void initGUI() {
        seqButton = new JButton("Sequence");
        seqButton.addActionListener(this::doSequence);
        
        randomButton = new JButton("Random");
        randomButton.addActionListener(this::doRandom);
        
        JButton quit = new JButton("Quit");
        quit.addActionListener(this::doQuit);
        
        question = new JTextField(40);
        question.setEditable(false);
        
        answer = new JTextField(40);
        answer.setEnabled(false);
        answer.addActionListener(this::doAnswer);
        
        correction = new JTextField(40);
        correction.setEditable(false);
        
        yesButton = new JButton("Yes");
        yesButton.setEnabled(false);
        yesButton.setForeground(Color.GREEN.darker());
        yesButton.addActionListener(this::doYes);
        
        noButton = new JButton("NO");
        noButton.setEnabled(false);
        noButton.setForeground(Color.RED.darker());
        noButton.addActionListener(this::doNo);
        
        Box buttons = Box.createHorizontalBox();
        buttons.add(Box.createHorizontalGlue());
        buttons.add(yesButton);
        buttons.add(Box.createHorizontalGlue());
        buttons.add(noButton);
        buttons.add(Box.createHorizontalGlue());
        
        status = new JLabel("mode?");
        status.setHorizontalAlignment(JLabel.CENTER);
        
        frame = new JFrame("BZF");
        frame.setLayout(new GridBagLayout());
        frame.add(seqButton, new GridBagConstraints(0, 0, 1, 1, 0, 0, CENTER, NONE, new Insets(14, 4, 24, 4) , 0, 0));
        frame.add(randomButton, new GridBagConstraints(RELATIVE, 0, 1, 1, 0, 0, CENTER, NONE, new Insets(14, 4, 24, 4) , 0, 0));
        frame.add(quit, new GridBagConstraints(RELATIVE, 0, 0, 1, 1.0, 0, LINE_END, NONE, new Insets(14, 4, 24, 4) , 0, 0));
        frame.add(question, new GridBagConstraints(0, RELATIVE, 3, 1, 0, 0, LINE_START, HORIZONTAL, new Insets(4, 4, 4, 4) , 0, 0));
        frame.add(answer, new GridBagConstraints(0, RELATIVE, 3, 1, 0, 0, LINE_START, HORIZONTAL, new Insets(4, 4, 24, 4) , 0, 0));
        frame.add(correction, new GridBagConstraints(0, RELATIVE, 3, 1, 0, 0, LINE_START, HORIZONTAL, new Insets(4, 4, 24, 4) , 0, 0));
        frame.add(buttons, new GridBagConstraints(0, RELATIVE, 3, 1, 0, 0, LINE_START, HORIZONTAL, new Insets(4, 4, 24, 4) , 0, 0));
        frame.add(status, new GridBagConstraints(0, RELATIVE, 3, 1, 0, 0, LINE_END, HORIZONTAL, new Insets(4, 4, 4, 4) , 0, 0));
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    private void next() {
        if (!words.isEmpty()) {
            words.remove();
        }
        if (words.isEmpty()) {
            fillWords();
            if (words.isEmpty()) {
                return;
            }
        }

        question.setText(words.peek().getEnglish());
        answer.setText(null);
        answer.setEnabled(true);
        yesButton.setEnabled(false);
        noButton.setEnabled(false);
        answer.requestFocusInWindow();
    }

    private void fillWords() {
        switch (mode) {
            case SEQ: {
                int min = dictionary.stream().mapToInt(Word::getDiff).min().orElse(0);
                dictionary.stream().filter(w -> w.getDiff() == min).forEachOrdered(words::add);
                break;
            }
            case RAND: {
                int min = dictionary.stream().mapToInt(Word::getDiff).min().orElse(0);
                dictionary.stream().filter(w -> w.getDiff() == min).forEach(words::add);
                Collections.shuffle(words);
                break;
            }
            case SEQ_C: {
                int min = dictionary.stream().mapToInt(Word::getCorrect).min().orElse(0);
                dictionary.stream().filter(w -> w.getCorrect() == min).forEachOrdered(words::add);
                break;
            }
            case RAND_C: {
                int min = dictionary.stream().mapToInt(Word::getCorrect).min().orElse(0);
                dictionary.stream().filter(w -> w.getCorrect() == min).forEach(words::add);
                Collections.shuffle(words);
                break;
            }
            case SEQ_T: {
                int min = dictionary.stream().mapToInt(Word::getTotal).min().orElse(0);
                dictionary.stream().filter(w -> w.getTotal() == min).forEachOrdered(words::add);
                break;
            }
            case RAND_T: {
                int min = dictionary.stream().mapToInt(Word::getTotal).min().orElse(0);
                dictionary.stream().filter(w -> w.getTotal() == min).forEach(words::add);
                Collections.shuffle(words);
                break;
            }
            default: {
                JOptionPane.showMessageDialog(frame, "Mode not implemented: " + mode);
                return;
            }
        }
        updateStatus();
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
    
    private void setMode(Mode m) {
        mode = m;
        seqButton.setEnabled(false);
        randomButton.setEnabled(false);
        next();
        updateStatus();
    }
    
    private void doSequence(ActionEvent ev) {
        if (isShift(ev)) {
            setMode(Mode.SEQ_T);
        } else if (isCtrl(ev)) {
            setMode(Mode.SEQ_C);
        } else {
            setMode(Mode.SEQ);
        }
    }
    
    private void doRandom(ActionEvent ev) {
        if (isShift(ev)) {
            setMode(Mode.RAND_T);
        } else if (isCtrl(ev)) {
            setMode(Mode.RAND_C);
        } else {
            setMode(Mode.RAND);
        }
    }
    
    private void doAnswer(ActionEvent ev) {
        String text = answer.getText().toLowerCase().trim();
        if (text.isEmpty()) {
            yesButton.setEnabled(false);
            noButton.setEnabled(false);
            answer.requestFocusInWindow();
        } else {
            Word word = words.peek();
            correction.setText(word.getGerman());
            correction.setForeground(null);
            if (text.equalsIgnoreCase(word.getGerman())) {
                correction.setBackground(Color.GREEN);
                correct();
                SwingUtilities.invokeLater(this::jumpNext);
                return;
            }
            
            yesButton.setEnabled(true);
            noButton.setEnabled(true);
            String correct = word.getGerman().toLowerCase();
            if (correct.contains(text) || text.contains(correct)) {
                correction.setBackground(Color.YELLOW);
                yesButton.requestFocusInWindow();
            } else {
                correction.setBackground(null);
                noButton.requestFocusInWindow();
            }
        }
    }
    
    private void doYes(ActionEvent ev) {
        correct();
        correction.setText(null);
        correction.setForeground(null);
        correction.setBackground(null);
        next();
    }
    
    private void doNo(ActionEvent ev) {
        wrong();
        correction.setText(null);
        correction.setForeground(null);
        correction.setBackground(null);
        next();
    }
    
    private void correct() {
        words.peek().correct();
        countCorrect += 1;
        totalCorrect += 1;
        updateStatus();
    }
    
    private void wrong() {
        words.peek().wrong();
        countWrong += 1;
        totalWrong += 1;
        updateStatus();
    }
    
    private void updateStatus() {
        status.setText(String.format("Session: +%d -%d, Total: +%d -%d, Open: %s", countCorrect, countWrong, totalCorrect, totalWrong, words.size()));
    }

    private void doQuit(ActionEvent ev) {
        try {
            saveWords();
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame, ex);
        }
        if (mode != null) {
            seqButton.setEnabled(true);
            randomButton.setEnabled(true);
            question.setText(null);
            answer.setText(null);
            answer.setEnabled(false);
            correction.setText(null);
            correction.setBackground(null);
            yesButton.setEnabled(false);
            noButton.setEnabled(false);
            status.setText("mode?");
            mode = null;
        } else {
            frame.dispose();
        }
    }
    
    private boolean isCtrl(ActionEvent ev) {
        return (ev.getModifiers() & ActionEvent.CTRL_MASK) != 0;
    }
    
    private boolean isShift(ActionEvent ev) {
        return (ev.getModifiers() & ActionEvent.SHIFT_MASK) != 0;
    }
    
    private int parseInt(String text) throws IOException {
        if (text.trim().isEmpty())
            return 0;
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            throw new IOException(ex);
        }
    }
}
