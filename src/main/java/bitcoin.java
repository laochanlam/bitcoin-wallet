import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.bitcoinj.core.*;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Future;

public class bitcoin {
    public static void main(String args[]) {
        BriefLogFormatter.init();
        Wallet wallet;
        NetworkParameters params = TestNet3Params.get();
        final File walletFile = new File("lam.wallet");

        if (args[0].equals("create")) {
            ECKey key = new ECKey();

            try {
                wallet = new Wallet(params);
                wallet.importKey(key);
                wallet.saveToFile(walletFile);
                System.out.println("New wallet created");
            } catch (IOException e) {
                System.out.println("Unable to create wallet file.");
            }
        } else if (args[0].equals("send")) {
            try {
                wallet = Wallet.loadFromFile(walletFile);
                ECKey key = wallet.currentReceiveKey();
                System.out.println(key.toAddress(params));

            } catch (UnreadableWalletException e) {
                e.printStackTrace();
            }
        } else if (args[0].equals("test")) {
            String filePrefix = "forwarding-service-testnet";
            // Start up a basic app using a class that automates some boilerplate. Ensure we always have at least one key.
            WalletAppKit kit = new WalletAppKit(params, new File("."), filePrefix) {
                @Override
                protected void onSetupCompleted() {
                    // This is called in a background thread after startAndWait is called, as setting up various objects
                    // can do disk and network IO that may cause UI jank/stuttering in wallet apps if it were to be done
                    // on the main thread.
                    wallet().addCoinsReceivedEventListener(new WalletCoinsReceivedEventListener() {
                        @Override
                        public void onCoinsReceived(Wallet w, Transaction tx, Coin prevBalance, Coin newBalance) {
                            // Runs in the dedicated "user thread".
                            Coin value = tx.getValueSentToMe(w);
                            System.out.println("Received tx for " + value.toFriendlyString() + ": " + tx);
                            System.out.println("Transaction will be forwarded after it confirms.");

                            Futures.addCallback(tx.getConfidence().getDepthFuture(1), new FutureCallback<TransactionConfidence>() {
                                @Override
                                public void onSuccess(TransactionConfidence result) {
                                    System.out.println("Success!");
                                }

                                @Override
                                public void onFailure(Throwable t) {}


                            });
                        }
                    });

                    if (wallet().getKeyChainGroupSize() < 1)
                        wallet().importKey(new ECKey());

                    ECKey key = wallet().currentReceiveKey();
                    System.out.println(key.toAddress(params));

                    Coin value = wallet().getBalance();
                    System.out.println(value);
                }
            };
            // Download the block chain and wait until it's done.
            kit.startAsync();
            kit.awaitRunning();
        }


//        System.out.println(key);
//        Address addressFromKey = key.toAddress(params);
//        System.out.println(addressFromKey);


    }
}