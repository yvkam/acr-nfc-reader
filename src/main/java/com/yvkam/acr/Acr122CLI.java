package com.yvkam.acr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.smartcardio.*;

import com.yvkam.acr.core.Acr122Device;
import com.yvkam.acr.core.HexUtils;
import com.yvkam.acr.core.MifareUtils;
import lombok.extern.slf4j.Slf4j;
import org.nfctools.mf.MfCardListener;
import org.nfctools.mf.card.MfCard;

@Slf4j
public class Acr122CLI {
    private static final byte[] UID_COMMAND = {(byte) 0xFF, (byte) 0xCA, (byte) 0x00, (byte) 0x00, (byte) 0x00};
    private static final int CARD_PRESENT_CHECK_INTERVAL_MS = 100;
    private static final int ERROR_SW1 = 0x63;
    private static final int ERROR_SW2 = 0x00;
    private static final String CARD_CONNECT_PROTOCOL = "*";

    /**
     * Entry point.
     *
     * @param args the command line arguments
     * @see Acr122CLI#printHelpAndExit()
     */
    public static void main(String[] args) throws IOException {
        if (args == null || args.length == 0) {
            printHelpAndExit();
        }

        switch (Objects.requireNonNull(args)[0]) {
            case "-u", "--uid" -> printUid();
            case "-d", "--dump" -> dumpCards(args);
            case "-w", "--write" -> writeToCards(args);
            default -> printHelpAndExit();
        }
    }

    private static void dumpCards(String... args) throws IOException {
        // Building the list of keys
        final List<String> keys = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            String k = args[i].toUpperCase();
            if (MifareUtils.isValidMifareClassic1KKey(k)) {
                keys.add(k);
            }
        }
        // Adding the common keys
        keys.addAll(MifareUtils.COMMON_MIFARE_CLASSIC_1K_KEYS);

        // Card listener for dump
        MfCardListener listener = (mfCard, mfReaderWriter) -> {
            printCardInfo(mfCard);
            try {
                MifareUtils.dumpMifareClassic1KCard(mfReaderWriter, mfCard, keys);
            } catch (CardException ce) {
                log.error("Card removed or not present.", ce);
            }
        };

        // Start listening
        listen(listener);
    }

    private static void writeToCards(String... args) throws IOException {
        // Checking arguments
        if (args.length != 5) {
            printHelpAndExit();
        }

        final String sector = args[1];
        final String block = args[2];
        final String key = args[3].toUpperCase();
        final String data = args[4].toUpperCase();
        if (!MifareUtils.isValidMifareClassic1KSectorIndex(sector)
                || !MifareUtils.isValidMifareClassic1KBlockIndex(block)
                || !MifareUtils.isValidMifareClassic1KKey(key)
                || !HexUtils.isHexString(data)) {
            printHelpAndExit();
        }

        final int sectorId = Integer.parseInt(sector);
        final int blockId = Integer.parseInt(block);

        // Card listener for writing
        MfCardListener listener = (mfCard, mfReaderWriter) -> {
            printCardInfo(mfCard);
            try {
                MifareUtils.writeToMifareClassic1KCard(mfReaderWriter, mfCard, sectorId, blockId, key, data);
            } catch (CardException ce) {
                log.error("Card removed or not present.", ce);
            }
        };

        // Start listening
        listen(listener);
    }


    /**
     * Listens for cards using the provided listener.
     *
     * @param listener a listener
     */
    private static void listen(MfCardListener listener) throws IOException {
        Acr122Device acr122 = initAcr122Device();
        acr122.open();
        acr122.listen(listener);
        log.info("Press ENTER to exit");
        System.in.read();

        acr122.close();
    }

    private static Acr122Device initAcr122Device() {
        try {
            return new Acr122Device();
        } catch (RuntimeException re) {
            log.error("No ACR122 reader found.");
            throw re;
        }
    }
    private static CardTerminal initializeCardTerminal() {
        log.info("Connecting to PC/SC interface...");

        Acr122Device acr122 = initAcr122Device();
        CardTerminal terminal = acr122.getCardTerminal();
        log.info("Reader found: " + terminal.getName());

        return terminal;
    }
    public static void printUid() {
        try {
            CardTerminal terminal = initializeCardTerminal();

            while (!Thread.currentThread().isInterrupted()) {
                waitForCard(terminal);
                if (terminal.isCardPresent()) {
                    String uid = readCardUid(terminal);
                    log.info("UID: " + uid);
                    waitForCardRemoval(terminal);
                }
            }
        } catch (Exception e) {
            log.error("Error: " + e.getMessage(), e);
        }
    }

    public static String readCardUidOnce() {
        String cardUid = "";
        try {
            CardTerminal terminal = initializeCardTerminal();
            long start = System.currentTimeMillis();
            long end = start + 30*1000; // 30 seconds * 1000 ms/sec
            while (System.currentTimeMillis() < end) {
                waitForCard(terminal);
                if (terminal.isCardPresent()) {
                    cardUid = readCardUid(terminal);
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Error: " + e.getMessage(), e);
        }

        return cardUid;
    }


    private static void waitForCard(CardTerminal terminal) throws CardException {
        terminal.waitForCardPresent(10);
    }

    private static String readCardUid(CardTerminal terminal) throws CardException {
        log.info("Card found, retrieving UID!");

        Card card = terminal.connect(CARD_CONNECT_PROTOCOL);
        CardChannel channel = card.getBasicChannel();
        ResponseAPDU response = channel.transmit(new CommandAPDU(UID_COMMAND));

        if (response.getSW1() == ERROR_SW1 && response.getSW2() == ERROR_SW2) {
            throw new CardException("Error reading card.");
        }

        return HexUtils.bytesToHexString(response.getData());
    }

    private static void waitForCardRemoval(CardTerminal terminal) throws InterruptedException, CardException {
        while (terminal.isCardPresent()) {
            Thread.sleep(CARD_PRESENT_CHECK_INTERVAL_MS);  // Check every 100ms
        }
    }

    /**
     * Prints help and exits.
     */
    private static void printHelpAndExit() {
        String jarPath = Acr122CLI.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        String jarName = jarPath.substring(jarPath.lastIndexOf('/') + 1);

        String sb = "Usage: java -jar " + jarName + " [option]\n" +
                "Options:\n" +
                "\t-h, --help\t\t\tshow this help message and exit\n" +
                "\t-d, --dump [KEYS...]\t\tdump Mifare Classic 1K cards using KEYS\n" +
                "\t-w, --write S B KEY DATA\twrite DATA to sector S, block B of Mifare Classic 1K cards using KEY\n" +
                "Examples:\n" +
                "\tjava -jar " + jarName + " --dump FF00A1A0B000 FF00A1A0B001 FF00A1A0B099\n" +
                "\tjava -jar " + jarName + " --write 13 2 FF00A1A0B001 FFFFFFFFFFFF00000000060504030201";

        log.info(sb);

        System.exit(0);
    }

    private static void printCardInfo(MfCard card) {
        log.info("Card detected: "
                + card.getTagType().toString() + " "
                + card);
    }

}
