# ACR-NFC-Reader

Tool to read/write Mifare RFID tags with an ACR family device. Only ACR122U is supported.

### Features

  * Read/dump Mifare Classic tags
  * Write to Mifare Classic tags (block-wise)
  * ACR122U compliant (only)
  * Supported tags: Mifare Classic 1K (only)
  * JRE 7.0 or later

### Build

```bash
~$ mvn clean package
```

### Usage

```bash
~$ java -jar ./acr122urw.jar -h
Usage: java -jar acr122urw.jar [option]
Options:
    -h, --help                      show this help message and exit
    -u, --uid                       retrieve UID from Mifare Classic 1K cards
    -d, --dump [KEYS...]            dump Mifare Classic 1K cards using KEYS
    -w, --write S B KEY DATA        write DATA to sector S, block B of Mifare Classic 1K cards using KEY
Examples:
    java -jar acr122urw.jar --dump FF00A1A0B000 FF00A1A0B001 FF00A1A0B099
    java -jar acr122urw.jar --write 13 2 FF00A1A0B001 FFFFFFFFFFFF00000000060504030201
```

## About the ACR122U reader/writer

![ACR122U NFC reader/writer](res/acr122u_reader_writer.png?raw=true)

The [ACR122U NFC Reader](http://www.acs.com.hk/en/products/3/acr122u-usb-nfc-reader/) is made by [Advanced Card Systems Ltd](http://www.acs.com.hk/) (Hong Kong, China).

  
## Notes

### System requirements

```bash
~# # For Debian Testing
~# echo "install pn533 /bin/false" >> /etc/modprobe.d/blacklist-nfc.conf
~# echo "install nfc /bin/false" >> /etc/modprobe.d/blacklist-nfc.conf
~# modprobe -r pn533 nfc
~# apt-get install libpcsclite1 libccid pcscd libacsccid1 pcsc-tools
~# pcscd -f
```
