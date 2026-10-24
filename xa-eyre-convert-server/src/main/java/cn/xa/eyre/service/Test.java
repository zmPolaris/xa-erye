package cn.xa.eyre.service;


import cn.xa.eyre.hub.staticvalue.DrugUnitEnum;

import java.util.*;
import java.util.regex.Pattern;

public class Test {
    // 预编译正则表达式以提高性能
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9.]+");

    /**
     * 去除数字部分，保留单位（包括特殊符号）
     */
    public static String removeNumbers(String str) {
        if (str == null || str.trim().isEmpty()) {
            return "";
        }

        // 使用正则表达式替换所有数字和小数点
        return NUMBER_PATTERN.matcher(str).replaceAll("");
    }

    /**
     * 去除数字部分，同时去除多余空格
     */
    public static String removeNumbersAndTrim(String str) {
        if (str == null || str.trim().isEmpty()) {
            return "";
        }

        // 替换数字和小数点，然后去除首尾空格
        String result = NUMBER_PATTERN.matcher(str).replaceAll("");
        return result.trim();
    }

    public static void main(String[] args) {
        String[] testCases = {
                "500mg", "30 L", "300iu:3ml", "1粒", "1片",
                "10ml:0.1g", "36.5 ℃", "25%", "100 mm/s²"
        };

        for (String test : testCases) {
            System.out.printf("%-20s -> %s%n", test, removeNumbersAndTrim(test));
        }

        DrugUnitEnum d1 = DrugUnitEnum.fromNameIgnoreCase(removeNumbersAndTrim("500mg"));
        System.out.println(d1);
        d1 = DrugUnitEnum.fromNameIgnoreCase(removeNumbersAndTrim("10ml:0.1g"));
        System.out.println(d1);
        d1 = DrugUnitEnum.fromNameIgnoreCase(removeNumbersAndTrim("400μg"));
        System.out.println(d1);
        d1 = DrugUnitEnum.fromNameIgnoreCase(removeNumbersAndTrim("1粒"));
        System.out.println(d1);

    }




}

