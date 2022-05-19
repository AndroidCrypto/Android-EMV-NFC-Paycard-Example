# Android EMV NFC Paycard Example

This sample program shows how to implement the library EMV-NFC-Paycard-Enrollment 
for reading the data on an EMC (CreditCard etc) with the NFC technology (contactless).

Source code: https://github.com/devnied/EMV-NFC-Paycard-Enrollment

The app uses the IsoDep class for communication with the NFC card and 
the enableReaderMode on the NfcAdapter for detecting a NFC tag; 
this mode is more reliable then the often used enableForegroundDispatch. 
see here for a more detailed explanation:

https://stackoverflow.com/questions/33633736/whats-the-difference-between-enablereadermode-and-enableforegrounddispatch

This app is getting just a small subset of all available data fields on an EMV card: 
typeName, aid(s), card number and expiration date of the card. 
The complete code is available here:

https://github.com/AndroidCrypto/Android-EMV-NFC-Paycard-Example

Don't forget to view the Logcat as the app gives a deep look to the commands and data 
that is exchanged between the Android device and the EMV card

The app was tested on a real device with Android 8 (SDK 26) and Android 12 (SDK 31).
