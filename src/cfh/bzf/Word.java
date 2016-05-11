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


/**
 * @author Carlos Heuberger
 *
 */
public class Word {

    private final int id;
    private final String english;
    private final String german;
    
    private int correct;
    private int wrong;
    
    
    public Word(int id, String english, String german, int correct, int wrong) {
        this.id = requireNonNegative(id);
        this.english = requireNonEmpty(english);
        this.german = requireNonEmpty(german);
        this.correct = requireNonNegative(correct);
        this.wrong = requireNonNegative(wrong);
    }
    
    public void correct() {
        correct += 1;
    }
    
    public void wrong() {
        wrong += 1;
    }
    
    public int getId() {
        return id;
    }

    public String getEnglish() {
        return english;
    }

    public String getGerman() {
        return german;
    }

    public int getCorrect() {
        return correct;
    }

    public int getWrong() {
        return wrong;
    }

    private static String requireNonEmpty(String text) {
        if (text.trim().isEmpty())
            throw new IllegalArgumentException("empty text");
        return text;
    }
    
    private static int requireNonNegative(int value) {
        if (value < 0)
            throw new IllegalArgumentException("negative value: " + value);
        return value;
    }
}
