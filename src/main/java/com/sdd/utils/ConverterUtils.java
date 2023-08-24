package com.sdd.utils;


import com.sdd.exception.SDDException;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletContext;
import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AlgorithmParameters;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ConverterUtils {


    public static String encrypt(String str, String password) {
        try {
            String encryptedpassword = null;
            try {
                /* MessageDigest instance for MD5. */
                MessageDigest m = MessageDigest.getInstance("MD5");

                /* Add plain-text password bytes to digest using MD5 update() method. */
                m.update(password.getBytes());

                /* Convert the hash value into bytes */
                byte[] bytes = m.digest();

                /* The bytes array has bytes in decimal form. Converting it into hexadecimal format. */
                StringBuilder s = new StringBuilder();
                for (Integer i = 0; i < bytes.length; i++) {
                    s.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
                }

                /* Complete hashed password in hexadecimal format */
                return encryptedpassword = s.toString();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            /* Display the unencrypted and encrypted passwords. */
            System.out.println("Plain-text password: " + password);
            System.out.println("Encrypted password using MD5: " + encryptedpassword);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static String getRandomString(MultipartFile file) {
        try {
            Thread.sleep(10);
            return System.currentTimeMillis() + "";
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return null + "";
    }

    public static String getRandomString(String fileName) {
        try {
            Thread.sleep(10);
            return fileName + System.currentTimeMillis() + "";
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return fileName + "";
    }


    public static String getRandomTimeStamp() {
        try {
            Thread.sleep(10);
            return System.currentTimeMillis() + "";
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return null + "";
    }


    public static String convertDate(Timestamp dateStart) {

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String string = dateFormat.format(dateStart);
            return string;
        } catch (Exception e) {
            e.printStackTrace();
        }


        return "";

    }

    public static long timeDiffer(Timestamp dateStart, Timestamp enddate) {


        Date d1 = null;
        Date d2 = null;
        try {
            d1 = new Date(dateStart.getTime());
            d2 = new Date(enddate.getTime());
        } catch (Exception e) {
            e.printStackTrace();
        }

        long diff = d2.getTime() - d1.getTime();

        long diffDays = diff / (60 * 60 * 1000 * 24);
//        long diffSeconds = diff / 1000;
//        long diffMinutes = diff / (60 * 1000);
//        long diffHours = diff / (60 * 60 * 1000);
//        System.out.println("Time in seconds: " + diffSeconds + " seconds.");
//        System.out.println("Time in minutes: " + diffMinutes + " minutes.");
//        System.out.println("Time in hours: " + diffHours + " hours.");
//        System.out.println("Time in Day: " + diffDays + " hours.");
        return diffDays;

    }


    public static File getComplaintPathOnly(String fileExtn, String fileName, String fileDestPath) {
        Path path = Paths.get(fileDestPath);
        File targetFile = new File(path.toString() + "/" + fileName + fileExtn);
        return targetFile;
    }


    public static String getCurrentDate() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDateTime now = LocalDateTime.now();
        return (dtf.format(now));
    }


    public static String addDecimalPoint(String number) {
        try {
            if (number == null) {
                return "0.0000";
            }

            String amoumt = String.format("%.4f", Double.parseDouble(number));
            DecimalFormat df = new DecimalFormat("#.####");
            String dat23 = df.format(Double.parseDouble(amoumt));

            if (!(dat23.contains("."))) {
                dat23 = dat23 + ".0000";
            }
            String amoumt11 = String.format("%.4f", Double.parseDouble(dat23));
            return amoumt11;
        } catch (Exception e) {
            return number;
        }
    }

    public static String addDecimal2Point(String number) {
        try {
            if (number == null) {
                return "0.00";
            }

            String amoumt = String.format("%.2f", Double.parseDouble(number));
            DecimalFormat df = new DecimalFormat("#.##");
            String dat23 = df.format(Double.parseDouble(amoumt));

            if (!(dat23.contains("."))) {
                dat23 = dat23 + ".00";
            }
            String amoumt11 = String.format("%.2f", Double.parseDouble(dat23));
            return amoumt11;
        } catch (Exception e) {
            return number;
        }
    }

    public static Boolean isNumber(String number) {
        try {
            Float.parseFloat(number);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    public static String addSpacaeInString(String number, int lenghtData) {

        String addSpace = "";
        for (Integer n = number.length(); n < lenghtData; n++) {
            addSpace = addSpace + " ";
        }
        if (number.length() == lenghtData) {
            return number + addSpace;
        } else {
            return number + addSpace + " ";
        }
    }

    public static String addSpacaeInStringInWord(String number, int lenghtData) {

        String addSpace = "";
        for (Integer n = number.length(); n < lenghtData; n++) {
            addSpace = addSpace + "   ";
        }
        if (number.length() == lenghtData) {
            return number + addSpace;
        } else {
            return number + addSpace + "  ";
        }
    }

    public static int getMaximumLength(int number1, int number2) {
        if (number1 > number2) {
            return number1;
        } else {
            return number2;
        }
    }


    public static String checkDateIsvalidOrNor(String dt) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        try {
            c.setTime(sdf.parse(dt));
        } catch (ParseException e) {
            e.printStackTrace();
            throw new SDDException(HttpStatus.BAD_REQUEST.value(), "INVALID DATE FORMAT");
        }
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
        return sdf1.format(c.getTime());
    }


    public static String conVertDateTimeFormat(String dt) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();

        if (dt == null) {
            return null;
        }
        try {
            c.setTime(sdf.parse(dt));
        } catch (ParseException e) {
            e.printStackTrace();
            return dt;
        }
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
        return sdf1.format(c.getTime());
    }

    public static Timestamp convertDateTotimeStamp(String inDate) {
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            return new Timestamp(((java.util.Date) df.parse(inDate)).getTime());
        } catch (Exception ie) {
            System.out.println("Problem creating directory " + ie);
            throw new SDDException(HttpStatus.UNAUTHORIZED.value(), "Date Format not correct. Use this yyyy-MM-dd.");
        }
    }


    public boolean isNumeric(String s) {
        return s != null && s.matches("[-+]?\\d*\\.?\\d+");
    }


    private static final String[] tensNames = {
            "",
            " Ten",
            " Twenty",
            " Thirty",
            " Forty",
            " Fifty",
            " Sixty",
            " Seventy",
            " Eighty",
            " Ninety"
    };

    private static final String[] numNames = {
            "",
            " One",
            " Two",
            " Three",
            " Four",
            " Five",
            " Six",
            " Seven",
            " Eight",
            " Nine",
            " Ten",
            " Eleven",
            " Twelve",
            " Thirteen",
            " Fourteen",
            " Fifteen",
            " Sixteen",
            " Seventeen",
            " Eighteen",
            " Nineteen"
    };


    private static String convertLessThanOneThousand(int number) {
        String soFar;

        if (number % 100 < 20) {
            soFar = numNames[number % 100];
            number /= 100;
        } else {
            soFar = numNames[number % 10];
            number /= 10;

            soFar = tensNames[number % 10] + soFar;
            number /= 10;
        }
        if (number == 0) return soFar;
        return numNames[number] + " Hundred" + soFar;
    }


    public static String convert(long number) {
        // 0 to 999 999 999 999
        if (number == 0) {
            return "zero";
        }

        String snumber = Long.toString(number);

        // pad with "0"
        String mask = "000000000000";
        DecimalFormat df = new DecimalFormat(mask);
        snumber = df.format(number);

        // XXXnnnnnnnnn
        int billions = Integer.parseInt(snumber.substring(0, 3));
        // nnnXXXnnnnnn
        int millions = Integer.parseInt(snumber.substring(3, 6));
        // nnnnnnXXXnnn
        int hundredThousands = Integer.parseInt(snumber.substring(6, 9));
        // nnnnnnnnnXXX
        int thousands = Integer.parseInt(snumber.substring(9, 12));

        String tradBillions;
        switch (billions) {
            case 0:
                tradBillions = "";
                break;
            case 1:
                tradBillions = convertLessThanOneThousand(billions)
                        + " Billion ";
                break;
            default:
                tradBillions = convertLessThanOneThousand(billions)
                        + " Billion ";
        }
        String result = tradBillions;

        String tradMillions;
        switch (millions) {
            case 0:
                tradMillions = "";
                break;
            case 1:
                tradMillions = convertLessThanOneThousand(millions)
                        + " Million ";
                break;
            default:
                tradMillions = convertLessThanOneThousand(millions)
                        + " Million ";
        }
        result = result + tradMillions;

        String tradHundredThousands;
        switch (hundredThousands) {
            case 0:
                tradHundredThousands = "";
                break;
            case 1:
                tradHundredThousands = "One Thousand ";
                break;
            default:
                tradHundredThousands = convertLessThanOneThousand(hundredThousands)
                        + " Thousand ";
        }
        result = result + tradHundredThousands;

        String tradThousand;
        tradThousand = convertLessThanOneThousand(thousands);
        result = result + tradThousand;

        // remove extra spaces!
        return result.replaceAll("^\\s+", "").replaceAll("\\b\\s{2,}\\b", " ");
    }


    public static void main(String[] args) {
        System.out.println("*** " + EnglishNumberToWords.convert(0));
        System.out.println("*** " + EnglishNumberToWords.convert(1));
        System.out.println("*** " + EnglishNumberToWords.convert(16));
        System.out.println("*** " + EnglishNumberToWords.convert(100));
        System.out.println("*** " + EnglishNumberToWords.convert(118));
        System.out.println("*** " + EnglishNumberToWords.convert(200));
        System.out.println("*** " + EnglishNumberToWords.convert(219));
        System.out.println("*** " + EnglishNumberToWords.convert(800));
        System.out.println("*** " + EnglishNumberToWords.convert(801));
        System.out.println("*** " + EnglishNumberToWords.convert(1316));
        System.out.println("*** " + EnglishNumberToWords.convert(1000000));
        System.out.println("*** " + EnglishNumberToWords.convert(2000000));
        System.out.println("*** " + EnglishNumberToWords.convert(3000200));
        System.out.println("*** " + EnglishNumberToWords.convert(700000));
        System.out.println("*** " + EnglishNumberToWords.convert(9000000));
        System.out.println("*** " + EnglishNumberToWords.convert(9001000));
        System.out.println("*** " + EnglishNumberToWords.convert(123456789));
        System.out.println("*** " + EnglishNumberToWords.convert(2147483647));
        System.out.println("*** " + EnglishNumberToWords.convert(3000000010L));

        /*
         *** zero
         *** one
         *** sixteen
         *** one hundred
         *** one hundred eighteen
         *** two hundred
         *** two hundred nineteen
         *** eight hundred
         *** eight hundred one
         *** one thousand three hundred sixteen
         *** one million
         *** two millions
         *** three millions two hundred
         *** seven hundred thousand
         *** nine millions
         *** nine millions one thousand
         *** one hundred twenty three millions four hundred
         **      fifty six thousand seven hundred eighty nine
         *** two billion one hundred forty seven millions
         **      four hundred eighty three thousand six hundred forty seven
         *** three billion ten
         **/
    }



    public static long timeDifferTimeStamp(Timestamp dateStart,Timestamp dateEnd) {


        Date d1 = null;
        Date d2 = null;
        try {
            d1 = new Date(dateStart.getTime());
            d2 = new Date(dateEnd.getTime());
        } catch (Exception e) {
            e.printStackTrace();
        }

        long diff = d1.getTime() - d2.getTime();
        long diffSeconds = diff / 1000;
        long diffMinutes = diff / (60 * 1000);
        long diffHours = diff / (60 * 60 * 1000);
        System.out.println("Time in seconds: " + diffSeconds + " seconds.");
        System.out.println("Time in minutes: " + diffMinutes + " minutes.");
        System.out.println("Time in hours: " + diffHours + " hours.");
        long diffday = diff / (60 * 60 * 1000 * 24);
        return diffday;

    }


}
