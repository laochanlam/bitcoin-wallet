import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.wallet.Wallet;

import java.io.File;
import java.io.IOException;

public class bitcoin {
    public static void main(String args[]) {
        BriefLogFormatter.init();
//        if (args.length < 2) {
//            System.err.println("Usage: address-to-send-back-to [regtest|testnet]");
//            return;
//        }

        NetworkParameters params;
        params = RegTestParams.get();

        Wallet wallet = null;
        final File walletFile = new File("lam.wallet");
        ECKey key = new ECKey();
        try {
            wallet = new Wallet(params);
            wallet.addKey(key);
            wallet.saveToFile(walletFile);


            Address addressFromKey = key.toAddress(params);
            System.out.println(addressFromKey);
        } catch (IOException e) {
            System.out.println("Unable to create wallet file.");
        }
/*        String filePrefix;

        if (args[1].equals("testnet")) {
            params = TestNet3Params.get();
            filePrefix = "forwarding-service-testnet";
        } else if (args[1].equals("regtest")) {
            params = RegTestParams.get();
            filePrefix = "forwarding-service-regtest";
        } else {
            return;
        }*/

//        System.out.println(key);
        Address addressFromKey = key.toAddress(params);
        System.out.println(addressFromKey);
    }
}