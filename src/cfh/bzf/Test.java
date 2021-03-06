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
import java.awt.Font;
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
import java.util.function.ToIntFunction;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
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
    private JRadioButton learnButton;
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
	private JButton quitButton;
    
    
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
        learnButton = new JRadioButton("Learn");
        learnButton.addActionListener(this::doLearn);
        
        seqButton = new JButton("Sequence");
        seqButton.setToolTipText("<html>original sequence <br>"
                + "grouped by difference (correct-wrong), <br>"
                + "SHIFT: by total (correct+wrong), <br>"
                + "CTRL: by correc answers only</html>");
        seqButton.addActionListener(this::doSequence);
        
        randomButton = new JButton("Random");
        randomButton.setToolTipText("<html>random sequence <br>"
                + "grouped by difference (correct-wrong), <br>"
                + "SHIFT: by total (correct+wrong), <br>"
                + "CTRL: by correct answers only</html>");
        randomButton.addActionListener(this::doRandom);
        
        quitButton = new JButton("Quit");
        quitButton.addActionListener(this::doQuit);
        
        Font font = new Font("Dialog", Font.BOLD, 14);
        question = new JTextField(40);
        question.setEditable(false);
        question.setFont(font);
        
        answer = new JTextField(40);
        answer.setEnabled(false);
        answer.setFont(font);
        answer.addActionListener(this::doAnswer);
        
        correction = new JTextField(40);
        correction.setEditable(false);
        correction.setFont(font);
        correction.setFont(font);
        
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
        frame.add(learnButton, new GridBagConstraints(0, 0, 1, 1, 0, 0, CENTER, NONE, new Insets(14, 4, 24, 4) , 0, 0));
        frame.add(seqButton, new GridBagConstraints(RELATIVE, 0, 1, 1, 0, 0, CENTER, NONE, new Insets(14, 4, 24, 4) , 0, 0));
        frame.add(randomButton, new GridBagConstraints(RELATIVE, 0, 1, 1, 0, 0, CENTER, NONE, new Insets(14, 4, 24, 4) , 0, 0));
        frame.add(quitButton, new GridBagConstraints(RELATIVE, 0, 0, 1, 1.0, 0, LINE_END, NONE, new Insets(14, 4, 24, 4) , 0, 0));
        frame.add(question, new GridBagConstraints(0, RELATIVE, 4, 1, 0, 0, LINE_START, HORIZONTAL, new Insets(4, 4, 4, 4) , 0, 0));
        frame.add(answer, new GridBagConstraints(0, RELATIVE, 4, 1, 0, 0, LINE_START, HORIZONTAL, new Insets(4, 4, 24, 4) , 0, 0));
        frame.add(correction, new GridBagConstraints(0, RELATIVE, 4, 1, 0, 0, LINE_START, HORIZONTAL, new Insets(4, 4, 24, 4) , 0, 0));
        frame.add(buttons, new GridBagConstraints(0, RELATIVE, 4, 1, 0, 0, LINE_START, HORIZONTAL, new Insets(4, 4, 24, 4) , 0, 0));
        frame.add(status, new GridBagConstraints(0, RELATIVE, 4, 1, 0, 0, LINE_END, HORIZONTAL, new Insets(4, 4, 4, 4) , 0, 0));
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

        Word word = words.peek();
        question.setText(word.getEnglish());
        question.setToolTipText(String.format("Correct: %d, Wrong: %d", word.getCorrect(), word.getWrong()));
        answer.setText(learnButton.isSelected() ? word.getGerman() : null);
        answer.setEnabled(true);
        answer.setEditable(!learnButton.isSelected());
        yesButton.setEnabled(false);
        noButton.setEnabled(false);
        updateStatus();
        answer.requestFocusInWindow();
    }

    private void fillWords() {
        switch (mode) {
            case SEQ: {
                fillWords(Word::getDiff);
                break;
            }
            case RAND: {
                fillWords(Word::getDiff);
                Collections.shuffle(words);
                break;
            }
            case SEQ_C: {
                fillWords(Word::getCorrect);
                break;
            }
            case RAND_C: {
                fillWords(Word::getCorrect);
                Collections.shuffle(words);
                break;
            }
            case SEQ_T: {
                fillWords(Word::getTotal);
                break;
            }
            case RAND_T: {
                fillWords(Word::getTotal);
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

    private void fillWords(ToIntFunction<? super Word> func) {
        int min = dictionary.stream().mapToInt(func).min().orElse(0);
        if (learnButton.isSelected()) {
            int max = dictionary.stream().mapToInt(func).max().orElse(0);
            for (int i = min; i <= max; i += 1) {
                final int value = i;
                dictionary.stream().filter(w -> func.applyAsInt(w) == value).forEachOrdered(words::add);
            }
        } else {
            dictionary.stream().filter(w -> func.applyAsInt(w) == min).forEachOrdered(words::add);
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
    
    private void setMode(Mode m) {
        mode = m;
        learnButton.setEnabled(false);
        seqButton.setEnabled(false);
        randomButton.setEnabled(false);
        quitButton.setText("Close");
        next();
        updateStatus();
    }
    
    private void doLearn(ActionEvent ev) {
        learnButton.setBackground(learnButton.isSelected() ? Color.GREEN : null);
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
        if (learnButton.isSelected()) {
            next();
            return;
        }
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
        status.setText(String.format("Session: +%d -%d, Total: +%d -%d, Open: %d from %d", 
        		countCorrect, countWrong, totalCorrect, totalWrong, words.size(), dictionary.size()));
    }

    private void doQuit(ActionEvent ev) {
        if (mode != null) {
            words.clear();
            learnButton.setEnabled(true);
            seqButton.setEnabled(true);
            randomButton.setEnabled(true);
            quitButton.setText("QUIT");
            question.setText(null);
            question.setToolTipText(null);
            answer.setText(null);
            answer.setEnabled(false);
            correction.setText(null);
            correction.setBackground(null);
            yesButton.setEnabled(false);
            noButton.setEnabled(false);
            status.setText("mode?");
            mode = null;
        } else {
            try {
                saveWords();
                frame.dispose();
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(frame, ex);
            }
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
