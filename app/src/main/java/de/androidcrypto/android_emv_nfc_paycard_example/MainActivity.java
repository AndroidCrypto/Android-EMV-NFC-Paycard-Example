package de.androidcrypto.android_emv_nfc_paycard_example;

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.devnied.emvnfccard.enums.EmvCardScheme;
import com.github.devnied.emvnfccard.model.Application;
import com.github.devnied.emvnfccard.model.EmvCard;
import com.github.devnied.emvnfccard.parser.EmvTemplate;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {
    /**
     * This sample program shows how to implement the library EMV-NFC-Paycard-Enrollment
     * for reading the data on an EMC (CreditCard etc) with the NFC technology (contactless).
     * Source code: https://github.com/devnied/EMV-NFC-Paycard-Enrollment
     *
     * The app uses the IsoDep class for communication with the NFC card and
     * the enableReaderMode on the NfcAdapter for detecting a NFC tag;
     * this mode is more reliable then the often used enableForegroundDispatch.
     * see here for a more detailed explanation:
     * https://stackoverflow.com/questions/33633736/whats-the-difference-between-enablereadermode-and-enableforegrounddispatch
     *
     * This app is getting just a small subset of all available data fields on an EMV card:
     * typeName, aid(s), card number and expiration date of the card.
     * The complete code is available here:
     * https://github.com/androidcrypto/Android-EMV-NFC-Paycard-Example
     *
     * Don't forget to view the Logcat as the app gives a deep look to the commands and data
     * that is exchanged between the Android device and the EMV card
     *
     * The app was tested on a real device with Android 8 (SDK 26) and Android 12 (SDK 31).
     */

    TextView nfcaContent;
    private NfcAdapter mNfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nfcaContent = findViewById(R.id.tvNfcaContent);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mNfcAdapter != null) {
            Bundle options = new Bundle();
            // Work around for some broken Nfc firmware implementations that poll the card too fast
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

            // Enable ReaderMode for all types of card and disable platform sounds
            // the option NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK is NOT set
            // to get the data of the tag after reading
            mNfcAdapter.enableReaderMode(this,
                    this,
                    NfcAdapter.FLAG_READER_NFC_A |
                            NfcAdapter.FLAG_READER_NFC_B |
                            NfcAdapter.FLAG_READER_NFC_F |
                            NfcAdapter.FLAG_READER_NFC_V |
                            NfcAdapter.FLAG_READER_NFC_BARCODE |
                            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                    options);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null)
            mNfcAdapter.disableReaderMode(this);
    }

    // This method is run in another thread when a card is discovered
    // !!!! This method cannot cannot direct interact with the UI Thread
    // Use `runOnUiThread` method to change the UI from this method
    @Override
    public void onTagDiscovered(Tag tag) {

        IsoDep isoDep = null;

        // Whole process is put into a big try-catch trying to catch the transceive's IOException
        try {
            isoDep = IsoDep.get(tag);
            if (isoDep != null) {
                ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(150, 10));
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //UI related things, not important for NFC
                    nfcaContent.setText("");
                }
            });
            isoDep.connect();
            byte[] response;
            String idContentString = "Content of ISO-DEP tag";

            PcscProvider provider = new PcscProvider();
            provider.setmTagCom(isoDep);

            EmvTemplate.Config config = EmvTemplate.Config()
                    .setContactLess(true)
                    .setReadAllAids(true)
                    .setReadTransactions(true)
                    .setRemoveDefaultParsers(false)
                    .setReadAt(true);

            EmvTemplate parser = EmvTemplate.Builder()
                    .setProvider(provider)
                    .setConfig(config)
                    .build();

            EmvCard card = parser.readEmvCard();
            String cardNumber = card.getCardNumber();
            Date expireDate = card.getExpireDate();
            LocalDate date = LocalDate.of(1999, 12, 31);
            if (expireDate != null) {
                date = expireDate.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
            }
            EmvCardScheme cardGetType = card.getType();
            if (cardGetType != null) {
                String typeName = card.getType().getName();
                String[] typeAids = card.getType().getAid();
                idContentString = idContentString + "\n" + "typeName: " + typeName;
                for (int i = 0; i < typeAids.length; i++) {
                    idContentString = idContentString + "\n" + "aid " + i + " : " + typeAids[i];
                }
            }

            List<Application> applications = card.getApplications();
            idContentString = idContentString + "\n" + "cardNumber: " + prettyPrintCardNumber(cardNumber);
            idContentString = idContentString + "\n" + "expireDate: " + date;

            String finalIdContentString = idContentString;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //UI related things, not important for NFC
                    nfcaContent.setText(finalIdContentString);
                }
            });
            try {
                isoDep.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            //Trying to catch any ioexception that may be thrown
            e.printStackTrace();
        } catch (Exception e) {
            //Trying to catch any exception that may be thrown
            e.printStackTrace();
        }

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    public static String prettyPrintCardNumber(String cardNumber) {
        if (cardNumber == null) return null;
        char delimiter = ' ';
        return cardNumber.replaceAll(".{4}(?!$)", "$0" + delimiter);
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuffer result = new StringBuffer();
        for (byte b : bytes) result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }
}
