package com.yvkam.acr.core;

import java.io.IOException;
import javax.smartcardio.CardTerminal;

import lombok.extern.slf4j.Slf4j;
import org.nfctools.mf.MfCardListener;
import org.nfctools.spi.acs.Acr122ReaderWriter;
import org.nfctools.spi.acs.AcsTerminal;
import org.nfctools.utils.CardTerminalUtils;

@Slf4j
public class Acr122Device extends AcsTerminal {
    
    private final Acr122ReaderWriter readerWriter;
    
    public Acr122Device() {
        CardTerminal terminal = CardTerminalUtils.getTerminalByName("ACR122");
        setCardTerminal(terminal);
        readerWriter = new Acr122ReaderWriter(this);
    }

    @Override
    public void open() throws IOException {
        log.info("Opening device");
        super.open();
    }

    /**
     * Start listening for cards using the provided listener.
     * @param listener a listener
     */
    public void listen(MfCardListener listener) throws IOException {
        log.info("Listening for cards...");
        readerWriter.setCardListener(listener);
    }
    
    @Override
    public void close() throws IOException {
        log.info("Closing device");
        readerWriter.removeCardListener();
        super.close();
    }

}