import org.bitcoinj.core.*;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.omg.CORBA.portable.UnknownException;

import java.io.File;
import java.io.IOException;

public class bitcoin {
    public static void main(String args[]) {
        BriefLogFormatter.init();
        Wallet wallet;
        final File walletFile = new File("lam.wallet");

        if (args[0].equals("create")) {
            NetworkParameters params;
            params = RegTestParams.get();
            ECKey key = new ECKey();

            try {
                wallet = new Wallet(params);
                wallet.addKey(key);
                wallet.saveToFile(walletFile);
            } catch (IOException e) {
                System.out.println("Unable to create wallet file.");
            }
        } else if (args[0].equals("send")) {
            try {
                wallet = Wallet.loadFromFile(walletFile);
                ECKey key = wallet.currentReceiveKey();
                System.out.println(key);
            } catch (UnreadableWalletException e) {
                e.printStackTrace();
            }
        }


//        System.out.println(key);
//        Address addressFromKey = key.toAddress(params);
//        System.out.println(addressFromKey);


    }
}