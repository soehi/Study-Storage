package com.example.taskhelper;

public enum OptionMap implements OptionInterface{
    LANGUAGE{
        @Override
        public void defaultMethod() {
            Language language = new Language();
            language.LanguageOption();
        }
    }
}
