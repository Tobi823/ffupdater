package de.marmaro.krt.ffupdater.utils;

import android.content.Context;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TextViewAligner {

    private final Context context;
    private List<TextView> textViews = new ArrayList<>();
    private List<Integer> stringIds = new ArrayList<>();
    private List<Integer> spaceParamNumbers = new ArrayList<>();
    private List<Object[]> allParams = new ArrayList<>();

    public TextViewAligner(Context context) {
        this.context = context;
    }

    public void addTextView(TextView textView, int stringId, int spaceParamNumber, Object... params) {
        textViews.add(textView);
        stringIds.add(stringId);
        spaceParamNumbers.add(spaceParamNumber);
        allParams.add(params);
    }

    public List<String> align() {
        boolean space = isSpaceNecessary();
        List<Integer> tabs = new ArrayList<>(Collections.nCopies(0,textViews.size()));

        for (int tries = 0; tries < 10; tries++) {
            // measure sizes of all textView with the given number of tabs and with/without a space
            List<Integer> sizes = new ArrayList<>();
            for (int id = 0; id < textViews.size(); id++) {
                Object[] params = allParams.get(id);
                params[spaceParamNumbers.get(id)] = generateSpace(tabs.get(id), space);
                sizes.add(measureWidth(textViews.get(id), stringIds.get(id), params));
            }

            // return tabs and space if TextViews has the same size
            int minSize = Collections.min(sizes);
            if (minSize == Collections.max(sizes)) {
                // clear TextViews
                for (TextView textView : textViews) {
                    textView.setText("");
                }
                List<String> result = new ArrayList<>();
                for (Integer tab : tabs) {
                    result.add(generateSpace(tab, space));
                }
                return result;
            }

            // increase number of tab for the shortest TextViews
            for (int i = 0; i < sizes.size(); i++) {
                if (sizes.get(i) == minSize) {
                    tabs.set(i, tabs.get(i) + 1);
                }
            }
        }
        throw new RuntimeException("abort - align() does not work");
    }

    private boolean isSpaceNecessary() {
        for (int i = 0; i < textViews.size(); i++) {
            if (isSpaceNecessaryForTextView(textViews.get(i), stringIds.get(i), spaceParamNumbers.get(i), allParams.get(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean isSpaceNecessaryForTextView(TextView textView, int stringId, int spaceParamNumber, Object[] params) {
        params[spaceParamNumber] = generateSpace(0, false);
        int widthNoSpace = measureWidth(textView, stringId, params);
        params[spaceParamNumber] = generateSpace(1, false);
        int widthTab = measureWidth(textView, stringId, params);
        return Math.abs(widthNoSpace - widthTab) <= 2;
    }

    private int measureWidth(TextView textView, int stringId, Object[] params) {
        textView.setText(context.getString(stringId, params));
        textView.measure(0, 0);
        return textView.getMeasuredWidth();
    }

    private String generateSpace(int tabs, boolean spaceAtEnd) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < tabs; i++) {
            result.append("\t");
        }
        if (spaceAtEnd) {
            result.append(" ");
        }
        return result.toString();
    }
}
