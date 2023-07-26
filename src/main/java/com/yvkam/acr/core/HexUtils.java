
package com.yvkam.acr.core;

import java.util.regex.Pattern;


public final class HexUtils {

    /** Regex pattern for hexadecimal strings */
    private static final Pattern HEX_STRING_PATTERN = Pattern.compile("^([0-9A-Fa-f]{2})+$");
    
    /** Array of all hexadecimal chars */
    private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();
    
    private HexUtils() {
    }

    /**
     * @param s a string
     * @return true if the provided string is hexadecimal, false otherwise
     */
    public static boolean isHexString(String s) {
        return (s != null) && HEX_STRING_PATTERN.matcher(s).matches();
    }
    
    /**
     * @param s a hex string
     * @return a byte array
     */
    public static byte[] hexStringToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * @param bytes a byte array
     * @return a hex string
     */
    public static String bytesToHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = HEX_CHARS[v >>> 4];
            hexChars[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(hexChars);
    }
}