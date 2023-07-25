package com.yvkam.acr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.smartcardio.*;

import lombok.extern.slf4j.Slf4j;
import org.nfctools.mf.MfCardListener;
import org.nfctools.mf.card.MfCard;
import org.nfctools.utils.CardTerminalUtils;

@Slf4j
public class Acr122Manager {
    
    /**
     * Entry point.
     * @param args the command line arguments
     * @see Acr122Manager#printHelpAndExit() 
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

    public static void printUid() {
        log.info("Connecting to PC/SC interface...");
        try {
            Acr122Device acr122 = initAcr122Device();
            CardTerminal terminal = acr122.getCardTerminal();
            log.info("Reader found: " + terminal.getName());

            // Loop to wait for cards
            while (true) {
                terminal.waitForCardPresent(10);

                if (terminal.isCardPresent()) {
                    // Card found, get details
                    Card card = terminal.connect("*");
                    log.info("Card found, retrieving UID!");

                    // Send UID request
                    CardChannel channel = card.getBasicChannel();
                    ResponseAPDU response = channel.transmit(new CommandAPDU(new byte[] {
                            (byte) 0xFF, (byte) 0xCA, (byte) 0x00, (byte) 0x00, (byte) 0x00
                    }));

                    if (response.getSW1() == 0x63 && response.getSW2() == 0x00) {
                        log.error("Error reading card.");
                    } else {
                        // Print UID
                        log.info("UID: " + HexUtils.bytesToHexString(response.getData()));
                        card.disconnect(false);
                    }

                    // Wait until card is removed before reading again
                    while(terminal.isCardPresent()) {
                        Thread.sleep(100);  // Check every 100ms
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error: " + e.getMessage(), e);
        }
    }

    /**
     * Prints help and exits.
     */
    private static void printHelpAndExit() {
        String jarPath = Acr122Manager.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        String jarName = jarPath.substring(jarPath.lastIndexOf('/') + 1);

        StringBuilder sb = new StringBuilder("Usage: java -jar ");
        sb.append(jarName).append(" [option]\n");

        sb.append("Options:\n");
        sb.append("\t-h, --help\t\t\tshow this help message and exit\n");
        sb.append("\t-d, --dump [KEYS...]\t\tdump Mifare Classic 1K cards using KEYS\n");
        sb.append("\t-w, --write S B KEY DATA\twrite DATA to sector S, block B of Mifare Classic 1K cards using KEY\n");

        sb.append("Examples:\n");
        sb.append("\tjava -jar ").append(jarName).append(" --dump FF00A1A0B000 FF00A1A0B001 FF00A1A0B099\n");
        sb.append("\tjava -jar ").append(jarName).append(" --write 13 2 FF00A1A0B001 FFFFFFFFFFFF00000000060504030201");

        log.info(sb.toString());

        System.exit(0);
    }

    private static void printCardInfo(MfCard card) {
        log.info("Card detected: "
                + card.getTagType().toString() + " "
                + card.toString());
    }
    
}
